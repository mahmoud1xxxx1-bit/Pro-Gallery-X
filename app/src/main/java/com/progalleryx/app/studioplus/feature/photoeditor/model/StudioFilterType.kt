package com.progalleryx.app.studioplus.feature.photoeditor.model

import android.graphics.ColorMatrix

/**
 * Filter presets matching Samsung One UI & Professional Studio looks.
 * Generates an Android ColorMatrix that can be applied to GPU Canvas and Bitmaps.
 */
enum class StudioFilterType(val labelEn: String, val labelAr: String) {
    ORIGINAL("Original", "الأصلي"),
    VIVID("Vivid", "حيوي"),
    WARM("Warm", "دافئ"),
    COOL("Cool", "بارد"),
    NOIR("Noir", "أبيض وأسود"),
    SEPIA("Sepia", "سيبيا"),
    VINTAGE("Vintage", "عتيق"),
    CINEMATIC("Cinematic", "سينمائي"),
    CRISP("Crisp", "نقي"),
    SUNSET("Sunset", "غروب");

    /**
     * Builds the ColorMatrix corresponding to this filter preset with the given intensity (0f..1f).
     */
    fun getColorMatrix(intensity: Float = 1.0f): ColorMatrix {
        val clampedIntensity = intensity.coerceIn(0f, 1f)
        if (this == ORIGINAL || clampedIntensity == 0f) {
            return ColorMatrix()
        }

        val target = ColorMatrix()
        when (this) {
            ORIGINAL -> {}
            VIVID -> {
                // Boost saturation and mild contrast
                target.setSaturation(1.35f)
                val contrast = ColorMatrix(
                    floatArrayOf(
                        1.12f, 0f, 0f, 0f, -10f,
                        0f, 1.12f, 0f, 0f, -10f,
                        0f, 0f, 1.12f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                target.postConcat(contrast)
            }
            WARM -> {
                // Boost red and green, slightly cut blue
                target.set(
                    floatArrayOf(
                        1.15f, 0f, 0f, 0f, 12f,
                        0f, 1.05f, 0f, 0f, 5f,
                        0f, 0f, 0.88f, 0f, -15f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            COOL -> {
                // Boost blue, slightly reduce red
                target.set(
                    floatArrayOf(
                        0.90f, 0f, 0f, 0f, -8f,
                        0f, 0.98f, 0f, 0f, 0f,
                        0f, 0f, 1.20f, 0f, 15f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            NOIR -> {
                // High-contrast Black & White
                target.setSaturation(0f)
                val contrast = ColorMatrix(
                    floatArrayOf(
                        1.25f, 0f, 0f, 0f, -22f,
                        0f, 1.25f, 0f, 0f, -22f,
                        0f, 0f, 1.25f, 0f, -22f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                target.postConcat(contrast)
            }
            SEPIA -> {
                // Warm brownish antique tone
                target.set(
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            VINTAGE -> {
                // Desaturated with warm yellow highlight and faded shadows
                target.setSaturation(0.75f)
                val tint = ColorMatrix(
                    floatArrayOf(
                        1.08f, 0f, 0f, 0f, 20f,
                        0f, 1.02f, 0f, 0f, 15f,
                        0f, 0f, 0.85f, 0f, 25f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                target.postConcat(tint)
            }
            CINEMATIC -> {
                // Teal shadows, Orange highlights
                target.set(
                    floatArrayOf(
                        1.18f, 0f, 0f, 0f, 5f,
                        0f, 1.05f, 0f, 0f, 0f,
                        0f, 0f, 1.15f, 0f, 15f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                target.setSaturation(1.15f)
            }
            CRISP -> {
                // Clear, slightly cool, punchy contrast
                val contrast = ColorMatrix(
                    floatArrayOf(
                        1.20f, 0f, 0f, 0f, -12f,
                        0f, 1.20f, 0f, 0f, -12f,
                        0f, 0f, 1.22f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                target.postConcat(contrast)
            }
            SUNSET -> {
                // Rich golden hour orange and magenta tone
                target.set(
                    floatArrayOf(
                        1.25f, 0f, 0f, 0f, 18f,
                        0f, 0.95f, 0f, 0f, -5f,
                        0f, 0f, 0.90f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                target.setSaturation(1.25f)
            }
        }

        // If intensity is < 1.0, blend between identity and target
        if (clampedIntensity < 1.0f) {
            val targetArray = target.array ?: return target
            val identity = ColorMatrix()
            val identArray = identity.array ?: return target
            val blended = FloatArray(20)
            for (i in 0 until 20) {
                blended[i] = identArray[i] + (targetArray[i] - identArray[i]) * clampedIntensity
            }
            return ColorMatrix(blended)
        }

        return target
    }
}
