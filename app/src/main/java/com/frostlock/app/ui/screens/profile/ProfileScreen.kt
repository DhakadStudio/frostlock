package com.frostlock.app.ui.screens.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.ui.components.controls.FrostSwitchRow
import com.frostlock.app.ui.components.surface.FrostCard
import com.frostlock.app.ui.components.surface.StatusRow
import com.frostlock.app.ui.shell.FrostTabScaffold
import com.frostlock.app.ui.theme.FrostShapes
import com.frostlock.app.ui.theme.FrostTheme
import com.frostlock.app.ui.theme.IcePalettes
import com.frostlock.app.ui.util.AmbienceSound
import com.frostlock.app.ui.util.ambienceFromKey
import com.frostlock.app.ui.util.pauseOptionLabel

private val GOAL_OPTIONS = listOf(30, 60, 90, 120, 180, 240)
private val PAUSE_OPTIONS = listOf(0, 1, 2, 3, -1)
private val BREAK_OPTIONS = listOf(3, 5, 10)

private const val DIALOG_GOAL = "goal"
private const val DIALOG_PAUSES = "pauses"
private const val DIALOG_BREAK = "break"
private const val DIALOG_SOUND = "sound"

/**
 * You: preferences, planning and appearance only.
 *
 * Blocking lives in Shield, insights live in Insights. Nothing here duplicates
 * another tab, and every permission row states the real access state instead of
 * claiming a feature is on when it cannot run.
 */
@Composable
fun ProfileScreen(
    onOpenGoals: () -> Unit = {},
    onOpenDeadline: () -> Unit = {},
    onOpenWidgets: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var dialog by remember { mutableStateOf<String?>(null) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshAccess()
    }

    FrostTabScaffold(
        title = "You",
        subtitle = "Your settings, your data."
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = FrostTheme.spacing.screenHorizontal,
                end = FrostTheme.spacing.screenHorizontal,
                top = FrostTheme.spacing.sm,
                bottom = FrostTheme.spacing.huge
            ),
            verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.md)
        ) {
            item { SectionLabel("Focus") }
            item {
                FrostCard(
                    contentPadding = PaddingValues(
                        horizontal = FrostTheme.spacing.lg,
                        vertical = FrostTheme.spacing.xs
                    )
                ) {
                    StatusRow(
                        title = "Daily target",
                        status = YouStatus.dailyTarget(state.dailyGoalMinutes),
                        icon = Icons.Default.Flag,
                        onClick = { dialog = DIALOG_GOAL }
                    )
                    Divider()
                    FrostSwitchRow(
                        title = "Strict mode",
                        checked = state.strictMode,
                        onCheckedChange = viewModel::setStrictMode,
                        description = "Sessions can't be paused or stopped until the timer ends."
                    )
                    Divider()
                    StatusRow(
                        title = "Pauses",
                        status = pauseOptionLabel(state.maxPauses),
                        icon = Icons.Default.Pause,
                        onClick = { dialog = DIALOG_PAUSES }
                    )
                    Divider()
                    FrostSwitchRow(
                        title = "Pomodoro breaks",
                        checked = state.breaksEnabled,
                        onCheckedChange = viewModel::setBreaksEnabled,
                        description = "Blocking lifts between rounds and a break timer starts."
                    )
                    if (state.breaksEnabled) {
                        Divider()
                        StatusRow(
                            title = "Break length",
                            status = "${state.breakMinutes}m",
                            icon = Icons.Default.Timer,
                            onClick = { dialog = DIALOG_BREAK }
                        )
                    }
                    Divider()
                    FrostSwitchRow(
                        title = "Reminders",
                        checked = state.remindersEnabled,
                        onCheckedChange = viewModel::setRemindersEnabled,
                        description = "A gentle nudge every 10 minutes during a session."
                    )
                    Divider()
                    FrostSwitchRow(
                        title = "Floating timer",
                        checked = state.bubbleEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !state.overlayGranted) {
                                openOverlaySettings(context)
                            }
                            viewModel.setBubbleEnabled(enabled)
                        },
                        description = if (state.overlayGranted) {
                            "A small draggable timer stays visible over other apps."
                        } else {
                            "Needs “display over other apps” access."
                        }
                    )
                    if (state.bubbleEnabled && !state.overlayGranted) {
                        TextButton(
                            onClick = { openOverlaySettings(context) },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Allow display over other apps",
                                style = FrostTheme.typography.labelLarge,
                                color = FrostTheme.colors.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
                    }
                    Divider()
                    StatusRow(
                        title = "Sound",
                        status = ambienceFromKey(state.ambienceKey).label,
                        icon = Icons.Default.MusicNote,
                        onClick = { dialog = DIALOG_SOUND }
                    )
                    Divider()
                    StatusRow(
                        title = "Session alerts",
                        status = YouStatus.sessionAlerts(state.notificationsAccess),
                        statusColor = if (state.notificationsGranted) {
                            FrostTheme.colors.secondary
                        } else {
                            FrostTheme.colors.warning
                        },
                        icon = Icons.Default.NotificationsActive,
                        onClick = {
                            if (state.notificationsGranted) {
                                openNotificationSettings(context)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermission.launch(
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                )
                            } else {
                                openNotificationSettings(context)
                            }
                        }
                    )
                }
            }

            item { SectionLabel("Planning") }
            item {
                FrostCard(
                    contentPadding = PaddingValues(
                        horizontal = FrostTheme.spacing.lg,
                        vertical = FrostTheme.spacing.xs
                    )
                ) {
                    StatusRow(
                        title = "Goals",
                        status = YouStatus.goals(state.goalCount),
                        icon = Icons.Default.Flag,
                        onClick = onOpenGoals
                    )
                    Divider()
                    StatusRow(
                        title = "Countdown",
                        status = YouStatus.deadline(state.deadline),
                        icon = Icons.Default.Event,
                        onClick = onOpenDeadline
                    )
                }
            }

            item { SectionLabel("Appearance") }
            item {
                FrostCard {
                    Text(
                        text = "Theme",
                        style = FrostTheme.typography.titleMedium,
                        color = FrostTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(FrostTheme.spacing.md))
                    Row(horizontalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)) {
                        IcePalettes.all.forEach { palette ->
                            val selected = state.themeKey == palette.key
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(FrostShapes.medium)
                                    .then(
                                        if (selected) {
                                            Modifier.background(FrostTheme.colors.surfaceContainerHigh)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { viewModel.setTheme(palette.key) }
                                    .padding(vertical = FrostTheme.spacing.sm),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.xs)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(palette.gradient))
                                        .border(
                                            1.dp,
                                            palette.glow.copy(alpha = 0.5f),
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = palette.name,
                                    style = FrostTheme.typography.labelMedium,
                                    color = if (selected) {
                                        FrostTheme.colors.textPrimary
                                    } else {
                                        FrostTheme.colors.textSecondary
                                    },
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            item {
                FrostCard(
                    contentPadding = PaddingValues(
                        horizontal = FrostTheme.spacing.lg,
                        vertical = FrostTheme.spacing.xs
                    )
                ) {
                    StatusRow(
                        title = "Widgets",
                        status = "Six widgets, pinned from here",
                        icon = Icons.Default.Widgets,
                        onClick = onOpenWidgets
                    )
                }
            }

            item { SectionLabel("Privacy") }
            item {
                FrostCard(
                    contentPadding = PaddingValues(
                        horizontal = FrostTheme.spacing.lg,
                        vertical = FrostTheme.spacing.xs
                    )
                ) {
                    StatusRow(
                        title = "Privacy and permissions",
                        status = "Works offline · stays on this device",
                        icon = Icons.Default.Lock,
                        onClick = onOpenPrivacy
                    )
                }
            }

            item { SectionLabel("About") }
            item {
                FrostCard(
                    contentPadding = PaddingValues(
                        horizontal = FrostTheme.spacing.lg,
                        vertical = FrostTheme.spacing.xs
                    )
                ) {
                    StatusRow(
                        title = "About FrostLock",
                        status = "Free forever · no ads",
                        icon = Icons.Default.Info,
                        onClick = onOpenAbout
                    )
                }
            }
        }
    }

    when (dialog) {
        DIALOG_GOAL -> ChoiceDialog(
            title = "Daily target",
            options = GOAL_OPTIONS,
            selected = state.dailyGoalMinutes,
            label = YouStatus::dailyTarget,
            onSelect = {
                viewModel.setDailyGoalMinutes(it)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        DIALOG_PAUSES -> ChoiceDialog(
            title = "Pauses per session",
            options = PAUSE_OPTIONS,
            selected = state.maxPauses,
            label = ::pauseOptionLabel,
            onSelect = {
                viewModel.setMaxPauses(it)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        DIALOG_BREAK -> ChoiceDialog(
            title = "Break length",
            options = BREAK_OPTIONS,
            selected = state.breakMinutes,
            label = { "${it}m" },
            onSelect = {
                viewModel.setBreakMinutes(it)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        DIALOG_SOUND -> ChoiceDialog(
            title = "Sound",
            options = AmbienceSound.entries.toList(),
            selected = ambienceFromKey(state.ambienceKey),
            label = { it.label },
            onSelect = {
                viewModel.setAmbience(it.key)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = FrostTheme.typography.titleMedium,
        color = FrostTheme.colors.secondary,
        modifier = Modifier
            .padding(
                start = FrostTheme.spacing.xs,
                top = FrostTheme.spacing.sm
            )
            .semantics { heading() }
    )
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(FrostTheme.colors.outline.copy(alpha = 0.55f))
    )
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FrostTheme.colors.surfaceContainer,
        titleContentColor = FrostTheme.colors.textPrimary,
        textContentColor = FrostTheme.colors.textSecondary,
        title = {
            Text(
                text = title,
                style = FrostTheme.typography.titleLarge,
                color = FrostTheme.colors.textPrimary
            )
        },
        text = {
            Column {
                options.forEach { option ->
                    val isSelected = option == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(FrostShapes.medium)
                            .clickable { onSelect(option) }
                            .padding(
                                horizontal = FrostTheme.spacing.sm,
                                vertical = FrostTheme.spacing.md
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label(option),
                            style = FrostTheme.typography.bodyLarge,
                            color = if (isSelected) {
                                FrostTheme.colors.primary
                            } else {
                                FrostTheme.colors.textPrimary
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = FrostTheme.colors.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Done",
                    style = FrostTheme.typography.labelLarge,
                    color = FrostTheme.colors.secondary
                )
            }
        }
    )
}

private fun openOverlaySettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        )
    }
}

private fun openNotificationSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        )
    }
}
