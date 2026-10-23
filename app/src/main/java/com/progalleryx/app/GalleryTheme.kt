package com.progalleryx.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightPrimary = Color(0xFF1D61D2)
private val LightOnPrimary = Color.White
private val LightBackground = Color(0xFFF7F9FC)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFEFF2F7)
private val LightOnSurface = Color(0xFF191C20)
private val LightOutline = Color(0xFFD2D8E2)

private val DarkPrimary = Color(0xFF6DA3FF)
private val DarkOnPrimary = Color(0xFF002E6C)
private val DarkBackground = Color(0xFF0F1216)
private val DarkSurface = Color(0xFF181C22)
private val DarkSurfaceVariant = Color(0xFF242A34)
private val DarkOnSurface = Color(0xFFE4E8EE)
private val DarkOutline = Color(0xFF38404E)

private val ProGalleryLightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurface = LightOnSurface,
    outline = LightOutline
)

private val ProGalleryDarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurface = DarkOnSurface,
    outline = DarkOutline
)

@Composable
fun ProGalleryTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = AppPreferences.isDarkTheme(themeMode, isSystemInDarkTheme())
    val colorScheme = if (isDark) ProGalleryDarkColors else ProGalleryLightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
