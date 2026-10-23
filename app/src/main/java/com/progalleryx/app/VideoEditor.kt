package com.progalleryx.app

import android.content.Context
import android.net.Uri
import android.media.MediaMetadataRetriever
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.ExportException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Single-clip on-device export; originals are always read-only. */
object VideoEditor {
    /** Export either side of a split using the same validated Media3 trim pipeline. */
    suspend fun splitPart(context: Context, input: Uri, output: Uri, splitMs: Long, secondHalf: Boolean, muteAudio: Boolean = false) {
        val duration = withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, input)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    ?: throw IOException("Unknown video duration")
            } finally { retriever.release() }
        }
        require(VideoExportRules.validSplit(splitMs, duration)) { "Both split segments must be at least 500 ms" }
        trimToDocument(context, input, output,
            if (secondHalf) splitMs else 0L,
            if (secondHalf) duration else splitMs, muteAudio)
    }

    suspend fun trimToDocument(context: Context, input: Uri, output: Uri, startMs: Long, endMs: Long, muteAudio: Boolean = false, speed: Float = 1f, bitrate: Int = VideoQualityRules.AUTO) {
        require(startMs >= 0 && endMs > startMs && endMs - startMs >= 500) { "Invalid trim window" }
        require(input != output) { "Source and output must differ" }
        require(VideoSpeedRules.valid(speed)) { "Unsupported video speed" }
        require(VideoQualityRules.valid(bitrate)) { "Unsupported output bitrate" }
        withContext(Dispatchers.IO) {
            val metadata = MediaMetadataRetriever()
            try {
                metadata.setDataSource(context, input)
                val duration = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    ?: throw IOException("Could not determine video duration")
                require(VideoExportRules.validTrim(startMs, endMs, duration)) { "Invalid trim range or video duration" }
            } finally { metadata.release() }
        }
        val temp = File.createTempFile("pgx_trim_", ".mp4", context.cacheDir)
        try {
            if (!temp.delete()) throw IOException("Cannot prepare export path")
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val clip = MediaItem.Builder()
                        .setUri(input)
                        .setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(startMs)
                                .setEndPositionMs(endMs)
                                .build()
                        ).build()
                    val builder = EditedMediaItem.Builder(clip).setRemoveAudio(muteAudio)
                    if (speed != 1f) builder.setSpeed(object : SpeedProvider {
                        override fun getSpeed(timeUs: Long): Float = speed
                        override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = C.TIME_UNSET
                    })
                    val editor = builder.build()
                    val transformerBuilder = Transformer.Builder(context)
                    if (bitrate != VideoQualityRules.AUTO) {
                        val encoderFactory = DefaultEncoderFactory.Builder(context)
                            .setRequestedVideoEncoderSettings(
                                androidx.media3.transformer.VideoEncoderSettings.Builder().setBitrate(bitrate).build()
                            ).build()
                        transformerBuilder.setEncoderFactory(encoderFactory)
                    }
                    val transformer = transformerBuilder
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: ExportResult) {
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                            override fun onError(composition: androidx.media3.transformer.Composition, exportResult: ExportResult, exportException: ExportException) {
                                if (continuation.isActive) continuation.resumeWithException(exportException)
                            }
                        }).build()
                    continuation.invokeOnCancellation { transformer.cancel() }
                    try { transformer.start(editor, temp.absolutePath) }
                    catch (error: Exception) { if (continuation.isActive) continuation.resumeWithException(error) }
                }
            }
            currentCoroutineContext().ensureActive()
            if (!temp.exists() || temp.length() == 0L) throw IOException("Video export produced an empty file")
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(output, "w")?.use { destination ->
                    temp.inputStream().use { source -> source.copyTo(destination, 256 * 1024) }
                } ?: throw IOException("Output document is not writable")
            }
        } finally { temp.delete() }
    }
}
