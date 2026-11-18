package com.praveen.hypertrophy

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightFallback: ColorScheme = lightColorScheme(
    primary = Color(0xFF0A7C76),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBDEFEA),
    onPrimaryContainer = Color(0xFF00201E),
)

private val DarkFallback: ColorScheme = darkColorScheme(
    primary = Color(0xFF3FC5BC),
    onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF00504C),
    onPrimaryContainer = Color(0xFFBDEFEA),
)

/**
 * Material 3 Expressive with wallpaper-based dynamic color (Android 12 and up, which is
 * the app's minSdk) and the expressive spring motion scheme.
 */
@Composable
fun HypertrophyTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme: ColorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else {
        if (dark) DarkFallback else LightFallback
    }
    androidx.compose.material3.MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        content = content,
    )
}
