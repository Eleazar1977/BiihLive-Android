package com.mision.biihlive.ui.theme

import androidx.compose.ui.graphics.Color

// ===========================================
// CORPORATE COLORS - Client Brand Identity
// ===========================================

// Primary - Celeste (Main brand color)
val BiihliveBlue = Color(0xFF1DC3FF)
val BiihliveBlueLight = Color(0xFF7DD3FC)
val BiihliveBlueContainer = Color(0xFFB3E5FF)
val BiihliveBlueContainerDark = Color(0xFF004D61)

// Secondary - Verde (Complementary color)
val BiihliveGreen = Color(0xFF60BF19)
val BiihliveGreenLight = Color(0xFFA8D982)
val BiihliveGreenContainer = Color(0xFFE1F5D1)
val BiihliveGreenContainerDark = Color(0xFF426B00)

// Tertiary/Accent - Naranja (Live/CTA color - Always prominent)
val BiihliveOrange = Color(0xFFDC5A01)
val BiihliveOrangeLight = Color(0xFFFF7300)
val BiihliveOrangeLighter = Color(0xE6FFB366) // 90% opacidad + más luminoso para badges
val BiihliveOrangeContainer = Color(0xFFFFDBCC)
val BiihliveOrangeContainerDark = Color(0xFF6A2F00)

// ===========================================
// NEUTRAL COLORS - Grays & Base Colors
// ===========================================

// Pure Colors
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF0C0C0C)
val PureBlack = Color(0xFF000000)

// Gray Scale - Light to Dark
val Gray50 = Color(0xFFF8FAFC)
val Gray100 = Color(0xFFF1F5F9)
val Gray200 = Color(0xFFE2E8F0)
val Gray300 = Color(0xFFCBD5E1)
val Gray400 = Color(0xFF94A3B8)
val Gray500 = Color(0xFF64748B)
val Gray600 = Color(0xFF475569)
val Gray700 = Color(0xFF334155)
val Gray800 = Color(0xFF1E293B)
val Gray900 = Color(0xFF0F172A)

// Custom Grays (from client specs)
val GrayLight = Color(0xFF706E6E)
val GrayMedium = Color(0xFF424242)
val GrayDark = Color(0xFF393939)
val GrayButton = Color(0xFF7C7C7C)

// Text Colors
val TextFieldGray = Color(0xFF4B4B4B) // Gris más oscuro para textos de campos

// ===========================================
// SEMANTIC COLORS - Functional Colors
// ===========================================

// Error States
val ErrorRed = Color(0xFFBA1A1A)
val ErrorRedLight = Color(0xFFFFB4AB)
val ErrorContainer = Color(0xFFFDDAD6)
val ErrorContainerDark = Color(0xFF93000A)

// Donation States (vibrant red for CTA)
val DonationRed = Color(0xFFFF4444)  // Rojo luminoso y vibrante
val DonationRedLight = Color(0xFFFF6B6B)
val DonationRedDark = Color(0xFFCC3333)

// Success States (using corporate green)
val Success = BiihliveGreen
val SuccessContainer = BiihliveGreenContainer

// Warning States
val Warning = Color(0xFFF59E0B)
val WarningContainer = Color(0xFFFEF3C7)

// Info States (using corporate blue)
val Info = BiihliveBlue
val InfoContainer = BiihliveBlueContainer

// ===========================================
// SURFACE COLORS - Light Theme
// ===========================================

val SurfaceLight = White
val SurfaceVariantLight = Color(0xFFF0F4F8)
val SurfaceContainerLowestLight = White
val SurfaceContainerLowLight = Color(0xFFF8FAFC)
val SurfaceContainerLight = Color(0xFFF1F5F9)
val SurfaceContainerHighLight = Color(0xFFE2E8F0)
val SurfaceContainerHighestLight = Color(0xFFCBD5E1)
val BackgroundLight = Color(0xFFFEFEFE)

// ===========================================
// SURFACE COLORS - Dark Theme
// ===========================================

val SurfaceDark = Color(0xFF0F1419)
val SurfaceVariantDark = Color(0xFF1E293B)
val SurfaceContainerLowestDark = Color(0xFF0A0E13)
val SurfaceContainerLowDark = Color(0xFF171C22)
val SurfaceContainerDark = Color(0xFF1B2028)
val SurfaceContainerHighDark = Color(0xFF252B33)
val SurfaceContainerHighestDark = Color(0xFF30373F)
val BackgroundDark = Color(0xFF0C1117)

// ===========================================
// TEXT COLORS
// ===========================================

// Light Theme Text
val OnSurfaceLight = Black
val OnSurfaceVariantLight = GrayMedium
val OnBackgroundLight = Black
val OnPrimaryLight = White
val OnSecondaryLight = White
val OnTertiaryLight = White
val OnErrorLight = White

// Dark Theme Text
val OnSurfaceDark = Color(0xFFE2E8F0)
val OnSurfaceVariantDark = Color(0xFFCBD5E1)
val OnBackgroundDark = Color(0xFFE2E8F0)
val OnPrimaryDark = Color(0xFF003544)
val OnSecondaryDark = Color(0xFF2D5100)
val OnTertiaryDark = Color(0xFF4A1C00)
val OnErrorDark = Color(0xFF690005)

// ===========================================
// SPECIAL PURPOSE COLORS
// ===========================================

// Live Streaming Specific
val LiveIndicatorActive = BiihliveOrange
val LiveIndicatorInactive = GrayButton
val LiveRecording = ErrorRed
val LivePulse = Color(0x80DC5A01) // 50% opacity orange

// Interactive States
val DisabledColor = Gray400
val DividerColor = Gray300
val OutlineColor = GrayLight
val OutlineVariant = Color(0xFFC4C7C5)

// Shadows and Overlays
val ShadowColor = PureBlack
val ScrimColor = Color(0x52000000) // 32% black
val OverlayLight = Color(0x0F000000) // 6% black
val OverlayDark = Color(0x1AFFFFFF) // 10% white

// ===========================================
// OPACITY VARIANTS - For special effects
// ===========================================

// Primary (Blue) with opacity
val PrimaryAlpha10 = Color(0x1A1DC3FF) // 10%
val PrimaryAlpha20 = Color(0x331DC3FF) // 20%
val PrimaryAlpha30 = Color(0x4D1DC3FF) // 30%
val PrimaryAlpha50 = Color(0x801DC3FF) // 50%
val PrimaryAlpha70 = Color(0xB31DC3FF) // 70%
val PrimaryAlpha90 = Color(0xE61DC3FF) // 90%

// Secondary (Green) with opacity
val SecondaryAlpha10 = Color(0x1A60BF19) // 10%
val SecondaryAlpha20 = Color(0x3360BF19) // 20%
val SecondaryAlpha30 = Color(0x4D60BF19) // 30%
val SecondaryAlpha50 = Color(0x8060BF19) // 50%
val SecondaryAlpha70 = Color(0xB360BF19) // 70%
val SecondaryAlpha90 = Color(0xE660BF19) // 90%

// Tertiary (Orange) with opacity
val TertiaryAlpha10 = Color(0x1ADC5A01) // 10%
val TertiaryAlpha20 = Color(0x33DC5A01) // 20%
val TertiaryAlpha30 = Color(0x4DDC5A01) // 30%
val TertiaryAlpha50 = Color(0x80DC5A01) // 50%
val TertiaryAlpha70 = Color(0xB3DC5A01) // 70%
val TertiaryAlpha90 = Color(0xE6DC5A01) // 90%

// Surface with opacity (for overlays)
val SurfaceAlpha10 = Color(0x1A0C0C0C) // 10%
val SurfaceAlpha20 = Color(0x330C0C0C) // 20%
val SurfaceAlpha30 = Color(0x4D0C0C0C) // 30%
val SurfaceAlpha50 = Color(0x800C0C0C) // 50%
val SurfaceAlpha70 = Color(0xB30C0C0C) // 70%
val SurfaceAlpha90 = Color(0xE60C0C0C) // 90%

// White with opacity (for dark theme overlays)
val WhiteAlpha10 = Color(0x1AFFFFFF) // 10%
val WhiteAlpha20 = Color(0x33FFFFFF) // 20%
val WhiteAlpha30 = Color(0x4DFFFFFF) // 30%
val WhiteAlpha50 = Color(0x80FFFFFF) // 50%
val WhiteAlpha70 = Color(0xB3FFFFFF) // 70%
val WhiteAlpha90 = Color(0xE6FFFFFF) // 90%