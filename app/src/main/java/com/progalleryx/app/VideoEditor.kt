package com.progalleryx.app

import android.content.Context
import android.net.Uri
import android.media.MediaMetadataRetriever
import androidx.media3.common.MediaItem
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.ExportException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Single-clip on-device export; originals are always read-only. */
object VideoEditor {
    suspend fun trimToDocument(context: Context, input: Uri, output: Uri, startMs: Long, endMs: Long, muteAudio: Boolean = false) {
        require(startMs >= 0 && endMs > startMs && endMs - startMs >= 500) { "Invalid trim window" }
        require(input != output) { "Source and output must differ" }
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
                    val editor = EditedMediaItem.Builder(clip).setRemoveAudio(muteAudio).build()
                    val transformer = Transformer.Builder(context)
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
            if (!temp.exists() || temp.length() == 0L) throw IOException("Video export produced an empty file")
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(output, "w")?.use { destination ->
                    temp.inputStream().use { source -> source.copyTo(destination, 256 * 1024) }
                } ?: throw IOException("Output document is not writable")
            }
        } finally { temp.delete() }
    }
}
