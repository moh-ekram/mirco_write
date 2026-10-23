package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ======================================================================
// PALETTE: BLUE (Fintech Ocean / Classic)
// ======================================================================
val BluePrimaryLight = Color(0xFF1D4ED8) // Deep vibrant blue
val BlueOnPrimaryLight = Color(0xFFFFFFFF)
val BluePrimaryContainerLight = Color(0xFFEFF6FF)
val BlueOnPrimaryContainerLight = Color(0xFF1E3A8A)
val BlueSecondaryLight = Color(0xFF0284C7)
val BlueBackgroundLight = Color(0xFFF6F8FC) // Soft tinted background
val BlueSurfaceLight = Color(0xFFFFFFFF) // Pure white floating card
val BlueSurfaceVariantLight = Color(0xFFF1F5F9)

val BluePrimaryDark = Color(0xFF60A5FA)
val BlueOnPrimaryDark = Color(0xFF0F172A)
val BluePrimaryContainerDark = Color(0xFF1E3A8A)
val BlueOnPrimaryContainerDark = Color(0xFFDBEAFE)
val BlueSecondaryDark = Color(0xFF38BDF8)
val BlueBackgroundDark = Color(0xFF0B1120)
val BlueSurfaceDark = Color(0xFF151F32)
val BlueSurfaceVariantDark = Color(0xFF1E293B)

val BlueHeroGradient = Brush.linearGradient(
    listOf(Color(0xFF1E40AF), Color(0xFF2563EB), Color(0xFF38BDF8))
)

// ======================================================================
// PALETTE: ORANGE (Warm Sunset Fintech)
// ======================================================================
val OrangePrimaryLight = Color(0xFFEA580C)
val OrangeOnPrimaryLight = Color(0xFFFFFFFF)
val OrangePrimaryContainerLight = Color(0xFFFFF7ED)
val OrangeOnPrimaryContainerLight = Color(0xFF9A3412)
val OrangeSecondaryLight = Color(0xFFD97706)
val OrangeBackgroundLight = Color(0xFFFAF7F4)
val OrangeSurfaceLight = Color(0xFFFFFFFF)
val OrangeSurfaceVariantLight = Color(0xFFFBF4ED)

val OrangePrimaryDark = Color(0xFFFB923C)
val OrangeOnPrimaryDark = Color(0xFF1C1917)
val OrangePrimaryContainerDark = Color(0xFF7C2D12)
val OrangeOnPrimaryContainerDark = Color(0xFFFFEDD5)
val OrangeSecondaryDark = Color(0xFFFBBF24)
val OrangeBackgroundDark = Color(0xFF14100E)
val OrangeSurfaceDark = Color(0xFF221B17)
val OrangeSurfaceVariantDark = Color(0xFF2C241F)

val OrangeHeroGradient = Brush.linearGradient(
    listOf(Color(0xFFC2410C), Color(0xFFEA580C), Color(0xFFF59E0B))
)

// ======================================================================
// PALETTE: GREEN (Emerald Mint Fintech)
// ======================================================================
val GreenPrimaryLight = Color(0xFF059669)
val GreenOnPrimaryLight = Color(0xFFFFFFFF)
val GreenPrimaryContainerLight = Color(0xFFECFDF5)
val GreenOnPrimaryContainerLight = Color(0xFF064E3B)
val GreenSecondaryLight = Color(0xFF0D9488)
val GreenBackgroundLight = Color(0xFFF3F8F5)
val GreenSurfaceLight = Color(0xFFFFFFFF)
val GreenSurfaceVariantLight = Color(0xFFEDF6F1)

val GreenPrimaryDark = Color(0xFF34D399)
val GreenOnPrimaryDark = Color(0xFF022C22)
val GreenPrimaryContainerDark = Color(0xFF064E3B)
val GreenOnPrimaryContainerDark = Color(0xFFA7F3D0)
val GreenSecondaryDark = Color(0xFF2DD4BF)
val GreenBackgroundDark = Color(0xFF081C16)
val GreenSurfaceDark = Color(0xFF0F2B23)
val GreenSurfaceVariantDark = Color(0xFF163C31)

val GreenHeroGradient = Brush.linearGradient(
    listOf(Color(0xFF065F46), Color(0xFF059669), Color(0xFF34D399))
)

// ======================================================================
// UNIVERSAL / EXTENDED SEMANTIC COLORS
// ======================================================================
val SuccessGreen = Color(0xFF16A34A)
val SuccessBg = Color(0xFFDCFCE7)
val SuccessOn = Color(0xFF14532D)

val WarningAmber = Color(0xFFD97706)
val WarningBg = Color(0xFFFEF3C7)
val WarningOn = Color(0xFF78350F)

val ErrorRed = Color(0xFFDC2626)
val ErrorBg = Color(0xFFFEE2E2)
val ErrorOn = Color(0xFF7F1D1D)

// Backward compatibility colors for existing composables
val SlateNavy900 = Color(0xFF0F172A)
val SlateNavy800 = Color(0xFF1E293B)
val SlateNavy700 = Color(0xFF334155)
val AcademicNavy = BluePrimaryLight
val AcademicBlue = BlueSecondaryLight
val SoftBlueContainer = BluePrimaryContainerLight
val SurfaceBg = BlueBackgroundLight
val CardSurface = Color(0xFFFFFFFF)
val SubtleBorder = Color(0xFFE2E8F0)
val BorderFocused = Color(0xFF94A3B8)
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)
val TextMuted = Color(0xFF64748B)

// Extended theme data class for semantic colors
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val subtleBorder: Color,
    val cardSurface: Color,
    val heroGradient: Brush,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        success = SuccessGreen,
        onSuccess = Color.White,
        successContainer = SuccessBg,
        warning = WarningAmber,
        onWarning = Color.White,
        warningContainer = WarningBg,
        danger = ErrorRed,
        onDanger = Color.White,
        dangerContainer = ErrorBg,
        subtleBorder = SubtleBorder,
        cardSurface = CardSurface,
        heroGradient = BlueHeroGradient,
        textPrimary = TextPrimary,
        textSecondary = TextSecondary,
        textMuted = TextMuted
    )
}
