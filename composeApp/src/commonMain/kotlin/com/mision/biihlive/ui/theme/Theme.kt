package com.mision.biihlive.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light Color Scheme
private val LightColorScheme = lightColorScheme(
    // Primary colors - Celeste/Blue
    primary = BiihliveBlue,
    onPrimary = OnPrimaryLight,
    primaryContainer = BiihliveBlueContainer,
    onPrimaryContainer = Black,
    inversePrimary = BiihliveBlueLight,
    
    // Secondary colors - Verde/Green
    secondary = BiihliveGreen,
    onSecondary = OnSecondaryLight,
    secondaryContainer = BiihliveGreenContainer,
    onSecondaryContainer = Black,
    
    // Tertiary colors - Naranja/Orange (for Live button and CTAs)
    tertiary = BiihliveOrange,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = BiihliveOrangeContainer,
    onTertiaryContainer = Black,
    
    // Error colors
    error = ErrorRed,
    onError = OnErrorLight,
    errorContainer = ErrorContainer,
    onErrorContainer = Black,
    
    // Background colors
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    
    // Surface colors
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = BiihliveBlue,
    inverseSurface = Gray800,
    inverseOnSurface = White,
    
    // Outline colors
    outline = OutlineColor,
    outlineVariant = OutlineVariant,
    
    // Other
    scrim = ScrimColor
)

// Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
    // Primary colors - Celeste/Blue
    primary = BiihliveBlueLight,
    onPrimary = OnPrimaryDark,
    primaryContainer = BiihliveBlueContainerDark,
    onPrimaryContainer = BiihliveBlueContainer,
    inversePrimary = BiihliveBlue,
    
    // Secondary colors - Verde/Green
    secondary = BiihliveGreenLight,
    onSecondary = OnSecondaryDark,
    secondaryContainer = BiihliveGreenContainerDark,
    onSecondaryContainer = BiihliveGreenContainer,
    
    // Tertiary colors - Naranja/Orange (for Live button and CTAs)
    tertiary = BiihliveOrangeLight,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = BiihliveOrangeContainerDark,
    onTertiaryContainer = BiihliveOrangeContainer,
    
    // Error colors
    error = ErrorRedLight,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorContainer,
    
    // Background colors
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    
    // Surface colors
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = BiihliveBlueLight,
    inverseSurface = Gray200,
    inverseOnSurface = Gray900,
    
    // Outline colors
    outline = Gray600,
    outlineVariant = Gray700,
    
    // Other
    scrim = ScrimColor
)

// Extended colors for special purposes
@Immutable
data class ExtendedColors(
    // Live streaming specific
    val liveIndicator: Color,
    val liveRecording: Color,
    val liveInactive: Color,
    val livePulse: Color,
    
    // Interactive states
    val disabled: Color,
    val divider: Color,
    
    // Surface containers (Material 3 style)
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    
    // Success and warning
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val info: Color,
    val infoContainer: Color,
    
    // Overlays
    val overlay: Color,
    
    // Opacity variants
    val primaryAlpha10: Color,
    val primaryAlpha20: Color,
    val primaryAlpha30: Color,
    val primaryAlpha50: Color,
    val primaryAlpha70: Color,
    val primaryAlpha90: Color,
    
    val tertiaryAlpha10: Color,
    val tertiaryAlpha20: Color,
    val tertiaryAlpha30: Color,
    val tertiaryAlpha50: Color,
    val tertiaryAlpha70: Color,
    val tertiaryAlpha90: Color
)

// Light extended colors
val LightExtendedColors = ExtendedColors(
    liveIndicator = LiveIndicatorActive,
    liveRecording = LiveRecording,
    liveInactive = LiveIndicatorInactive,
    livePulse = LivePulse,
    disabled = DisabledColor,
    divider = DividerColor,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    success = Success,
    successContainer = SuccessContainer,
    warning = Warning,
    warningContainer = WarningContainer,
    info = Info,
    infoContainer = InfoContainer,
    overlay = OverlayLight,
    primaryAlpha10 = PrimaryAlpha10,
    primaryAlpha20 = PrimaryAlpha20,
    primaryAlpha30 = PrimaryAlpha30,
    primaryAlpha50 = PrimaryAlpha50,
    primaryAlpha70 = PrimaryAlpha70,
    primaryAlpha90 = PrimaryAlpha90,
    tertiaryAlpha10 = TertiaryAlpha10,
    tertiaryAlpha20 = TertiaryAlpha20,
    tertiaryAlpha30 = TertiaryAlpha30,
    tertiaryAlpha50 = TertiaryAlpha50,
    tertiaryAlpha70 = TertiaryAlpha70,
    tertiaryAlpha90 = TertiaryAlpha90
)

// Dark extended colors
val DarkExtendedColors = ExtendedColors(
    liveIndicator = LiveIndicatorActive,
    liveRecording = LiveRecording,
    liveInactive = Gray600,
    livePulse = LivePulse,
    disabled = Gray600,
    divider = Gray700,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    success = BiihliveGreenLight,
    successContainer = BiihliveGreenContainerDark,
    warning = Warning,
    warningContainer = Color(0xFF7C2E00),
    info = BiihliveBlueLight,
    infoContainer = BiihliveBlueContainerDark,
    overlay = OverlayDark,
    primaryAlpha10 = PrimaryAlpha10,
    primaryAlpha20 = PrimaryAlpha20,
    primaryAlpha30 = PrimaryAlpha30,
    primaryAlpha50 = PrimaryAlpha50,
    primaryAlpha70 = PrimaryAlpha70,
    primaryAlpha90 = PrimaryAlpha90,
    tertiaryAlpha10 = TertiaryAlpha10,
    tertiaryAlpha20 = TertiaryAlpha20,
    tertiaryAlpha30 = TertiaryAlpha30,
    tertiaryAlpha50 = TertiaryAlpha50,
    tertiaryAlpha70 = TertiaryAlpha70,
    tertiaryAlpha90 = TertiaryAlpha90
)

// Local composition for extended colors
val LocalExtendedColors = staticCompositionLocalOf {
    LightExtendedColors
}

// Extension property to access extended colors easily
val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    get() = LocalExtendedColors.current

@Composable
fun BiihliveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    
    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}