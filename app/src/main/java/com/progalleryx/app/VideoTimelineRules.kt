package com.progalleryx.app

/** Stable, bounded sampling times for video thumbnail/storyboard navigation. */
object VideoTimelineRules {
    fun sampleTimes(durationMs: Long, count: Int): List<Long> {
        require(durationMs >= 500 && count in 2..12)
        return (0 until count).map { index ->
            ((durationMs - 1) * index / (count - 1)).coerceIn(0, durationMs - 1)
        }
    }
}
