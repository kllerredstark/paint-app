package com.redstars.artvault.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

val Serif = FontFamily.Serif

private val Light = lightColorScheme(
    primary = Color(0xFFB4502E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4502E),
    onPrimaryContainer = Color.White,
    secondaryContainer = Color(0xFF1F1D1A),
    onSecondaryContainer = Color(0xFFF4F1EA),
    background = Color(0xFFF4F1EA),
    onBackground = Color(0xFF1F1D1A),
    surface = Color(0xFFF4F1EA),
    onSurface = Color(0xFF1F1D1A),
    surfaceVariant = Color(0xFFEBE6DC),
    onSurfaceVariant = Color(0xFF6F6A61),
    surfaceContainerLow = Color(0xFFFBF9F4),
    surfaceContainer = Color(0xFFFBF9F4),
    surfaceContainerHigh = Color(0xFFFBF9F4),
    outline = Color(0xFFD8D2C6),
    outlineVariant = Color(0xFFE3DED3),
    error = Color(0xFFC0392B),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFE07A52),
    onPrimary = Color(0xFF1A0E08),
    primaryContainer = Color(0xFFE07A52),
    onPrimaryContainer = Color(0xFF1A0E08),
    secondaryContainer = Color(0xFFEFEBE3),
    onSecondaryContainer = Color(0xFF151412),
    background = Color(0xFF151412),
    onBackground = Color(0xFFEFEBE3),
    surface = Color(0xFF151412),
    onSurface = Color(0xFFEFEBE3),
    surfaceVariant = Color(0xFF2A2723),
    onSurfaceVariant = Color(0xFFA29C91),
    surfaceContainerLow = Color(0xFF1D1B19),
    surfaceContainer = Color(0xFF1D1B19),
    surfaceContainerHigh = Color(0xFF1D1B19),
    outline = Color(0xFF3A3631),
    outlineVariant = Color(0xFF2E2B27),
    error = Color(0xFFE57368),
)

@Composable
fun ArtVaultTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = Typography(),
        content = content,
    )
}
