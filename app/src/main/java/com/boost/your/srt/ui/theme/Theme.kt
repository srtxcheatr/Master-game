package com.boost.your.srt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BoostColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = BgPrimary,
    secondary = AccentAmber,
    onSecondary = BgPrimary,
    tertiary = AccentPurple,
    background = BgPrimary,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgSideNav,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    error = AccentRed,
    onError = TextPrimary
)

@Composable
fun BoostMasterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BoostColorScheme,
        typography = BoostTypography,
        content = content
    )
}
