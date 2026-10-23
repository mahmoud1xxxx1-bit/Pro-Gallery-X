package com.progalleryx.app

/** Shared, side-effect-free checks for video trimming and frame extraction. */
object VideoExportRules {
    fun validTrim(startMs: Long, endMs: Long, durationMs: Long): Boolean =
        durationMs >= 500L && startMs >= 0L && endMs > startMs &&
            endMs - startMs >= 500L && endMs <= durationMs

    fun validSplit(splitMs: Long, durationMs: Long): Boolean =
        durationMs >= 1000L && splitMs >= 500L && splitMs <= durationMs - 500L

    fun validFrame(timeMs: Long, durationMs: Long): Boolean =
        durationMs > 0L && timeMs >= 0L && timeMs <= durationMs
}
