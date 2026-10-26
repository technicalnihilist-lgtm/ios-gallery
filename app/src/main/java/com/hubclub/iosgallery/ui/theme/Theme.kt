package com.hubclub.iosgallery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class GalleryPalette(
    val background: Color,
    val groupedBackground: Color,
    val card: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val separator: Color,
    val accent: Color,
    val favorite: Color,
    val barBackground: Color,
    val barText: Color,
    val scrim: Color,
    val isDark: Boolean
)

private val LightPalette = GalleryPalette(
    background = Color(0xFFFFFFFF),
    groupedBackground = Color(0xFFF2F2F7),
    card = Color(0xFFFFFFFF),
    primaryText = Color(0xFF1C1C1E),
    secondaryText = Color(0xFF7C7C82),
    tertiaryText = Color(0xFFAEAEB2),
    separator = Color(0x1A3C3C43),
    accent = Color(0xFF0A84FF),
    favorite = Color(0xFFFF375F),
    barBackground = Color(0xF7F9F9FB),
    barText = Color(0xFF1C1C1E),
    scrim = Color(0x66000000),
    isDark = false
)

private val DarkPalette = GalleryPalette(
    background = Color(0xFF000000),
    groupedBackground = Color(0xFF1C1C1E),
    card = Color(0xFF1C1C1E),
    primaryText = Color(0xFFFFFFFF),
    secondaryText = Color(0xFF9E9EA4),
    tertiaryText = Color(0xFF6C6C70),
    separator = Color(0x2EFFFFFF),
    accent = Color(0xFF0A84FF),
    favorite = Color(0xFFFF375F),
    barBackground = Color(0xF7121214),
    barText = Color(0xFFFFFFFF),
    scrim = Color(0x99000000),
    isDark = true
)

val LocalGalleryPalette = staticCompositionLocalOf { LightPalette }

object GalleryTheme {
    val palette: GalleryPalette
        @Composable get() = LocalGalleryPalette.current
}

private val GalleryTypography = Typography(
    // Large navigation titles, e.g. "Library"
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.6).sp
    ),
    // Section headers, e.g. "Today"
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.1.sp
    )
)

@Composable
fun IOSGalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = Color.White,
            background = palette.background,
            onBackground = palette.primaryText,
            surface = palette.card,
            onSurface = palette.primaryText,
            surfaceVariant = palette.groupedBackground,
            onSurfaceVariant = palette.secondaryText,
            outline = palette.separator,
            error = Color(0xFFFF453A)
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = Color.White,
            background = palette.background,
            onBackground = palette.primaryText,
            surface = palette.card,
            onSurface = palette.primaryText,
            surfaceVariant = palette.groupedBackground,
            onSurfaceVariant = palette.secondaryText,
            outline = palette.separator,
            error = Color(0xFFD70015)
        )
    }
    CompositionLocalProvider(LocalGalleryPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = GalleryTypography, content = content)
    }
}
