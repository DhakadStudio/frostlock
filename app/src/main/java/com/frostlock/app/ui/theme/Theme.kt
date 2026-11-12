package com.frostlock.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceCard,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHigh,
    primary = PrimaryIce,
    onPrimary = InkOnIce,
    secondary = AccentFrost,
    onSecondary = InkOnIce,
    tertiary = GlowIce,
    outline = StrokeSoft,
    outlineVariant = StrokeSoft,
    error = DangerRed
)

@Composable
fun FrostLockTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
