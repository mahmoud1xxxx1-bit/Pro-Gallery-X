package com.progalleryx.app

/** Shared color calculations for both editor preview and exported JPEG. */
object PhotoColorRules {
    fun contrastBrightness(contrast: Float, brightness: Float): FloatArray {
        require(contrast in 0.5f..1.5f && brightness in -100f..100f)
        val offset = 128f * (1f - contrast) + brightness * 2.55f
        return floatArrayOf(
            contrast, 0f, 0f, 0f, offset,
            0f, contrast, 0f, 0f, offset,
            0f, 0f, contrast, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
    }
    fun temperatureTint(temperature: Float, tint: Float): FloatArray {
        require(temperature in -100f..100f && tint in -100f..100f)
        return floatArrayOf(
            1f, 0f, 0f, 0f, temperature * 0.6f + tint * 0.22f,
            0f, 1f, 0f, 0f, -tint * 0.45f,
            0f, 0f, 1f, 0f, -temperature * 0.6f + tint * 0.22f,
            0f, 0f, 0f, 1f, 0f
        )
    }
}
