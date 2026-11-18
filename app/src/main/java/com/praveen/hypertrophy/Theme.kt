package com.praveen.hypertrophy

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// UI/UX Pro Max: Dark High-Contrast Fitness Editorial Theme
private val FitnessVolt = Color(0xFFE2F952)
private val FitnessDarkCanvas = Color(0xFF0E0E10)
private val FitnessSurfaceLow = Color(0xFF18181C)
private val FitnessSurfaceHigh = Color(0xFF222228)
private val FitnessOnSurface = Color(0xFFF2F2F5)
private val FitnessMuted = Color(0xFFA0A0AB)
private val FitnessError = Color(0xFFFF5555)
private val FitnessErrorContainer = Color(0xFF3B1517)

private val EditorialDarkColorScheme: ColorScheme = darkColorScheme(
    primary = FitnessVolt,
    onPrimary = Color(0xFF141A00),
    primaryContainer = Color(0xFF374000),
    onPrimaryContainer = Color(0xFFE8FFA6),
    surface = FitnessDarkCanvas,
    onSurface = FitnessOnSurface,
    surfaceContainer = FitnessSurfaceLow,
    surfaceContainerLow = FitnessSurfaceLow,
    surfaceContainerHigh = FitnessSurfaceHigh,
    surfaceContainerHighest = Color(0xFF2C2C34),
    onSurfaceVariant = FitnessMuted,
    outline = Color(0xFF3F3F4A),
    error = FitnessError,
    onError = Color.White,
    errorContainer = FitnessErrorContainer,
    onErrorContainer = Color(0xFFFFD1D2),
)

private val EditorialLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF536500),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EE48),
    onPrimaryContainer = Color(0xFF181F00),
    surface = Color(0xFFFBFBFF),
    onSurface = Color(0xFF191C1E),
    surfaceContainerLow = Color(0xFFF0F0F5),
)

@Composable
fun HypertrophyTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) EditorialDarkColorScheme else EditorialLightColorScheme,
        typography = Typography(),
        content = content,
    )
}
