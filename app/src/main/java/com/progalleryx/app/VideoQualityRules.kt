package com.progalleryx.app

/** Safe output video encoding target bitrates and resolutions. */
object VideoQualityRules {
    const val AUTO = 0
    const val COMPACT = 2_000_000
    const val BALANCED = 5_000_000
    const val HIGH = 10_000_000
    val presets = listOf(AUTO, COMPACT, BALANCED, HIGH)

    const val HEIGHT_AUTO = 0
    const val HEIGHT_480P = 480
    const val HEIGHT_720P = 720
    const val HEIGHT_1080P = 1080
    val resolutionPresets = listOf(HEIGHT_AUTO, HEIGHT_480P, HEIGHT_720P, HEIGHT_1080P)

    fun valid(bitrate: Int): Boolean = bitrate in presets
    fun validHeight(height: Int): Boolean = height in resolutionPresets

    fun expectedVideoMegabytes(durationMs: Long, bitrate: Int): Double {
        require(durationMs >= 0 && valid(bitrate) && bitrate != AUTO)
        return durationMs.toDouble() / 1000.0 * bitrate / 8_000_000.0
    }
}
