package com.frostlock.app.ui.screens.shield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.data.InstalledApp
import com.frostlock.app.ui.components.controls.FrostButton
import com.frostlock.app.ui.components.controls.FrostButtonVariant
import com.frostlock.app.ui.components.controls.FrostSearchField
import com.frostlock.app.ui.components.surface.FrostCard
import com.frostlock.app.ui.shell.FrostDetailScaffold
import com.frostlock.app.ui.theme.FrostShapes
import com.frostlock.app.ui.theme.FrostTheme
import com.frostlock.app.ui.util.SUGGESTED_LIMIT_MINUTES
import com.frostlock.app.ui.util.suggestedLimitApps

/**
 * Shield → Limits.
 *
 * Daily budgets are independent of focus blocking and reset at midnight. The
 * screen only ever shows one list: every app, with its budget and today's use.
 */
@Composable
fun LimitsScreen(
    onBack: () -> Unit,
    viewModel: LimitsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var limitTarget by remember { mutableStateOf<InstalledApp?>(null) }

    val visibleApps = remember(state.apps, query) {
        state.apps.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
    }
    val suggestedApps = remember(state.apps, state.limits) {
        suggestedLimitApps(state.apps, state.limits)
    }

    FrostDetailScaffold(title = "Limits", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = FrostTheme.spacing.xl,
                vertical = FrostTheme.spacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.md)
        ) {
            item {
                FrostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = FrostTheme.colors.secondary
                        )
                        Spacer(modifier = Modifier.width(FrostTheme.spacing.md))
                        Column {
                            Text(
                                text = "Daily limits are independent",
                                style = FrostTheme.typography.titleMedium,
                                color = FrostTheme.colors.textPrimary
                            )
                            Text(
                                text = "They apply on their own, even when focus blocking is off. Budgets reset at midnight.",
                                style = FrostTheme.typography.bodyMedium,
                                color = FrostTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }

            if (suggestedApps.isNotEmpty()) {
                item {
                    Text(
                        text = "Suggested limits",
                        style = FrostTheme.typography.titleLarge,
                        color = FrostTheme.colors.textPrimary
                    )
                }
                item {
                    FrostCard {
                        Text(
                            text = "The usual time sinks. One tap gives each a " +
                                "$SUGGESTED_LIMIT_MINUTES-minute daily budget.",
                            style = FrostTheme.typography.bodyMedium,
                            color = FrostTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(FrostTheme.spacing.md))
                        suggestedApps.forEach { app ->
                            SuggestedLimitRow(
                                app = app,
                                onAdd = {
                                    viewModel.setLimit(app.packageName, SUGGESTED_LIMIT_MINUTES)
                                }
                            )
                        }
                    }
                }
            }

            item {
                FrostSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search apps"
                )
            }

            item {
                Text(
                    text = if (state.limitedCount > 0) {
                        "Daily limits · ${state.limitedCount} set"
                    } else {
                        "Daily limits"
                    },
                    style = FrostTheme.typography.titleLarge,
                    color = FrostTheme.colors.textPrimary
                )
            }

            when {
                state.loading -> item { LoadingRow() }
                visibleApps.isEmpty() -> item {
                    FrostCard {
                        Text(
                            text = if (query.isBlank()) {
                                "No apps found on this device."
                            } else {
                                "No apps match that search."
                            },
                            style = FrostTheme.typography.bodyMedium,
                            color = FrostTheme.colors.textSecondary
                        )
                    }
                }
                else -> items(visibleApps, key = { it.packageName }) { app ->
                    LimitRow(
                        app = app,
                        limitMinutes = state.limits[app.packageName] ?: 0,
                        usedMinutes = state.usedMinutes(app.packageName),
                        onSetLimit = { limitTarget = app }
                    )
                }
            }
        }
    }

    limitTarget?.let { target ->
        LimitEditorDialog(
            appLabel = target.label,
            current = state.limits[target.packageName] ?: 0,
            usedMinutes = state.usedMinutes(target.packageName),
            onSave = { minutes ->
                viewModel.setLimit(target.packageName, minutes)
                limitTarget = null
            },
            onDismiss = { limitTarget = null }
        )
    }
}

@Composable
private fun LimitRow(
    app: InstalledApp,
    limitMinutes: Int,
    usedMinutes: Int,
    onSetLimit: () -> Unit
) {
    val over = limitMinutes > 0 && usedMinutes >= limitMinutes
    FrostCard(onClick = onSetLimit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShieldAppIcon(app)
            Spacer(modifier = Modifier.width(FrostTheme.spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = FrostTheme.typography.titleMedium,
                    color = FrostTheme.colors.textPrimary,
                    maxLines = 1
                )
                if (limitMinutes > 0) {
                    Text(
                        text = "$usedMinutes / $limitMinutes min today",
                        style = FrostTheme.typography.bodyMedium,
                        color = if (over) FrostTheme.colors.danger else FrostTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
                    LinearProgressIndicator(
                        progress = {
                            (usedMinutes.toFloat() / limitMinutes.toFloat()).coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = if (over) FrostTheme.colors.danger else FrostTheme.colors.primary,
                        trackColor = FrostTheme.colors.surfaceContainerHigh
                    )
                } else {
                    Text(
                        text = "No limit",
                        style = FrostTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.width(FrostTheme.spacing.sm))
            Text(
                text = if (limitMinutes > 0) "Edit" else "Set limit",
                style = FrostTheme.typography.labelLarge,
                color = FrostTheme.colors.secondary
            )
        }
    }
}

@Composable
private fun SuggestedLimitRow(
    app: InstalledApp,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FrostTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShieldAppIcon(app)
        Spacer(modifier = Modifier.width(FrostTheme.spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = FrostTheme.typography.titleMedium,
                color = FrostTheme.colors.textPrimary,
                maxLines = 1
            )
            Text(
                text = "${SUGGESTED_LIMIT_MINUTES}m daily budget",
                style = FrostTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(FrostTheme.spacing.sm))
        Surface(
            onClick = onAdd,
            shape = FrostShapes.pill,
            color = FrostTheme.colors.primary,
            contentColor = FrostTheme.colors.onPrimary
        ) {
            Text(
                text = "Add ${SUGGESTED_LIMIT_MINUTES}m",
                style = FrostTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LimitEditorDialog(
    appLabel: String,
    current: Int,
    usedMinutes: Int,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var minutes by remember {
        mutableIntStateOf(if (current > 0) current else DEFAULT_LIMIT_MINUTES)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(FrostShapes.extraLarge)
                .background(FrostTheme.colors.surfaceContainer)
                .border(1.dp, FrostTheme.colors.outline, FrostShapes.extraLarge)
                .padding(FrostTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(FrostTheme.colors.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = FrostTheme.colors.secondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(FrostTheme.spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily limit",
                        style = FrostTheme.typography.titleLarge,
                        color = FrostTheme.colors.textPrimary
                    )
                    Text(
                        text = appLabel,
                        style = FrostTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }

            Surface(
                shape = FrostShapes.pill,
                color = FrostTheme.colors.surfaceContainerHigh,
                contentColor = FrostTheme.colors.secondary
            ) {
                Text(
                    text = "$usedMinutes min used today · resets at midnight",
                    style = FrostTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepperButton(text = "−", onClick = {
                    minutes = (minutes - 5).coerceAtLeast(MIN_LIMIT_MINUTES)
                })
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$minutes",
                        style = FrostTheme.typography.displayLarge,
                        color = FrostTheme.colors.textPrimary
                    )
                    Text(
                        text = "minutes / day",
                        style = FrostTheme.typography.bodySmall,
                        color = FrostTheme.colors.textSecondary
                    )
                }
                StepperButton(text = "+", onClick = {
                    minutes = (minutes + 5).coerceAtMost(MAX_LIMIT_MINUTES)
                })
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)
            ) {
                LIMIT_PRESETS.forEach { preset ->
                    FilterChip(
                        selected = minutes == preset,
                        onClick = { minutes = preset },
                        label = { Text("${preset}m") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = FrostTheme.colors.surfaceContainerHigh,
                            labelColor = FrostTheme.colors.textSecondary,
                            selectedContainerColor = FrostTheme.colors.primary,
                            selectedLabelColor = FrostTheme.colors.onPrimary
                        )
                    )
                }
            }

            if (current > 0) {
                FrostButton(
                    text = "Remove limit",
                    onClick = { onSave(0) },
                    variant = FrostButtonVariant.DANGER
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FrostTheme.spacing.md)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Cancel",
                        color = FrostTheme.colors.textSecondary
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    FrostButton(
                        text = "Save limit",
                        onClick = { onSave(minutes) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepperButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = FrostTheme.colors.surfaceContainerHigh,
        contentColor = FrostTheme.colors.textPrimary,
        border = androidx.compose.foundation.BorderStroke(1.dp, FrostTheme.colors.outline)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = FrostTheme.typography.titleLarge,
                color = FrostTheme.colors.textPrimary
            )
        }
    }
}

private val LIMIT_PRESETS = listOf(15, 30, 60, 120)
private const val MIN_LIMIT_MINUTES = 5
private const val DEFAULT_LIMIT_MINUTES = 30
private const val MAX_LIMIT_MINUTES = 480
