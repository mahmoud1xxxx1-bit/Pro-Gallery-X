package com.progalleryx.app

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object VideoExportManager {

    suspend fun exportProjectToUri(
        context: Context,
        project: VideoStudioProject,
        targetUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ) {
        require(project.clips.isNotEmpty()) { "Project must contain at least one clip" }
        require(VideoQualityRules.valid(project.bitrate)) { "Invalid export bitrate" }
        require(VideoQualityRules.validHeight(project.targetHeight)) { "Invalid target resolution" }

        // Validate clip metadata
        withContext(Dispatchers.IO) {
            for (clip in project.clips) {
                val uri = clip.sourceUri ?: throw IOException("Source Uri is missing for clip: ${clip.displayName}")
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                        ?: throw IOException("Unable to read duration for clip: ${clip.displayName}")
                    require(VideoExportRules.validTrim(clip.startMs, clip.endMs, duration)) {
                        "Invalid clip bounds for ${clip.displayName}: ${clip.startMs}..${clip.endMs} (duration: $duration)"
                    }
                } finally {
                    retriever.release()
                }
            }
        }

        val temp = File.createTempFile("pgx_studio_export_", ".mp4", context.cacheDir)
        try {
            if (!temp.delete()) throw IOException("Cannot prepare export path")

            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<Unit> { cont ->
                    val editedClips = project.clips.map { clip ->
                        val clipConfig = MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(clip.startMs)
                            .setEndPositionMs(clip.endMs)
                            .build()

                        val mediaItem = MediaItem.Builder()
                            .setUri(clip.sourceUri!!)
                            .setClippingConfiguration(clipConfig)
                            .build()

                        val clipBuilder = EditedMediaItem.Builder(mediaItem)
                            .setRemoveAudio(clip.isMuted || project.isMasterOriginalMuted)

                        if (clip.speed != 1.0f && VideoSpeedRules.valid(clip.speed)) {
                            clipBuilder.setSpeed(object : SpeedProvider {
                                override fun getSpeed(timeUs: Long): Float = clip.speed
                                override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
                            })
                        }

                        val effectsList = mutableListOf<androidx.media3.common.Effect>()

                        // Rotation Effect
                        if (clip.rotationDegrees != 0) {
                            effectsList.add(
                                ScaleAndRotateTransformation.Builder()
                                    .setRotationDegrees(clip.rotationDegrees.toFloat())
                                    .build()
                            )
                        }

                        // Aspect Ratio or Target Height Presentation Effect
                        if (project.aspectRatio != VideoAspectRatio.ORIGINAL) {
                            effectsList.add(
                                Presentation.createForAspectRatio(
                                    project.aspectRatio.ratio,
                                    Presentation.LAYOUT_SCALE_TO_FIT
                                )
                            )
                        } else if (project.targetHeight > 0) {
                            effectsList.add(Presentation.createForHeight(project.targetHeight))
                        }

                        // Presets like Monochrome
                        if (clip.filterId == PhotoFilterPresets.MONO.id) {
                            effectsList.add(RgbFilter.createGrayscaleFilter())
                        }

                        if (effectsList.isNotEmpty()) {
                            clipBuilder.setEffects(Effects(emptyList(), effectsList))
                        }

                        clipBuilder.build()
                    }

                    val videoSequence = EditedMediaItemSequence.Builder(editedClips).build()

                    val composition = if (project.soundtrack != null && !project.soundtrack.isMuted) {
                        val audioItem = MediaItem.Builder()
                            .setUri(project.soundtrack.uri)
                            .setClippingConfiguration(
                                MediaItem.ClippingConfiguration.Builder()
                                    .setStartPositionMs(project.soundtrack.startOffsetMs)
                                    .build()
                            )
                            .build()
                        val audioEdited = EditedMediaItem.Builder(audioItem).build()
                        val audioSequence = EditedMediaItemSequence.Builder(listOf(audioEdited)).build()
                        Composition.Builder(listOf(videoSequence, audioSequence)).build()
                    } else {
                        Composition.Builder(videoSequence).build()
                    }

                    val transformerBuilder = Transformer.Builder(context)

                    if (project.bitrate != VideoQualityRules.AUTO) {
                        val encoderFactory = DefaultEncoderFactory.Builder(context)
                            .setRequestedVideoEncoderSettings(
                                VideoEncoderSettings.Builder().setBitrate(project.bitrate).build()
                            ).build()
                        transformerBuilder.setEncoderFactory(encoderFactory)
                    }

                    val transformer = transformerBuilder
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(comp: Composition, exportResult: ExportResult) {
                                if (cont.isActive) cont.resume(Unit)
                            }
                            override fun onError(comp: Composition, exportResult: ExportResult, exportException: ExportException) {
                                if (cont.isActive) cont.resumeWithException(exportException)
                            }
                        })
                        .build()

                    cont.invokeOnCancellation { transformer.cancel() }

                    try {
                        transformer.start(composition, temp.absolutePath)
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resumeWithException(e)
                    }
                }
            }

            currentCoroutineContext().ensureActive()
            if (!temp.exists() || temp.length() <= 0L) {
                throw IOException("Exported video file is empty")
            }

            // Write to destination Uri
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(targetUri, "w")?.use { destStream ->
                    temp.inputStream().use { srcStream ->
                        srcStream.copyTo(destStream, 256 * 1024)
                    }
                } ?: throw IOException("Destination Uri is not writable")
            }
        } finally {
            temp.delete()
        }
    }

    suspend fun exportProjectToMediaStore(
        context: Context,
        project: VideoStudioProject,
        displayName: String = "ProGalleryX_${System.currentTimeMillis()}",
        onProgress: ((Float) -> Unit)? = null
    ): Uri {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "$displayName.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ProGalleryX")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val targetUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IOException("Failed to create MediaStore entry for video export")

        try {
            exportProjectToUri(context, project, targetUri, onProgress)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                resolver.update(targetUri, updateValues, null, null)
            }

            return targetUri
        } catch (e: Exception) {
            resolver.delete(targetUri, null, null)
            throw e
        }
    }
}
