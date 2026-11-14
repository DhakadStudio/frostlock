package com.frostlock.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun FrostLockTheme(
    palette: IcePalette = FrostThemeState.palette,
    content: @Composable () -> Unit
) {
    // Assign before composing the content so every screen sees the new palette
    // in the same frame. The colour getters read this state, so a change
    // recomposes the whole tree.
    FrostThemeState.palette = palette

    val colorScheme = darkColorScheme(
        background = palette.background,
        onBackground = palette.textPrimary,
        surface = palette.surfaceCard,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.surfaceHigh,
        onSurfaceVariant = palette.textSecondary,
        surfaceContainer = palette.surfaceCard,
        surfaceContainerHigh = palette.surfaceHigh,
        surfaceContainerHighest = palette.surfaceHigh,
        primary = palette.primary,
        onPrimary = palette.ink,
        secondary = palette.accent,
        onSecondary = palette.ink,
        tertiary = palette.glow,
        outline = palette.strokeSoft,
        outlineVariant = palette.strokeSoft,
        error = DangerRed
    )

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
