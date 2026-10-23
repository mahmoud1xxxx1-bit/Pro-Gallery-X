package com.progalleryx.app

/** Safe output video encoding target bitrates, not guaranteed final file size or fidelity. */
object VideoQualityRules {
    const val AUTO = 0
    const val COMPACT = 2_000_000
    const val BALANCED = 5_000_000
    const val HIGH = 10_000_000
    val presets = listOf(AUTO, COMPACT, BALANCED, HIGH)
    fun valid(bitrate: Int): Boolean = bitrate in presets
    fun expectedVideoMegabytes(durationMs: Long, bitrate: Int): Double {
        require(durationMs >= 0 && valid(bitrate) && bitrate != AUTO)
        return durationMs.toDouble() / 1000.0 * bitrate / 8_000_000.0
    }
}
