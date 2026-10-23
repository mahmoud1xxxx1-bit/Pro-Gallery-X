package com.progalleryx.app

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException

/** Small, bounded frame strip to preview cuts without uploading a video. */
object VideoStoryboard {
    suspend fun generate(context: Context, uri: Uri, count: Int = 8): List<Bitmap> = withContext(Dispatchers.IO) {
        require(count in 2..12)
        val retriever = MediaMetadataRetriever()
        val result = ArrayList<Bitmap>(count)
        try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?: throw IOException("Unable to read video duration")
            require(duration >= 500) { "Video too short" }
            for (i in 0 until count) {
                currentCoroutineContext().ensureActive()
                val timestamp = (duration * i / (count - 1)).coerceAtMost(duration - 1)
                val frame = if (android.os.Build.VERSION.SDK_INT >= 27) {
                    retriever.getScaledFrameAtTime(timestamp * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 160, 90)
                } else {
                    retriever.getFrameAtTime(timestamp * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { full ->
                        val scaled = Bitmap.createScaledBitmap(full, 160, 90, true)
                        if (scaled !== full) full.recycle()
                        scaled
                    }
                }
                if (frame != null) result.add(frame)
            }
            result.toList()
        } catch (error: Throwable) {
            result.forEach { it.recycle() }
            throw error
        } finally {
            retriever.release()
        }
    }
}
