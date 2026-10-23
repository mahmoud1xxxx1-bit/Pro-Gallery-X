package com.progalleryx.app

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Export a still JPEG from a locally selected video without modifying the source. */
object VideoFrameExporter {
    suspend fun export(context: Context, source: Uri, destination: Uri, timeMs: Long, quality: Int = 92) =
        withContext(Dispatchers.IO) {
            require(source != destination) { "Source and destination must differ" }
            require(timeMs >= 0 && quality in 70..100)
            val retriever = MediaMetadataRetriever()
            val temp = File.createTempFile("pgx_frame_", ".jpg", context.cacheDir)
            try {
                retriever.setDataSource(context, source)
                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    ?: throw IOException("Unknown video duration")
                require(timeMs <= duration) { "Frame timestamp exceeds duration" }
                currentCoroutineContext().ensureActive()
                val frame = retriever.getFrameAtTime(timeMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST)
                    ?: throw IOException("Unable to decode requested frame")
                try {
                    temp.outputStream().use { output ->
                        if (!frame.compress(Bitmap.CompressFormat.JPEG, quality, output)) throw IOException("JPEG encoding failed")
                    }
                } finally { frame.recycle() }
                if (temp.length() == 0L) throw IOException("Empty frame output")
                currentCoroutineContext().ensureActive()
                context.contentResolver.openOutputStream(destination, "w")?.use { out ->
                    temp.inputStream().use { it.copyTo(out, 256 * 1024) }
                } ?: throw IOException("Output not writable")
            } finally {
                retriever.release()
                temp.delete()
            }
        }
}
