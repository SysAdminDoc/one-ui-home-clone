package com.oneuihomeclone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = OneUiAccent,
    onPrimary = OneUiSurface,
    primaryContainer = OneUiAccentSoft,
    onPrimaryContainer = OneUiAccent,
    secondary = OneUiAccent,
    onSecondary = OneUiSurface,
    secondaryContainer = OneUiAccentSoft,
    onSecondaryContainer = OneUiAccent,
    tertiary = OneUiPositive,
    onTertiary = OneUiSurface,
    background = OneUiBackground,
    onBackground = OneUiText,
    surface = OneUiSurface,
    onSurface = OneUiText,
    surfaceVariant = OneUiSurfaceSoft,
    onSurfaceVariant = OneUiTextSecondary,
    surfaceTint = OneUiAccent,
    outline = OneUiBorder,
    outlineVariant = OneUiBorder,
)

private val DarkColors = darkColorScheme(
    primary = OneUiAccentDark,
    onPrimary = OneUiBackgroundDark,
    primaryContainer = OneUiAccentSoftDark,
    onPrimaryContainer = OneUiAccentDark,
    secondary = OneUiAccentDark,
    onSecondary = OneUiBackgroundDark,
    secondaryContainer = OneUiAccentSoftDark,
    onSecondaryContainer = OneUiAccentDark,
    tertiary = OneUiPositiveDark,
    onTertiary = OneUiBackgroundDark,
    background = OneUiBackgroundDark,
    onBackground = OneUiTextDark,
    surface = OneUiSurfaceDark,
    onSurface = OneUiTextDark,
    surfaceVariant = OneUiSurfaceSoftDark,
    onSurfaceVariant = OneUiTextSecondaryDark,
    surfaceTint = OneUiAccentDark,
    outline = OneUiBorderDark,
    outlineVariant = OneUiBorderDark,
)

@Composable
fun OneUiHomeCloneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
