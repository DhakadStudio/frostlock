package com.frostlock.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun FrostLockTheme(
    palette: IcePalette = FrostThemeState.palette,
    content: @Composable () -> Unit
) {
    val colors = remember(palette) { FrostColors.from(palette) }

    // Deprecated compatibility shim. Screens written before the token migration
    // read the top-level colours in Color.kt, which still point at this state.
    // New code reads FrostTheme.colors from the CompositionLocal below.
    SideEffect { FrostThemeState.palette = palette }

    val colorScheme = darkColorScheme(
        background = colors.background,
        onBackground = colors.textPrimary,
        surface = colors.surfaceContainer,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.surfaceContainerHigh,
        onSurfaceVariant = colors.textSecondary,
        surfaceContainer = colors.surfaceContainer,
        surfaceContainerHigh = colors.surfaceContainerHigh,
        surfaceContainerHighest = colors.surfaceContainerHighest,
        primary = colors.primary,
        onPrimary = colors.onPrimary,
        secondary = colors.secondary,
        onSecondary = colors.onPrimary,
        tertiary = colors.tertiary,
        outline = colors.outline,
        outlineVariant = colors.outline,
        error = colors.danger
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

    CompositionLocalProvider(
        LocalFrostColors provides colors,
        LocalFrostTypography provides FrostType
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
