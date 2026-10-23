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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class VideoJoinPart(val source: Uri, val startMs: Long, val endMs: Long, val muteAudio: Boolean = false)

/** Concatenate 2–12 video clips on-device. Originals never overwritten. */
object MultiClipEditor {
    /** Join individually trimmed segments; validate times against actual metadata. */
    suspend fun exportParts(context: Context, parts: List<VideoJoinPart>, destination: Uri) {
        require(parts.size in 2..12)
        require(parts.map { it.source }.distinct().size == parts.size)
        require(parts.none { it.source == destination })
        withContext(Dispatchers.IO) {
            for (part in parts) {
                val retriever = android.media.MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, part.source)
                    val duration = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                        ?: throw IOException("Unable to read duration")
                    require(VideoExportRules.validTrim(part.startMs, part.endMs, duration)) { "Invalid clip bounds" }
                } finally { retriever.release() }
            }
        }
        val temp = File.createTempFile("pgx_joinparts_", ".mp4", context.cacheDir)
        try {
            if (!temp.delete()) throw IOException("Cannot prepare export path")
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<Unit> { cont ->
                    val items = parts.map { part ->
                        val item = MediaItem.Builder().setUri(part.source)
                            .setClippingConfiguration(MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(part.startMs).setEndPositionMs(part.endMs).build())
                            .build()
                        EditedMediaItem.Builder(item).setRemoveAudio(part.muteAudio).build()
                    }
                    val composition = Composition.Builder(EditedMediaItemSequence.Builder(items).build()).build()
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
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            if (!temp.exists() || temp.length() <= 0L) throw IOException("Empty merge export")
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(destination, "w")?.use { out ->
                    temp.inputStream().use { it.copyTo(out, 256 * 1024) }
                } ?: throw IOException("Destination is not writable")
            }
        } finally { temp.delete() }
    }

    suspend fun export(context: Context, sources: List<Uri>, destination: Uri) {
        require(sources.size in 2..12) { "Choose 2 to 12 clips" }
        require(sources.distinct().size == sources.size) { "Duplicate clips selected" }
        require(destination !in sources) { "Output must not overwrite an original" }
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
            currentCoroutineContext().ensureActive()
            withContext(Dispatchers.IO) {
                if (!temp.exists() || temp.length() == 0L) throw IOException("Export produced an empty video")
                context.contentResolver.openOutputStream(destination, "w")?.use { out ->
                    temp.inputStream().use { it.copyTo(out, 256 * 1024) }
                } ?: throw IOException("Cannot write destination")
            }
        } finally { temp.delete() }
    }
}


/** Source-relative clip and cumulative playback position; no media is modified. */
data class TimelineSegment<T>(
    val source: T,
    val sourceStartMs: Long,
    val sourceEndMs: Long,
    val timelineStartMs: Long
) {
    val durationMs: Long get() = sourceEndMs - sourceStartMs
    val timelineEndMs: Long get() = timelineStartMs + durationMs
}

object ClipSequenceTimelineRules {
    const val MIN_CLIP_MS = 500L
    const val MAX_TOTAL_MS = 6L * 60L * 60L * 1000L

    fun <T> build(clips: List<Triple<T, Long, Long>>): List<TimelineSegment<T>> {
        require(clips.size in 2..12) { "Choose 2 to 12 clips" }
        require(clips.map { it.first }.distinct().size == clips.size) { "Duplicate clips" }
        var position = 0L
        return clips.map { (source, start, end) ->
            require(start >= 0 && end > start && end - start >= MIN_CLIP_MS) { "Invalid trim window" }
            val duration = end - start
            require(duration <= MAX_TOTAL_MS - position) { "Timeline too long" }
            TimelineSegment(source, start, end, position).also { position += duration }
        }
    }

    /** Pure batch trimming presets. Durations and windows use milliseconds. */
    enum class TrimPreset { FULL, FIRST_FIVE_SECONDS, LAST_FIVE_SECONDS }

    fun batchRanges(durationsMs: Map<String, Long>, preset: TrimPreset): Map<String, Pair<Long, Long>> {
        require(durationsMs.isNotEmpty() && durationsMs.size <= 12) { "Invalid clip count" }
        return durationsMs.mapValues { (_, duration) ->
            require(duration >= MIN_CLIP_MS && duration <= MAX_TOTAL_MS) { "Invalid clip duration" }
            val selection = minOf(duration, 5000L)
            when (preset) {
                TrimPreset.FULL -> 0L to duration
                TrimPreset.FIRST_FIVE_SECONDS -> 0L to selection
                TrimPreset.LAST_FIVE_SECONDS -> (duration - selection) to duration
            }
        }
    }

    fun totalSelectedDurationMs(ranges: Collection<Pair<Long, Long>>): Long {
        require(ranges.size in 2..12)
        var duration = 0L
        ranges.forEach { (start, end) ->
            require(start >= 0L && end >= start + MIN_CLIP_MS) { "Invalid clip range" }
            require(end - start <= MAX_TOTAL_MS - duration) { "Timeline duration exceeded" }
            duration += end - start
        }
        return duration
    }

    fun <T> segmentAt(timeline: List<TimelineSegment<T>>, positionMs: Long): TimelineSegment<T>? =
        timeline.firstOrNull { positionMs >= it.timelineStartMs && positionMs < it.timelineEndMs }

    /** Map a timeline cursor to its source-relative timestamp, clamping the end safely. */
    fun <T> sourcePositionAt(timeline: List<TimelineSegment<T>>, positionMs: Long): Pair<T, Long>? {
        if (timeline.isEmpty() || positionMs < 0L) return null
        val duration = totalDurationMs(timeline)
        if (duration == 0L) return null
        val clamped = positionMs.coerceAtMost(duration - 1L)
        val clip = segmentAt(timeline, clamped) ?: return null
        return clip.source to (clip.sourceStartMs + clamped - clip.timelineStartMs)
    }

    fun <T> clipIndexAt(timeline: List<TimelineSegment<T>>, positionMs: Long): Int {
        if (timeline.isEmpty() || positionMs < 0L) return -1
        val duration = totalDurationMs(timeline)
        if (duration == 0L) return -1
        val position = positionMs.coerceAtMost(duration - 1L)
        return timeline.indexOfFirst { position >= it.timelineStartMs && position < it.timelineEndMs }
    }

    fun <T> totalDurationMs(timeline: List<TimelineSegment<T>>): Long =
        timeline.lastOrNull()?.timelineEndMs ?: 0L

    fun formatTime(milliseconds: Long): String {
        val seconds = milliseconds.coerceAtLeast(0L) / 1000L
        return "%02d:%02d:%02d".format(java.util.Locale.ROOT, seconds / 3600L, seconds / 60L % 60L, seconds % 60L)
    }
}
