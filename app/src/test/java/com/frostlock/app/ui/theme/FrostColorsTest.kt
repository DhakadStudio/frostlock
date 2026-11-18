package com.frostlock.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FrostColorsTest {

    @Test
    fun `every palette maps its core roles`() {
        IcePalettes.all.forEach { palette ->
            val colors = FrostColors.from(palette)
            assertEquals(palette.primary, colors.primary)
            assertEquals(palette.accent, colors.secondary)
            assertEquals(palette.glow, colors.tertiary)
            assertEquals(palette.strokeSoft, colors.outline)
            assertEquals(palette.textPrimary, colors.textPrimary)
            assertEquals(palette.textSecondary, colors.textSecondary)
            assertEquals(palette.ink, colors.onPrimary)
        }
    }

    @Test
    fun `surface roles step from lowest to highest`() {
        IcePalettes.all.forEach { palette ->
            val colors = FrostColors.from(palette)
            assertEquals(palette.background, colors.surfaceContainerLowest)
            assertEquals(palette.backgroundElevated, colors.surfaceContainerLow)
            assertEquals(palette.surfaceCard, colors.surfaceContainer)
            assertEquals(palette.surfaceHigh, colors.surfaceContainerHigh)
            assertNotEquals(colors.surfaceContainerLowest, colors.surfaceContainerHighest)
        }
    }

    @Test
    fun `feedback roles are shared across palettes`() {
        IcePalettes.all.forEach { palette ->
            val colors = FrostColors.from(palette)
            assertEquals(SuccessGreen, colors.success)
            assertEquals(WarnAmber, colors.warning)
            assertEquals(DangerRed, colors.danger)
        }
    }

    @Test
    fun `action gradient runs from primary to glacier`() {
        IcePalettes.all.forEach { palette ->
            val colors = FrostColors.from(palette)
            assertEquals(listOf(palette.primary, palette.glacier), colors.actionGradient)
        }
    }
}
