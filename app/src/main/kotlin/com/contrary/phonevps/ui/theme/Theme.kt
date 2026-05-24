package com.contrary.phonevps.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberPurple,
    onPrimary = Color.White,
    primaryContainer = CyberPurpleDark,
    onPrimaryContainer = CyberPurpleLight,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = NeonCyanDark,
    onSecondaryContainer = Color.White,
    tertiary = ElectricBlue,
    onTertiary = Color.White,
    background = DeepNavy,
    onBackground = Color(0xFFE2E8F0),
    surface = CardSurface,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = CardSurfaceElevated,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    error = StatusRed,
    onError = Color.White,
)

@Composable
fun ContraryPhoneVPSTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
