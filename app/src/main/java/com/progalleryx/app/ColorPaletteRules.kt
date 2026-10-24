package com.progalleryx.app

object ColorPaletteRules {

    val STUDIO_COLORS: List<Long> = listOf(
        0xFFFFFFFF, // Pure White
        0xFF121212, // Deep Black
        0xFFFF3B30, // Vibrant Red
        0xFFFF9500, // Sunset Orange
        0xFFFFCC00, // Gold Yellow
        0xFF34C759, // Vibrant Green
        0xFF00C7BE, // Cyan / Teal
        0xFF007AFF, // Studio Blue
        0xFF5856D6, // Indigo
        0xFFAF52DE, // Violet Purple
        0xFFFF2D55, // Rose Pink
        0xFF8E8E93, // Neutral Gray
        0xFFFFD60A, // Bright Lemon
        0xFF30D158, // Neon Lime
        0xFF64D2FF  // Sky Blue
    )

    /** Parses a HEX string into a 64-bit unsigned/signed ARGB Long. */
    fun parseHexColor(hexString: String): Long? {
        val clean = hexString.trim().removePrefix("#")
        return when (clean.length) {
            6 -> {
                val rgb = clean.toLongOrNull(16) ?: return null
                0xFF000000L or rgb
            }
            8 -> clean.toLongOrNull(16)
            3 -> {
                val r = clean.substring(0, 1).repeat(2)
                val g = clean.substring(1, 2).repeat(2)
                val b = clean.substring(2, 3).repeat(2)
                val rgb = "$r$g$b".toLongOrNull(16) ?: return null
                0xFF000000L or rgb
            }
            else -> null
        }
    }

    /** Formats a Long ARGB color into a #RRGGBB or #AARRGGBB hex string. */
    fun formatHexColor(colorArgb: Long, includeAlpha: Boolean = false): String {
        val a = ((colorArgb shr 24) and 0xFF).toInt()
        val r = ((colorArgb shr 16) and 0xFF).toInt()
        val g = ((colorArgb shr 8) and 0xFF).toInt()
        val b = (colorArgb and 0xFF).toInt()
        return if (includeAlpha) {
            String.format("#%02X%02X%02X%02X", a, r, g, b)
        } else {
            String.format("#%02X%02X%02X", r, g, b)
        }
    }

    /** Applies an opacity factor (0.0 to 1.0) to an ARGB color. */
    fun applyOpacity(colorArgb: Long, opacity: Float): Long {
        val bounded = opacity.coerceIn(0f, 1f)
        val originalAlpha = ((colorArgb shr 24) and 0xFF).toFloat()
        val finalAlpha = (originalAlpha * bounded).toInt().coerceIn(0, 255)
        val rgb = colorArgb and 0x00FFFFFFL
        return (finalAlpha.toLong() shl 24) or rgb
    }

    /** Suggests a contrasting text color (White or Black) based on background luminance. */
    fun getContrastingTextColor(backgroundColorArgb: Long): Long {
        val r = ((backgroundColorArgb shr 16) and 0xFF) / 255f
        val g = ((backgroundColorArgb shr 8) and 0xFF) / 255f
        val b = (backgroundColorArgb and 0xFF) / 255f
        // Standard relative luminance formula
        val luminance = 0.299f * r + 0.587f * g + 0.114f * b
        return if (luminance > 0.55f) 0xFF121212L else 0xFFFFFFFFL
    }
}
