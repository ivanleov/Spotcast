package com.example.spotcast.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Dark_Primary,
    onPrimary = Dark_OnPrimary,
    secondary = Dark_Secondary,
    onSecondary = Dark_OnSecondary,
    tertiary = Dark_Tertiary,
    background = Dark_Background,
    surface = Dark_Surface,
    surfaceVariant = Dark_SurfaceVariant,
    onSurface = Dark_OnSurface,
    onSurfaceVariant = Dark_OnSurfaceVariant,
    error = Dark_Error,
    outline = Dark_Outline,
)

private val LightColorScheme = lightColorScheme(
    primary = Light_Primary,
    onPrimary = Light_OnPrimary,
    secondary = Light_Secondary,
    onSecondary = Light_OnSecondary,
    tertiary = Light_Tertiary,
    background = Light_Background,
    surface = Light_Surface,
    surfaceVariant = Light_SurfaceVariant,
    onSurface = Light_OnSurface,
    onSurfaceVariant = Light_OnSurfaceVariant,
    error = Light_Error,
    outline = Light_Outline,
)

@Composable
fun SpotcastTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}