package com.progalleryx.app

import org.junit.Assert.*
import org.junit.Test

class AppPreferencesRulesTest {

    @Test
    fun testIsArabicLanguage() {
        assertTrue(AppPreferences.isArabicLanguage(AppLanguage.ARABIC, systemLocaleIsArabic = false))
        assertTrue(AppPreferences.isArabicLanguage(AppLanguage.ARABIC, systemLocaleIsArabic = true))

        assertFalse(AppPreferences.isArabicLanguage(AppLanguage.ENGLISH, systemLocaleIsArabic = false))
        assertFalse(AppPreferences.isArabicLanguage(AppLanguage.ENGLISH, systemLocaleIsArabic = true))

        assertTrue(AppPreferences.isArabicLanguage(AppLanguage.SYSTEM, systemLocaleIsArabic = true))
        assertFalse(AppPreferences.isArabicLanguage(AppLanguage.SYSTEM, systemLocaleIsArabic = false))
    }

    @Test
    fun testIsDarkTheme() {
        assertTrue(AppPreferences.isDarkTheme(ThemeMode.DARK, systemIsDark = false))
        assertTrue(AppPreferences.isDarkTheme(ThemeMode.DARK, systemIsDark = true))

        assertFalse(AppPreferences.isDarkTheme(ThemeMode.LIGHT, systemIsDark = false))
        assertFalse(AppPreferences.isDarkTheme(ThemeMode.LIGHT, systemIsDark = true))

        assertTrue(AppPreferences.isDarkTheme(ThemeMode.SYSTEM, systemIsDark = true))
        assertFalse(AppPreferences.isDarkTheme(ThemeMode.SYSTEM, systemIsDark = false))
    }

    @Test
    fun testRecentStickersParsingAndAppending() {
        val initial = listOf("😀", "❤️", "⭐")
        val formatted = AppPreferences.formatRecentStickers(initial)
        assertEquals("😀###❤️###⭐", formatted)

        val parsed = AppPreferences.parseRecentStickers(formatted)
        assertEquals(3, parsed.size)
        assertEquals("😀", parsed[0])
        assertEquals("❤️", parsed[1])
        assertEquals("⭐", parsed[2])

        // Append a new sticker (moves to head)
        val updated = AppPreferences.appendRecentSticker(parsed, "🔥")
        assertEquals(4, updated.size)
        assertEquals("🔥", updated[0])
        assertEquals("😀", updated[1])

        // Append existing sticker (deduplicates and bumps to head)
        val reBumped = AppPreferences.appendRecentSticker(updated, "❤️")
        assertEquals(4, reBumped.size)
        assertEquals("❤️", reBumped[0])
        assertEquals("🔥", reBumped[1])
        assertEquals("😀", reBumped[2])
        assertEquals("⭐", reBumped[3])

        // Empty/blank ignores
        val same = AppPreferences.appendRecentSticker(reBumped, "   ")
        assertEquals(reBumped, same)
    }
}
