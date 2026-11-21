package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ManoDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = TextOnGreen,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = MintAccent,
    secondary = EmeraldVibrant,
    onSecondary = TextOnGreen,
    secondaryContainer = CarbonHighlight,
    onSecondaryContainer = TextPrimary,
    tertiary = MintAccent,
    onTertiary = TextOnGreen,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = CarbonSurface,
    onSurface = TextPrimary,
    surfaceVariant = CarbonElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = CarbonHighlight,
    error = ErrorRed,
    onError = TextPrimary,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorRed
)

@Composable
fun ManoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ManoDarkColorScheme,
        typography = ManoTypography,
        content = content
    )
}
