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
}
