package com.frostlock.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/** The active colour roles. Replaced only by [FrostLockTheme]. */
val LocalFrostColors = staticCompositionLocalOf { FrostColors.from(IcePalettes.Frost) }

/** The active typography tokens. */
val LocalFrostTypography = staticCompositionLocalOf { FrostType }

/**
 * Single entry point for the redesign design system.
 *
 * ```kotlin
 * FrostTheme.colors.surfaceContainer
 * FrostTheme.typography.titleLarge
 * FrostTheme.shapes.large
 * FrostTheme.spacing.xxl
 * FrostTheme.motion.defaultSpatial
 * ```
 */
object FrostTheme {
    val colors: FrostColors
        @Composable @ReadOnlyComposable get() = LocalFrostColors.current

    val typography: FrostTypography
        @Composable @ReadOnlyComposable get() = LocalFrostTypography.current

    val shapes: FrostShapes get() = FrostShapes
    val spacing: FrostSpacing get() = FrostSpacing
    val motion: FrostMotion get() = FrostMotion
}
