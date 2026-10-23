package com.progalleryx.app

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Add a separate soundtrack to one video, preserving original source files. */
object VideoSoundtrackEditor {
    suspend fun export(context: Context, video: Uri, soundtrack: Uri, output: Uri, startMs: Long, endMs: Long) {
        require(video != soundtrack && output != video && output != soundtrack)
        val duration = withContext(Dispatchers.IO) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, video)
                retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    ?: throw IOException("Video duration unavailable")
            } finally { retriever.release() }
        }
        require(VideoExportRules.validTrim(startMs, endMs, duration))
        val soundtrackDuration = withContext(Dispatchers.IO) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, soundtrack)
                retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    ?: throw IOException("Audio duration unavailable")
            } finally { retriever.release() }
        }
        require(soundtrackDuration >= endMs - startMs) { "Soundtrack must cover the selected clip" }
        val temp = File.createTempFile("pgx_soundtrack_", ".mp4", context.cacheDir)
        try {
            if (!temp.delete()) throw IOException("Cannot prepare file")
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<Unit> { cont ->
                    val videoItem = MediaItem.Builder().setUri(video)
                        .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(startMs).setEndPositionMs(endMs).build()).build()
                    val videoSequence = EditedMediaItemSequence.Builder(
                        listOf(EditedMediaItem.Builder(videoItem).setRemoveAudio(true).build())).build()
                    val audioItem = MediaItem.Builder().setUri(soundtrack)
                        .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
                            .setEndPositionMs(endMs - startMs).build()).build()
                    val audioSequence = EditedMediaItemSequence.Builder(
                        listOf(EditedMediaItem.Builder(audioItem).setRemoveVideo(true).build())).build()
                    val composition = Composition.Builder(listOf(videoSequence, audioSequence)).build()
                    val transformer = Transformer.Builder(context).addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (cont.isActive) cont.resume(Unit)
                        }
                        override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                            if (cont.isActive) cont.resumeWithException(exportException)
                        }
                    }).build()
                    cont.invokeOnCancellation { transformer.cancel() }
                    try { transformer.start(composition, temp.absolutePath) }
                    catch (error: Exception) { if (cont.isActive) cont.resumeWithException(error) }
                }
            }
            currentCoroutineContext().ensureActive()
            if (temp.length() <= 0) throw IOException("Empty output")
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(output, "w")?.use { destination ->
                    temp.inputStream().use { it.copyTo(destination, 256 * 1024) }
                } ?: throw IOException("Destination unavailable")
            }
        } finally { temp.delete() }
    }
}
