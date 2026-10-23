package com.progalleryx.app

/** Stable user-facing supported speed values; export duration is approximate due to encoder timing. */
object VideoSpeedRules {
    val allowed = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
    fun valid(speed: Float): Boolean = speed in allowed
    fun expectedDurationMs(inputMs: Long, speed: Float): Long {
        require(inputMs >= 500 && valid(speed))
        return (inputMs / speed).toLong()
    }
}
