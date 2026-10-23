package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Blue Schemes
private val BlueLightScheme = lightColorScheme(
    primary = BluePrimaryLight,
    onPrimary = BlueOnPrimaryLight,
    primaryContainer = BluePrimaryContainerLight,
    onPrimaryContainer = BlueOnPrimaryContainerLight,
    secondary = BlueSecondaryLight,
    onSecondary = Color.White,
    background = BlueBackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = BlueSurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = BlueSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFF1F5F9)
)

private val BlueDarkScheme = darkColorScheme(
    primary = BluePrimaryDark,
    onPrimary = BlueOnPrimaryDark,
    primaryContainer = BluePrimaryContainerDark,
    onPrimaryContainer = BlueOnPrimaryContainerDark,
    secondary = BlueSecondaryDark,
    onSecondary = Color.Black,
    background = BlueBackgroundDark,
    onBackground = Color(0xFFF8FAFC),
    surface = BlueSurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = BlueSurfaceVariantDark,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B)
)

// Orange Schemes
private val OrangeLightScheme = lightColorScheme(
    primary = OrangePrimaryLight,
    onPrimary = OrangeOnPrimaryLight,
    primaryContainer = OrangePrimaryContainerLight,
    onPrimaryContainer = OrangeOnPrimaryContainerLight,
    secondary = OrangeSecondaryLight,
    onSecondary = Color.White,
    background = OrangeBackgroundLight,
    onBackground = Color(0xFF1C1917),
    surface = OrangeSurfaceLight,
    onSurface = Color(0xFF1C1917),
    surfaceVariant = OrangeSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF57534E),
    outline = Color(0xFFE7E5E4),
    outlineVariant = Color(0xFFF5F5F4)
)

private val OrangeDarkScheme = darkColorScheme(
    primary = OrangePrimaryDark,
    onPrimary = OrangeOnPrimaryDark,
    primaryContainer = OrangePrimaryContainerDark,
    onPrimaryContainer = OrangeOnPrimaryContainerDark,
    secondary = OrangeSecondaryDark,
    onSecondary = Color.Black,
    background = OrangeBackgroundDark,
    onBackground = Color(0xFFFAFAF9),
    surface = OrangeSurfaceDark,
    onSurface = Color(0xFFFAFAF9),
    surfaceVariant = OrangeSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFA8A29E),
    outline = Color(0xFF44403C),
    outlineVariant = Color(0xFF292524)
)

// Green Schemes
private val GreenLightScheme = lightColorScheme(
    primary = GreenPrimaryLight,
    onPrimary = GreenOnPrimaryLight,
    primaryContainer = GreenPrimaryContainerLight,
    onPrimaryContainer = GreenOnPrimaryContainerLight,
    secondary = GreenSecondaryLight,
    onSecondary = Color.White,
    background = GreenBackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = GreenSurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = GreenSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFD1FAE5),
    outlineVariant = Color(0xFFE6F4EA)
)

private val GreenDarkScheme = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = GreenOnPrimaryDark,
    primaryContainer = GreenPrimaryContainerDark,
    onPrimaryContainer = GreenOnPrimaryContainerDark,
    secondary = GreenSecondaryDark,
    onSecondary = Color.Black,
    background = GreenBackgroundDark,
    onBackground = Color(0xFFF0FDF4),
    surface = GreenSurfaceDark,
    onSurface = Color(0xFFF0FDF4),
    surfaceVariant = GreenSurfaceVariantDark,
    onSurfaceVariant = Color(0xFF6EE7B7),
    outline = Color(0xFF064E3B),
    outlineVariant = Color(0xFF0F2B23)
)

@Composable
fun MicroWriteTheme(
    themeMode: String = "system",
    palette: String = "blue",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode.lowercase()) {
        "dark" -> true
        "light" -> false
        else -> isSystemDark
    }

    val colorScheme = when (palette.lowercase()) {
        "orange" -> if (isDark) OrangeDarkScheme else OrangeLightScheme
        "green" -> if (isDark) GreenDarkScheme else GreenLightScheme
        else -> if (isDark) BlueDarkScheme else BlueLightScheme
    }

    val heroGradient = when (palette.lowercase()) {
        "orange" -> OrangeHeroGradient
        "green" -> GreenHeroGradient
        else -> BlueHeroGradient
    }

    val extendedColors = ExtendedColors(
        success = if (isDark) Color(0xFF22C55E) else SuccessGreen,
        onSuccess = Color.White,
        successContainer = if (isDark) Color(0xFF14532D) else SuccessBg,
        warning = if (isDark) Color(0xFFFBBF24) else WarningAmber,
        onWarning = if (isDark) Color.Black else Color.White,
        warningContainer = if (isDark) Color(0xFF78350F) else WarningBg,
        danger = if (isDark) Color(0xFFF87171) else ErrorRed,
        onDanger = Color.White,
        dangerContainer = if (isDark) Color(0xFF7F1D1D) else ErrorBg,
        subtleBorder = if (isDark) Color(0xFF334155) else SubtleBorder,
        cardSurface = colorScheme.surface,
        heroGradient = heroGradient,
        textPrimary = colorScheme.onSurface,
        textSecondary = colorScheme.onSurfaceVariant,
        textMuted = if (isDark) Color(0xFF94A3B8) else TextMuted
    )

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

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current
