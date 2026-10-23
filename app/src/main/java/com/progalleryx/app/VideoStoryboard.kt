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
    /** Decode one bounded preview frame at the selected original-source timestamp. */
    suspend fun frameAt(context: Context, uri: Uri, positionMs: Long): Bitmap = withContext(Dispatchers.IO) {
        require(positionMs >= 0L) { "Negative preview timestamp" }
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: throw IOException("Unknown clip duration")
            require(duration >= 500L) { "Video is too short" }
            val requested = positionMs.coerceAtMost(duration - 1L) * 1000L
            (if (android.os.Build.VERSION.SDK_INT >= 27) {
                retriever.getScaledFrameAtTime(requested, MediaMetadataRetriever.OPTION_CLOSEST, 480, 270)
            } else {
                retriever.getFrameAtTime(requested, MediaMetadataRetriever.OPTION_CLOSEST)?.let { original ->
                    val result = Bitmap.createScaledBitmap(original, 480, 270, true)
                    if (original !== result) original.recycle()
                    result
                }
            }) ?: throw IOException("Could not decode preview frame")
        } finally {
            retriever.release()
        }
    }

    suspend fun generate(context: Context, uri: Uri, count: Int = 8): List<Bitmap> = withContext(Dispatchers.IO) {
        require(count in 2..12)
        val retriever = MediaMetadataRetriever()
        val result = ArrayList<Bitmap>(count)
        try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?: throw IOException("Unable to read video duration")
            require(duration >= 500) { "Video too short" }
            for (timestamp in VideoTimelineRules.sampleTimes(duration, count)) {
                currentCoroutineContext().ensureActive()
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
