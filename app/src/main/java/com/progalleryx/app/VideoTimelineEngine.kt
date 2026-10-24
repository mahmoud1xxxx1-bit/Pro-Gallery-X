package com.progalleryx.app

import java.util.Locale
import java.util.UUID

data class ClipTimeMapping(
    val clip: VideoClipItem,
    val clipIndex: Int,
    val clipStartInProjectMs: Long,
    val localOffsetInClipMs: Long,
    val sourceMediaTimeMs: Long
)

object VideoTimelineEngine {

    const val MIN_CLIP_DURATION_MS = 500L
    const val MIN_OVERLAY_DURATION_MS = 200L

    fun totalDurationMs(clips: List<VideoClipItem>): Long {
        return clips.sumOf { it.effectiveDurationMs }
    }

    fun clipAtProjectTime(clips: List<VideoClipItem>, projectTimeMs: Long): ClipTimeMapping? {
        if (clips.isEmpty()) return null
        val total = totalDurationMs(clips)
        val clampedTime = projectTimeMs.coerceIn(0L, total)

        var accumulatedMs = 0L
        for ((index, clip) in clips.withIndex()) {
            val clipDuration = clip.effectiveDurationMs
            val clipEndInProject = accumulatedMs + clipDuration

            val isLast = index == clips.lastIndex
            val matches = if (isLast) {
                clampedTime in accumulatedMs..clipEndInProject
            } else {
                clampedTime >= accumulatedMs && clampedTime < clipEndInProject
            }

            if (matches) {
                val localOffset = (clampedTime - accumulatedMs).coerceIn(0L, clipDuration)
                val sourceTime = (clip.startMs + (localOffset * clip.speed)).toLong().coerceIn(clip.startMs, clip.endMs)
                return ClipTimeMapping(
                    clip = clip,
                    clipIndex = index,
                    clipStartInProjectMs = accumulatedMs,
                    localOffsetInClipMs = localOffset,
                    sourceMediaTimeMs = sourceTime
                )
            }
            accumulatedMs = clipEndInProject
        }
        val lastClip = clips.last()
        return ClipTimeMapping(
            clip = lastClip,
            clipIndex = clips.lastIndex,
            clipStartInProjectMs = (total - lastClip.effectiveDurationMs).coerceAtLeast(0L),
            localOffsetInClipMs = lastClip.effectiveDurationMs,
            sourceMediaTimeMs = lastClip.endMs
        )
    }

    fun splitClip(
        clips: List<VideoClipItem>,
        clipId: String,
        projectSplitMs: Long
    ): Pair<List<VideoClipItem>, String?> {
        val clipIndex = clips.indexOfFirst { it.id == clipId }
        if (clipIndex == -1) return Pair(clips, null)

        val clip = clips[clipIndex]
        var startInProject = 0L
        for (i in 0 until clipIndex) {
            startInProject += clips[i].effectiveDurationMs
        }

        val localOffset = projectSplitMs - startInProject
        if (localOffset <= 0L || localOffset >= clip.effectiveDurationMs) {
            return Pair(clips, null)
        }

        val sourceSplitMs = (clip.startMs + (localOffset * clip.speed)).toLong()
        val part1Duration = sourceSplitMs - clip.startMs
        val part2Duration = clip.endMs - sourceSplitMs

        if (part1Duration < MIN_CLIP_DURATION_MS || part2Duration < MIN_CLIP_DURATION_MS) {
            return Pair(clips, null)
        }

        val part1 = clip.copy(endMs = sourceSplitMs)
        val newPartId = UUID.randomUUID().toString()
        val part2 = clip.copy(
            id = newPartId,
            startMs = sourceSplitMs
        )

        val updated = clips.toMutableList()
        updated[clipIndex] = part1
        updated.add(clipIndex + 1, part2)
        return Pair(updated, newPartId)
    }

    fun updateClipTrim(
        clips: List<VideoClipItem>,
        clipId: String,
        newStartMs: Long,
        newEndMs: Long,
        minTrimMs: Long = MIN_CLIP_DURATION_MS
    ): List<VideoClipItem> {
        return clips.map { clip ->
            if (clip.id == clipId) {
                val safeStart = newStartMs.coerceIn(0L, (clip.originalDurationMs - minTrimMs).coerceAtLeast(0L))
                val safeEnd = newEndMs.coerceIn((safeStart + minTrimMs).coerceAtMost(clip.originalDurationMs), clip.originalDurationMs)
                if (safeEnd - safeStart >= minTrimMs) {
                    clip.copy(startMs = safeStart, endMs = safeEnd)
                } else {
                    clip
                }
            } else {
                clip
            }
        }
    }

    fun reorderClips(clips: List<VideoClipItem>, fromIndex: Int, toIndex: Int): List<VideoClipItem> {
        if (fromIndex !in clips.indices || toIndex !in clips.indices || fromIndex == toIndex) {
            return clips
        }
        val mutable = clips.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return mutable
    }

    fun removeClip(clips: List<VideoClipItem>, clipId: String): List<VideoClipItem> {
        return clips.filterNot { it.id == clipId }
    }

    fun duplicateClip(clips: List<VideoClipItem>, clipId: String): Pair<List<VideoClipItem>, String?> {
        val index = clips.indexOfFirst { it.id == clipId }
        if (index == -1) return Pair(clips, null)
        val original = clips[index]
        val duplicateId = UUID.randomUUID().toString()
        val duplicate = original.copy(id = duplicateId)
        val mutable = clips.toMutableList()
        mutable.add(index + 1, duplicate)
        return Pair(mutable, duplicateId)
    }

    fun updateClipSpeed(clips: List<VideoClipItem>, clipId: String, speed: Float): List<VideoClipItem> {
        if (!VideoSpeedRules.valid(speed)) return clips
        return clips.map { clip ->
            if (clip.id == clipId) clip.copy(speed = speed) else clip
        }
    }

    fun updateClipVolume(clips: List<VideoClipItem>, clipId: String, volume: Float, isMuted: Boolean): List<VideoClipItem> {
        val safeVolume = volume.coerceIn(0f, 1f)
        return clips.map { clip ->
            if (clip.id == clipId) clip.copy(volume = safeVolume, isMuted = isMuted) else clip
        }
    }

    fun updateClipRotation(clips: List<VideoClipItem>, clipId: String, deltaDegrees: Int = 90): List<VideoClipItem> {
        return clips.map { clip ->
            if (clip.id == clipId) {
                val newRot = (clip.rotationDegrees + deltaDegrees).let { ((it % 360) + 360) % 360 }
                clip.copy(rotationDegrees = newRot)
            } else {
                clip
            }
        }
    }

    fun updateClipFilter(clips: List<VideoClipItem>, clipId: String, filterId: String): List<VideoClipItem> {
        return clips.map { clip ->
            if (clip.id == clipId) clip.copy(filterId = filterId) else clip
        }
    }

    fun updateClipTuning(
        clips: List<VideoClipItem>,
        clipId: String,
        brightness: Float,
        contrast: Float,
        saturation: Float
    ): List<VideoClipItem> {
        val safeBrightness = brightness.coerceIn(-0.5f, 0.5f)
        val safeContrast = contrast.coerceIn(0.5f, 2.0f)
        val safeSaturation = saturation.coerceIn(0f, 2.0f)
        return clips.map { clip ->
            if (clip.id == clipId) {
                clip.copy(
                    brightness = safeBrightness,
                    contrast = safeContrast,
                    saturation = safeSaturation
                )
            } else {
                clip
            }
        }
    }

    fun activeOverlaysAt(overlays: List<VideoTimedOverlay>, projectTimeMs: Long): List<VideoTimedOverlay> {
        return overlays.filter { it.isVisibleAt(projectTimeMs) }
    }

    fun clampOverlayTime(
        overlay: VideoTimedOverlay,
        newStartMs: Long,
        newEndMs: Long,
        projectDurationMs: Long,
        minDurationMs: Long = MIN_OVERLAY_DURATION_MS
    ): VideoTimedOverlay {
        val maxDuration = projectDurationMs.coerceAtLeast(minDurationMs)
        val safeStart = newStartMs.coerceIn(0L, (maxDuration - minDurationMs).coerceAtLeast(0L))
        val safeEnd = newEndMs.coerceIn((safeStart + minDurationMs).coerceAtMost(maxDuration), maxDuration)
        return overlay.copy(startMs = safeStart, endMs = safeEnd)
    }

    fun formatTimeMs(timeMs: Long): String {
        val safeMs = timeMs.coerceAtLeast(0L)
        val totalSeconds = safeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hundredths = (safeMs % 1000) / 10
        return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, hundredths)
    }

    fun timeToFraction(timeMs: Long, totalDurationMs: Long): Float {
        if (totalDurationMs <= 0L) return 0f
        return (timeMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    }

    fun fractionToTime(fraction: Float, totalDurationMs: Long): Long {
        if (totalDurationMs <= 0L) return 0L
        return (fraction.coerceIn(0f, 1f) * totalDurationMs).toLong()
    }
}
