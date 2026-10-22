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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Concatenate 2–12 video clips on-device. Originals never overwritten. */
object MultiClipEditor {
    suspend fun export(context: Context, sources: List<Uri>, destination: Uri) {
        require(sources.size in 2..12) { "Choose 2 to 12 clips" }
        val temp = File.createTempFile("pgx_join_", ".mp4", context.cacheDir)
        try {
            // Transformer requires an output path that does not already exist.
            if (!temp.delete()) throw IOException("Cannot prepare export path")
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<Unit> { cont ->
                    val clips = sources.map { EditedMediaItem.Builder(MediaItem.fromUri(it)).build() }
                    val sequence = EditedMediaItemSequence.Builder(clips).build()
                    val composition = Composition.Builder(sequence).build()
                    val transformer = Transformer.Builder(context)
                        .addListener(object : Transformer.Listener {
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
            withContext(Dispatchers.IO) {
                if (!temp.exists() || temp.length() == 0L) throw IOException("Export produced an empty video")
                context.contentResolver.openOutputStream(destination, "w")?.use { out ->
                    temp.inputStream().use { it.copyTo(out, 256 * 1024) }
                } ?: throw IOException("Cannot write destination")
            }
        } finally { temp.delete() }
    }
}
