package com.frostlock.app.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.components.controls.FrostButton
import com.frostlock.app.ui.components.controls.FrostButtonVariant
import com.frostlock.app.ui.components.controls.FrostSearchField
import com.frostlock.app.ui.components.controls.FrostSegmentedControl
import com.frostlock.app.ui.components.controls.FrostSwitchRow
import com.frostlock.app.ui.components.surface.FrostCard
import com.frostlock.app.ui.components.surface.HeroCard
import com.frostlock.app.ui.components.surface.PermissionCard
import com.frostlock.app.ui.components.surface.StatusRow
import com.frostlock.app.ui.shell.FrostScreen
import com.frostlock.app.ui.theme.FrostLockTheme
import com.frostlock.app.ui.theme.FrostTheme
import com.frostlock.app.ui.theme.IcePalettes

/**
 * Visual smoke test for the Phase 1 tokens and components.
 *
 * Eight previews cover all four palettes at compact width plus Frost and
 * Midnight at expanded width, so a token change is visible before any screen is
 * migrated.
 */
@Composable
private fun ComponentShowcase() {
    FrostScreen(scrollable = true) {
        Spacer(modifier = Modifier.height(FrostTheme.spacing.xl))
        Text(
            text = "Design tokens",
            style = FrostTheme.typography.headlineLarge,
            color = FrostTheme.colors.textPrimary
        )
        Text(
            text = "Phase 1 components",
            style = FrostTheme.typography.bodyMedium,
            color = FrostTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.height(FrostTheme.spacing.xxl))

        HeroCard {
            Text(
                text = "TODAY",
                style = FrostTheme.typography.labelMedium,
                color = FrostTheme.colors.textSecondary
            )
            Text(
                text = "1h 20m",
                style = FrostTheme.typography.displayLargeEmphasized,
                color = FrostTheme.colors.textPrimary
            )
            Text(
                text = "of 2h target · 3 sessions",
                style = FrostTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(FrostTheme.spacing.xxl))
        FrostSegmentedControl(
            options = listOf("7 days", "30 days", "6 months"),
            selected = "7 days",
            onSelect = {},
            label = { it }
        )

        Spacer(modifier = Modifier.height(FrostTheme.spacing.xxl))
        FrostCard {
            StatusRow(
                title = "Apps",
                status = "On · 4 apps held",
                icon = Icons.Filled.Shield,
                onClick = {}
            )
            StatusRow(
                title = "Limits",
                status = "2 limits",
                icon = Icons.Filled.HourglassTop,
                onClick = {}
            )
        }

        Spacer(modifier = Modifier.height(FrostTheme.spacing.xxl))
        PermissionCard(
            title = "Access needed",
            message = "FrostLock needs accessibility access to hold apps during focus.",
            actionLabel = "Open accessibility settings",
            onAction = {}
        )

        Spacer(modifier = Modifier.height(FrostTheme.spacing.xxl))
        FrostSwitchRow(
            title = "Strict mode",
            description = "No pause, no minimise",
            checked = true,
            onCheckedChange = {}
        )
        FrostSearchField(value = "", onValueChange = {})

        Spacer(modifier = Modifier.height(FrostTheme.spacing.xxl))
        FrostButton(text = "Start focus", onClick = {})
        Spacer(modifier = Modifier.height(FrostTheme.spacing.md))
        FrostButton(
            text = "Choose apps",
            onClick = {},
            variant = FrostButtonVariant.SECONDARY
        )
        Spacer(modifier = Modifier.height(FrostTheme.spacing.md))
        FrostButton(
            text = "Discard session",
            onClick = {},
            variant = FrostButtonVariant.DANGER
        )
        Spacer(modifier = Modifier.height(FrostTheme.spacing.huge))
    }
}

@Preview(name = "Frost · compact", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun ShowcaseFrostCompact() {
    FrostLockTheme(IcePalettes.Frost) { ComponentShowcase() }
}

@Preview(name = "Aurora · compact", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun ShowcaseAuroraCompact() {
    FrostLockTheme(IcePalettes.Aurora) { ComponentShowcase() }
}

@Preview(name = "Midnight · compact", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun ShowcaseMidnightCompact() {
    FrostLockTheme(IcePalettes.Midnight) { ComponentShowcase() }
}

@Preview(name = "Crystal · compact", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun ShowcaseCrystalCompact() {
    FrostLockTheme(IcePalettes.Crystal) { ComponentShowcase() }
}

@Preview(name = "Frost · expanded", widthDp = 840, heightDp = 1000, showBackground = true)
@Composable
private fun ShowcaseFrostExpanded() {
    FrostLockTheme(IcePalettes.Frost) { ComponentShowcase() }
}

@Preview(name = "Aurora · expanded", widthDp = 840, heightDp = 1000, showBackground = true)
@Composable
private fun ShowcaseAuroraExpanded() {
    FrostLockTheme(IcePalettes.Aurora) { ComponentShowcase() }
}

@Preview(name = "Midnight · expanded", widthDp = 840, heightDp = 1000, showBackground = true)
@Composable
private fun ShowcaseMidnightExpanded() {
    FrostLockTheme(IcePalettes.Midnight) { ComponentShowcase() }
}

@Preview(name = "Crystal · expanded", widthDp = 840, heightDp = 1000, showBackground = true)
@Composable
private fun ShowcaseCrystalExpanded() {
    FrostLockTheme(IcePalettes.Crystal) { ComponentShowcase() }
}
