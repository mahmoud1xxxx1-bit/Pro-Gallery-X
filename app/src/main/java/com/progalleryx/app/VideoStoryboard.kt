package com.progalleryx.app

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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


/**
 * Preview a single selected source clip with its configured trim boundaries.
 * Playback does not modify the source or the exported timeline.
 */
@androidx.compose.runtime.Composable
fun TrimmedClipPlayback(
    source: Uri,
    trimStartMs: Long,
    trimEndMs: Long,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    require(trimStartMs >= 0L && trimEndMs - trimStartMs >= 500L)
    val context = androidx.compose.ui.platform.LocalContext.current
    var view by androidx.compose.runtime.remember(source) {
        androidx.compose.runtime.mutableStateOf<android.widget.VideoView?>(null)
    }
    var playing by androidx.compose.runtime.remember(source, trimStartMs, trimEndMs) {
        androidx.compose.runtime.mutableStateOf(false)
    }
    var error by androidx.compose.runtime.remember(source) { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.DisposableEffect(source) {
        onDispose {
            view?.stopPlayback()
            view = null
        }
    }
    androidx.compose.runtime.LaunchedEffect(playing, source, trimStartMs, trimEndMs) {
        while (playing) {
            kotlinx.coroutines.delay(100L)
            val player = view ?: continue
            if (!player.isPlaying || player.currentPosition.toLong() >= trimEndMs) {
                player.pause()
                playing = false
                if (player.currentPosition.toLong() >= trimEndMs) {
                    player.seekTo(trimStartMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                }
            }
        }
    }
    androidx.compose.foundation.layout.Column(modifier) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { activityContext ->
                android.widget.VideoView(activityContext).also { video ->
                    video.setVideoURI(source)
                    video.setOnPreparedListener {
                        video.seekTo(trimStartMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                    }
                    video.setOnErrorListener { _, _, _ ->
                        error = true
                        playing = false
                        true
                    }
                    view = video
                }
            },
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .height(androidx.compose.ui.unit.Dp(210f))
        )
        androidx.compose.foundation.layout.Row {
            androidx.compose.material3.OutlinedButton(
                onClick = {
                    val player = view
                    if (player != null && !error) {
                        if (playing) {
                            player.pause()
                            playing = false
                        } else {
                            val current = player.currentPosition.toLong()
                            if (current < trimStartMs || current >= trimEndMs) {
                                player.seekTo(trimStartMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                            }
                            player.start()
                            playing = true
                        }
                    }
                },
                enabled = !error && view != null
            ) { androidx.compose.material3.Text(if (playing) "إيقاف مؤقت" else "تشغيل المقطع") }
            androidx.compose.material3.TextButton(onClick = {
                view?.pause()
                view?.seekTo(trimStartMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                playing = false
            }) { androidx.compose.material3.Text("إعادة البداية") }
        }
        if (error) androidx.compose.material3.Text("تعذر تشغيل هذا الملف على الجهاز")
    }
}

/** Sequential playback of trimmed clips. Uses original files and switches sources at boundaries. */
@androidx.compose.runtime.Composable
fun SequentialTimelinePlayback(
    segments: List<TimelineSegment<android.net.Uri>>,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    require(segments.size in 2..12)
    val context = androidx.compose.ui.platform.LocalContext.current
    var index by androidx.compose.runtime.remember(segments) { androidx.compose.runtime.mutableIntStateOf(0) }
    var playing by androidx.compose.runtime.remember(segments) { androidx.compose.runtime.mutableStateOf(false) }
    var ready by androidx.compose.runtime.remember(segments) { androidx.compose.runtime.mutableStateOf(false) }
    var error by androidx.compose.runtime.remember(segments) { androidx.compose.runtime.mutableStateOf(false) }
    val part = segments[index]
    val player = androidx.compose.runtime.remember(context) { android.widget.VideoView(context) }
    androidx.compose.runtime.DisposableEffect(player) {
        onDispose { player.stopPlayback() }
    }
    androidx.compose.runtime.LaunchedEffect(index, segments) {
        ready = false
        error = false
        player.stopPlayback()
        player.setOnErrorListener { _, _, _ ->
            error = true
            playing = false
            true
        }
        player.setOnPreparedListener {
            player.seekTo(part.sourceStartMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            ready = true
            if (playing) player.start()
        }
        player.setVideoURI(part.source)
    }
    androidx.compose.runtime.LaunchedEffect(playing, index, ready, segments) {
        while (playing && ready && !error) {
            kotlinx.coroutines.delay(100L)
            if (player.currentPosition.toLong() >= part.sourceEndMs || !player.isPlaying) {
                player.pause()
                val next = ClipSequenceTimelineRules.nextClipIndex(index, segments.size)
                if (next != null) {
                    index = next
                } else {
                    playing = false
                    index = 0
                }
                break
            }
        }
    }
    androidx.compose.foundation.layout.Column(modifier) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { player },
            modifier = androidx.compose.ui.Modifier.fillMaxWidth().height(androidx.compose.ui.unit.Dp(210f))
        )
        androidx.compose.material3.Text("المقطع ${index + 1} من ${segments.size} — تشغيل متتابع")
        androidx.compose.foundation.layout.Row {
            androidx.compose.material3.OutlinedButton(
                enabled = !error && ready,
                onClick = {
                    if (playing) {
                        player.pause()
                        playing = false
                    } else {
                        val position = player.currentPosition.toLong()
                        if (position < part.sourceStartMs || position >= part.sourceEndMs) {
                            player.seekTo(part.sourceStartMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                        }
                        player.start()
                        playing = true
                    }
                }
            ) { androidx.compose.material3.Text(if (playing) "إيقاف مؤقت" else "تشغيل الكل") }
            androidx.compose.material3.TextButton(onClick = {
                playing = false
                player.pause()
                index = 0
                player.seekTo(segments.first().sourceStartMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            }) { androidx.compose.material3.Text("من البداية") }
            androidx.compose.material3.TextButton(onClick = {
                val next = ClipSequenceTimelineRules.nextClipIndex(index, segments.size)
                if (next != null) index = next else playing = false
            }) { androidx.compose.material3.Text("التالي") }
        }
        if (error) androidx.compose.material3.Text("تعذر تشغيل أحد المقاطع على الجهاز")
    }
}

