package com.example.snapcalories.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Our own fallback palettes (colors come from Color.kt). Only the roles we care about are set;
// Material fills in the rest (surface, background, error...) with sensible defaults.
private val LightColors = lightColorScheme(
    primary = Green40,
    secondary = Orange40
)

private val DarkColors = darkColorScheme(
    primary = Green80,
    secondary = Orange80
)

/**
 * The app theme. Wrap the whole UI in it once (in MainActivity). Inside, read colors with
 * `MaterialTheme.colorScheme.primary` instead of hard-coding them, so light/dark mode just works.
 *
 * `content: @Composable () -> Unit` as the LAST parameter lets callers use trailing-lambda syntax:
 * `SnapCaloriesTheme { MyScreen() }`.
 */
@Composable
fun SnapCaloriesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        // Android 12+ ("S"): "Material You" — colors derived from the user's wallpaper.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Older phones: fall back to our own palettes.
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
