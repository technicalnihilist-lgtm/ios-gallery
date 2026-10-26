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
    val backgroundDeep: Color,
    val groupedBackground: Color,
    val card: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val tertiaryText: Color,
    val separator: Color,
    val accent: Color,
    val accentSoft: Color,
    val favorite: Color,
    val barBackground: Color,
    val barText: Color,
    val scrim: Color,
    val isDark: Boolean
)

/**
 * Tokens for the liquid-glass material.
 *
 * Panels are built from four layers: a translucent fill, a hairline border that is
 * brighter at the top edge, a diagonal specular sheen, and a soft drop shadow that
 * lifts the panel off the content behind it.
 */
@Immutable
data class GlassTokens(
    val fillTop: Color,
    val fillBottom: Color,
    val borderTop: Color,
    val borderBottom: Color,
    val sheen: Color,
    val shadow: Color,
    val highlight: Color,
    val tintedFill: Color,
    val scrimStart: Color,
    val scrimEnd: Color
)

private val LightPalette = GalleryPalette(
    background = Color(0xFFF4F6FB),
    backgroundDeep = Color(0xFFE8ECF6),
    groupedBackground = Color(0x66FFFFFF),
    card = Color(0xFFFFFFFF),
    primaryText = Color(0xFF11131A),
    secondaryText = Color(0xFF6B6F7B),
    tertiaryText = Color(0xFF9A9EAA),
    separator = Color(0x143C3C43),
    accent = Color(0xFF0A84FF),
    accentSoft = Color(0x1F0A84FF),
    favorite = Color(0xFFFF375F),
    barBackground = Color(0xCCFFFFFF),
    barText = Color(0xFF11131A),
    scrim = Color(0x59000000),
    isDark = false
)

private val DarkPalette = GalleryPalette(
    background = Color(0xFF0A0A0C),
    backgroundDeep = Color(0xFF000000),
    groupedBackground = Color(0x14FFFFFF),
    card = Color(0xFF17171B),
    primaryText = Color(0xFFFFFFFF),
    secondaryText = Color(0xFF9C9CA6),
    tertiaryText = Color(0xFF6C6C78),
    separator = Color(0x1FFFFFFF),
    accent = Color(0xFF4DA3FF),
    accentSoft = Color(0x334DA3FF),
    favorite = Color(0xFFFF375F),
    barBackground = Color(0xCC17171B),
    barText = Color(0xFFFFFFFF),
    scrim = Color(0x99000000),
    isDark = true
)

// Reference recipe: linear-gradient(135deg, white .38, white .10),
// border 1px white .48, inset 0 1px 1px white .6, shadow 0 8 24 black .13
private val LightGlass = GlassTokens(
    fillTop = Color(0x61FFFFFF),
    fillBottom = Color(0x1AFFFFFF),
    borderTop = Color(0x7AFFFFFF),
    borderBottom = Color(0x7AFFFFFF),
    sheen = Color(0x1FFFFFFF),
    shadow = Color(0xFF000000),
    highlight = Color(0x99FFFFFF),
    tintedFill = Color(0x330A84FF),
    scrimStart = Color(0x00F4F6FB),
    scrimEnd = Color(0xE6F4F6FB)
)

private val DarkGlass = GlassTokens(
    fillTop = Color(0x2EFFFFFF),
    fillBottom = Color(0x0DFFFFFF),
    borderTop = Color(0x3DFFFFFF),
    borderBottom = Color(0x3DFFFFFF),
    sheen = Color(0x14FFFFFF),
    shadow = Color(0xFF000000),
    highlight = Color(0x4DFFFFFF),
    tintedFill = Color(0x334DA3FF),
    scrimStart = Color(0x000A0A0C),
    scrimEnd = Color(0xF20A0A0C)
)

/** Scales the alpha of every glass layer, so the panels can be made clearer or more solid. */
private fun GlassTokens.withOpacity(factor: Float): GlassTokens {
    if (factor >= 0.999f) return this
    fun Color.fade(): Color = copy(alpha = (alpha * factor).coerceIn(0f, 1f))
    return copy(
        fillTop = fillTop.fade(),
        fillBottom = fillBottom.fade(),
        borderTop = borderTop.fade(),
        borderBottom = borderBottom.fade(),
        sheen = sheen.fade(),
        highlight = highlight.fade(),
        tintedFill = tintedFill.fade()
    )
}

val LocalGalleryPalette = staticCompositionLocalOf { LightPalette }
val LocalGlass = staticCompositionLocalOf { LightGlass }

object GalleryTheme {
    val palette: GalleryPalette
        @Composable get() = LocalGalleryPalette.current

    val glass: GlassTokens
        @Composable get() = LocalGlass.current
}

private val GalleryTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.7).sp
    ),
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
    glassOpacity: Float = 1f,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    val glass = (if (darkTheme) DarkGlass else LightGlass).withOpacity(glassOpacity)
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
    CompositionLocalProvider(
        LocalGalleryPalette provides palette,
        LocalGlass provides glass
    ) {
        MaterialTheme(colorScheme = scheme, typography = GalleryTypography, content = content)
    }
}
