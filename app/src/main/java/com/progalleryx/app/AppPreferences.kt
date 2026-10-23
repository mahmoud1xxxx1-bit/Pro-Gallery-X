package com.progalleryx.app

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage {
    SYSTEM,
    ARABIC,
    ENGLISH
}

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("progalleryx_settings", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
            return runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
        }
        set(value) {
            prefs.edit().putString(KEY_THEME, value.name).apply()
        }

    var language: AppLanguage
        get() {
            val name = prefs.getString(KEY_LANGUAGE, AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name
            return runCatching { AppLanguage.valueOf(name) }.getOrDefault(AppLanguage.SYSTEM)
        }
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE, value.name).apply()
        }

    companion object {
        private const val KEY_THEME = "app_theme_mode"
        private const val KEY_LANGUAGE = "app_language"

        fun isArabicLanguage(language: AppLanguage, systemLocaleIsArabic: Boolean): Boolean {
            return when (language) {
                AppLanguage.ARABIC -> true
                AppLanguage.ENGLISH -> false
                AppLanguage.SYSTEM -> systemLocaleIsArabic
            }
        }

        fun isDarkTheme(themeMode: ThemeMode, systemIsDark: Boolean): Boolean {
            return when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemIsDark
            }
        }
    }
}
