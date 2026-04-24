package com.oneuihomeclone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = OneUiAccent,
    secondary = OneUiAccent,
    background = OneUiBackground,
    surface = OneUiSurface,
    surfaceVariant = OneUiSurfaceSoft,
    onPrimary = OneUiSurface,
    onBackground = OneUiText,
    onSurface = OneUiText,
    onSurfaceVariant = OneUiTextSecondary,
)

private val DarkColors = darkColorScheme(
    primary = OneUiAccent,
)

@Composable
fun OneUiHomeCloneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
