package com.progalleryx.app

import org.junit.Assert.*
import org.junit.Test

class ColorPaletteRulesTest {

    @Test
    fun parseHexColors() {
        // 6-digit hex
        val white = ColorPaletteRules.parseHexColor("#FFFFFF")
        assertEquals(0xFFFFFFFFL, white)

        val red = ColorPaletteRules.parseHexColor("FF3B30")
        assertEquals(0xFFFF3B30L, red)

        // 3-digit shorthand
        val shortWhite = ColorPaletteRules.parseHexColor("#FFF")
        assertEquals(0xFFFFFFFFL, shortWhite)

        // 8-digit ARGB
        val semiTrans = ColorPaletteRules.parseHexColor("#80FF0000")
        assertEquals(0x80FF0000L, semiTrans)

        // Invalid
        assertNull(ColorPaletteRules.parseHexColor("XYZ123"))
        assertNull(ColorPaletteRules.parseHexColor("#12"))
    }

    @Test
    fun formatHexColor() {
        val hexNoAlpha = ColorPaletteRules.formatHexColor(0xFFFF3B30L, includeAlpha = false)
        assertEquals("#FF3B30", hexNoAlpha)

        val hexAlpha = ColorPaletteRules.formatHexColor(0x80FF3B30L, includeAlpha = true)
        assertEquals("#80FF3B30", hexAlpha)
    }

    @Test
    fun applyOpacity() {
        val opaque = 0xFFFFFFFFL
        val half = ColorPaletteRules.applyOpacity(opaque, 0.5f)
        val alpha = (half shr 24) and 0xFF
        assertTrue(alpha in 127..128)
    }

    @Test
    fun getContrastingTextColor() {
        // White background -> black text
        val textOnWhite = ColorPaletteRules.getContrastingTextColor(0xFFFFFFFFL)
        assertEquals(0xFF121212L, textOnWhite)

        // Black background -> white text
        val textOnBlack = ColorPaletteRules.getContrastingTextColor(0xFF121212L)
        assertEquals(0xFFFFFFFFL, textOnBlack)

        // Bright yellow background -> black text
        val textOnYellow = ColorPaletteRules.getContrastingTextColor(0xFFFFCC00L)
        assertEquals(0xFF121212L, textOnYellow)
    }

    @Test
    fun studioColorsPaletteHasRichSelection() {
        assertTrue(ColorPaletteRules.STUDIO_COLORS.size >= 10)
        assertTrue(ColorPaletteRules.STUDIO_COLORS.contains(0xFFFFFFFFL))
        assertTrue(ColorPaletteRules.STUDIO_COLORS.contains(0xFF121212L))
    }
}
