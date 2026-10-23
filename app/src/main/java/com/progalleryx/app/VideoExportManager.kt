package com.progalleryx.app

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.effect.Brightness
import androidx.media3.effect.CanvasOverlay
import androidx.media3.effect.Contrast
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

        val totalProjectDurationMs = VideoTimelineEngine.totalDurationMs(project.clips)
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

                        val audioProcessors = mutableListOf<AudioProcessor>()
                        if (clip.speed != 1.0f && VideoSpeedRules.valid(clip.speed)) {
                            clipBuilder.setSpeed(object : SpeedProvider {
                                override fun getSpeed(timeUs: Long): Float = clip.speed
                                override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
                            })
                            val sonic = SonicAudioProcessor().apply {
                                setSpeed(clip.speed)
                            }
                            audioProcessors.add(sonic)
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

                        // Visual Color Tuning & Presets
                        if (clip.brightness != 0f) {
                            effectsList.add(Brightness(clip.brightness))
                        }
                        if (clip.contrast != 1f) {
                            effectsList.add(Contrast(clip.contrast - 1f))
                        }
                        when (clip.filterId) {
                            PhotoFilterPresets.MONO.id -> {
                                effectsList.add(RgbFilter.createGrayscaleFilter())
                            }
                            PhotoFilterPresets.WARM.id -> {
                                effectsList.add(
                                    RgbAdjustment.Builder()
                                        .setRedScale(1.15f)
                                        .setGreenScale(1.05f)
                                        .setBlueScale(0.9f)
                                        .build()
                                )
                            }
                            PhotoFilterPresets.COOL.id -> {
                                effectsList.add(
                                    RgbAdjustment.Builder()
                                        .setRedScale(0.9f)
                                        .setGreenScale(1.02f)
                                        .setBlueScale(1.15f)
                                        .build()
                                )
                            }
                        }

                        if (audioProcessors.isNotEmpty() || effectsList.isNotEmpty()) {
                            clipBuilder.setEffects(Effects(audioProcessors, effectsList))
                        }

                        clipBuilder.build()
                    }

                    val videoSequence = EditedMediaItemSequence.Builder(editedClips).build()

                    // Composition-level effects (timed text and sticker overlays)
                    val compositionEffects = mutableListOf<androidx.media3.common.Effect>()
                    if (project.overlays.isNotEmpty()) {
                        val canvasOverlay = object : CanvasOverlay(true) {
                            override fun onDraw(canvas: Canvas, presentationTimeUs: Long) {
                                val timeMs = presentationTimeUs / 1000L
                                VideoOverlayRenderer.renderOverlaysToCanvas(
                                    canvas = canvas,
                                    overlays = project.overlays,
                                    timeMs = timeMs,
                                    width = canvas.width,
                                    height = canvas.height
                                )
                            }
                        }
                        compositionEffects.add(OverlayEffect(listOf(canvasOverlay)))
                    }

                    val compositionBuilder = if (project.soundtrack != null && !project.soundtrack.isMuted) {
                        val soundtrackStartMs = project.soundtrack.startOffsetMs
                        // Prevent soundtrack from exceeding total video project duration
                        val soundtrackEndMs = soundtrackStartMs + totalProjectDurationMs
                        val audioItem = MediaItem.Builder()
                            .setUri(project.soundtrack.uri)
                            .setClippingConfiguration(
                                MediaItem.ClippingConfiguration.Builder()
                                    .setStartPositionMs(soundtrackStartMs)
                                    .setEndPositionMs(soundtrackEndMs)
                                    .build()
                            )
                            .build()
                        val audioEdited = EditedMediaItem.Builder(audioItem).build()
                        val audioSequence = EditedMediaItemSequence.Builder(listOf(audioEdited)).build()
                        Composition.Builder(listOf(videoSequence, audioSequence))
                    } else {
                        Composition.Builder(videoSequence)
                    }

                    if (compositionEffects.isNotEmpty()) {
                        compositionBuilder.setEffects(Effects(emptyList(), compositionEffects))
                    }
                    val composition = compositionBuilder.build()

                    val transformerBuilder = Transformer.Builder(context)

                    if (project.bitrate != VideoQualityRules.AUTO) {
                        val encoderFactory = DefaultEncoderFactory.Builder(context)
                            .setRequestedVideoEncoderSettings(
                                VideoEncoderSettings.Builder().setBitrate(project.bitrate).build()
                            ).build()
                        transformerBuilder.setEncoderFactory(encoderFactory)
                    }

                    // Progress polling setup
                    val progressHolder = ProgressHolder()
                    val progressScope = CoroutineScope(Dispatchers.Main)
                    var progressJob: kotlinx.coroutines.Job? = null

                    val transformer = transformerBuilder
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(comp: Composition, exportResult: ExportResult) {
                                progressJob?.cancel()
                                onProgress?.invoke(1f)
                                if (cont.isActive) cont.resume(Unit)
                            }
                            override fun onError(comp: Composition, exportResult: ExportResult, exportException: ExportException) {
                                progressJob?.cancel()
                                if (cont.isActive) cont.resumeWithException(exportException)
                            }
                        })
                        .build()

                    cont.invokeOnCancellation {
                        progressJob?.cancel()
                        transformer.cancel()
                        if (temp.exists()) temp.delete()
                    }

                    try {
                        transformer.start(composition, temp.absolutePath)
                        progressJob = progressScope.launch {
                            while (isActive) {
                                val state = transformer.getProgress(progressHolder)
                                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                                    val ratio = (progressHolder.progress / 100f).coerceIn(0f, 1f)
                                    onProgress?.invoke(ratio)
                                }
                                delay(100)
                            }
                        }
                    } catch (e: Exception) {
                        progressJob?.cancel()
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
                context.contentResolver.openOutputStream(targetUri)?.use { outStream ->
                    temp.inputStream().use { inStream ->
                        inStream.copyTo(outStream)
                    }
                } ?: throw IOException("Failed to open destination Uri for writing")
            }
        } finally {
            if (temp.exists()) {
                temp.delete()
            }
        }
    }

    suspend fun exportProjectToGallery(
        context: Context,
        project: VideoStudioProject,
        displayName: String = "ProGalleryX_${System.currentTimeMillis()}.mp4",
        onProgress: ((Float) -> Unit)? = null
    ): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ProGalleryX")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val uri = withContext(Dispatchers.IO) {
            context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        } ?: throw IOException("Failed to create MediaStore entry for video export")

        try {
            exportProjectToUri(context, project, uri, onProgress)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val completeValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, completeValues, null, null)
            }
            return uri
        } catch (e: Exception) {
            context.contentResolver.delete(uri, null, null)
            throw e
        }
    }
}
