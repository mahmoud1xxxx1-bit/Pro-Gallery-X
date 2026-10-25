package com.progalleryx.app.studioplus.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Samsung One UI inspired color scheme
val SamsungBlue = Color(0xFF2E6EEF)
val SamsungBlueLight = Color(0xFF538CFF)
val SamsungBlueDark = Color(0xFF1E52C0)

val StudioPitchBlack = Color(0xFF000000)
val StudioDarkSurface = Color(0xFF121418)
val StudioDarkElevated = Color(0xFF1E222A)
val StudioDarkCard = Color(0xFF262B35)
val StudioDarkBorder = Color(0xFF333A48)

val StudioLightBg = Color(0xFFF7F9FC)
val StudioLightSurface = Color(0xFFFFFFFF)
val StudioLightElevated = Color(0xFFEFF2F7)
val StudioLightCard = Color(0xFFE4E8F0)
val StudioLightBorder = Color(0xFFD6DCE7)

private val DarkColorScheme = darkColorScheme(
    primary = SamsungBlueLight,
    onPrimary = Color.White,
    primaryContainer = SamsungBlueDark,
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFF74C0FC),
    onSecondary = Color.Black,
    background = StudioPitchBlack,
    onBackground = Color(0xFFE6EBF5),
    surface = StudioDarkSurface,
    onSurface = Color(0xFFE6EBF5),
    surfaceVariant = StudioDarkElevated,
    onSurfaceVariant = Color(0xFFB0B8C8),
    outline = StudioDarkBorder,
    error = Color(0xFFFF6B6B)
)

private val LightColorScheme = lightColorScheme(
    primary = SamsungBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = SamsungBlueDark,
    secondary = Color(0xFF228BE6),
    onSecondary = Color.White,
    background = StudioLightBg,
    onBackground = Color(0xFF1A1F2C),
    surface = StudioLightSurface,
    onSurface = Color(0xFF1A1F2C),
    surfaceVariant = StudioLightElevated,
    onSurfaceVariant = Color(0xFF5A6478),
    outline = StudioLightBorder,
    error = Color(0xFFE03131)
)

val StudioPlusShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val StudioPlusTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun StudioPlusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = StudioPlusShapes,
        typography = StudioPlusTypography,
        content = content
    )
}
