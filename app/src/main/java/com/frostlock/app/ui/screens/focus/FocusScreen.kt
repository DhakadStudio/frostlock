package com.frostlock.app.ui.screens.focus

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.data.InstalledApp
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.focus.FocusMode
import com.frostlock.app.focus.FocusPhase
import com.frostlock.app.focus.FocusUiState
import com.frostlock.app.ui.components.AppPickerDialog
import com.frostlock.app.ui.components.controls.FrostButton
import com.frostlock.app.ui.components.controls.FrostButtonVariant
import com.frostlock.app.ui.components.controls.FrostChip
import com.frostlock.app.ui.components.controls.FrostSegmentedControl
import com.frostlock.app.ui.components.controls.FrostSwitchRow
import com.frostlock.app.ui.components.dialogs.EndSessionDialog
import com.frostlock.app.ui.components.surface.FrostCard
import com.frostlock.app.ui.components.surface.HeroCard
import com.frostlock.app.ui.components.surface.PermissionCard
import com.frostlock.app.ui.shell.FrostTabScaffold
import com.frostlock.app.ui.theme.FrostTheme
import com.frostlock.app.ui.util.AmbienceSound
import com.frostlock.app.ui.util.ambienceFromKey
import com.frostlock.app.ui.util.ambiencePillLabel
import com.frostlock.app.ui.util.formatClock
import com.frostlock.app.ui.util.formatDuration
import com.frostlock.app.ui.util.formatSessionTime
import com.frostlock.app.ui.util.pauseOptionLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DURATION_OPTIONS = listOf(10, 15, 25, 30, 45, 60, 90)
private val PAUSE_OPTIONS = listOf(0, 1, 2, 3, -1)

/**
 * Focus: configure a session in a few taps, then get out of the way.
 *
 * The running session is owned by [com.frostlock.app.focus.FocusSessionController]
 * and shown by the shell, so this screen never holds the timer itself.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    onOpenShieldApps: () -> Unit,
    viewModel: FocusViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val lastSession by viewModel.lastSavedSession.collectAsState()
    val context = LocalContext.current

    var customTagOpen by remember { mutableStateOf(false) }
    var subjectPickerOpen by remember { mutableStateOf(false) }
    var sessionSettingsOpen by remember { mutableStateOf(false) }
    var confirmEnd by remember { mutableStateOf(false) }

    val accessMissing = state.plannedPackages.isNotEmpty() && !state.canBlockApps

    FrostTabScaffold(
        title = "Focus",
        subtitle = "One task at a time.",
        actions = {
            Text(
                text = LocalDate.now().format(
                    DateTimeFormatter.ofPattern("EEE d MMM", Locale.US)
                ),
                style = FrostTheme.typography.labelMedium,
                color = FrostTheme.colors.textSecondary
            )
        }
    ) { padding ->
        if (state.hasStarted && !state.justCompleted) {
            ActiveSessionPreview(
                state = state,
                padding = padding,
                onReturn = viewModel::showImmersive,
                onPause = viewModel::pause,
                onResume = viewModel::startOrResume,
                onEnd = {
                    if (state.elapsedSeconds >= 5) {
                        confirmEnd = true
                    } else {
                        viewModel.stop()
                    }
                }
            )
        } else {
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
                verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.lg)
            ) {
                item {
                    FrostSegmentedControl(
                        options = FocusMode.entries,
                        selected = state.mode,
                        onSelect = viewModel::selectMode,
                        label = { it.label }
                    )
                }

                if (state.mode != FocusMode.STOPWATCH) {
                    item {
                        DurationRow(
                            targetMinutes = state.targetMinutes,
                            enabled = !state.isRunning,
                            onSelect = viewModel::selectMinutes
                        )
                    }
                }

                item {
                    SubjectSection(
                        state = state,
                        onSelectTag = viewModel::selectTag,
                        onCustomTag = { customTagOpen = true },
                        onOpenShieldApps = onOpenShieldApps
                    )
                }

                item {
                    OutlinedTextField(
                        value = state.intent,
                        onValueChange = viewModel::setIntent,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Intention") },
                        placeholder = {
                            Text(
                                text = "What are you focusing on?",
                                color = FrostTheme.colors.textSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null,
                                tint = FrostTheme.colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        supportingText = {
                            Text(
                                text = if (state.intent.isBlank()) {
                                    "Optional — it stays on screen so you don't drift."
                                } else {
                                    "${state.intent.length}/${FocusSettings.MAX_INTENT_LENGTH}"
                                },
                                color = FrostTheme.colors.textSecondary
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FrostTheme.colors.primary,
                            unfocusedBorderColor = FrostTheme.colors.outline,
                            focusedTextColor = FrostTheme.colors.textPrimary,
                            unfocusedTextColor = FrostTheme.colors.textPrimary,
                            cursorColor = FrostTheme.colors.primary,
                            focusedLabelColor = FrostTheme.colors.primary,
                            unfocusedLabelColor = FrostTheme.colors.textSecondary
                        )
                    )
                }

                item {
                    FocusDial(
                        progress = state.progress,
                        timeText = if (state.mode == FocusMode.STOPWATCH) {
                            "00:00"
                        } else {
                            formatClock(state.targetMinutes * 60)
                        },
                        subtitle = when {
                            state.justCompleted -> "Session complete"
                            state.breakFinished -> "Round ${state.round} ready"
                            state.mode == FocusMode.STOPWATCH -> "Open-ended"
                            else -> "Ready"
                        }
                    )
                }

                item {
                    SessionSettingsRow(
                        state = state,
                        onOpen = { sessionSettingsOpen = true }
                    )
                }

                if (state.justCompleted) {
                    item {
                        Text(
                            text = "Session saved. Your summary is ready.",
                            style = FrostTheme.typography.bodyLarge,
                            color = FrostTheme.colors.success,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (accessMissing) {
                    item {
                        PermissionCard(
                            title = "Blocking is off",
                            message = "This session would hold ${state.plannedPackages.size} " +
                                "app${if (state.plannedPackages.size == 1) "" else "s"}, but " +
                                "accessibility access is off. Start the timer anyway or turn " +
                                "access on.",
                            actionLabel = "Fix access",
                            onAction = { openAccessibilitySettings(context) }
                        )
                    }
                } else if (state.plannedPackages.isEmpty()) {
                    item {
                        Text(
                            text = "No apps selected — this session won't block anything.",
                            style = FrostTheme.typography.bodyMedium,
                            color = FrostTheme.colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    FrostButton(
                        text = when {
                            accessMissing -> "Start timer only"
                            state.breakFinished -> "Start round ${state.round}"
                            state.justCompleted -> "Start again"
                            else -> "Start focus"
                        },
                        onClick = viewModel::startOrResume,
                        icon = Icons.Default.PlayArrow,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                lastSession?.let { session ->
                    item {
                        LastSessionCard(session = session)
                    }
                }
            }
        }
    }

    if (customTagOpen) {
        TagDialog(
            initialTag = if (state.selectedSubject == null) state.tag else "",
            onDismiss = { customTagOpen = false },
            onSave = { tag ->
                viewModel.selectTag(tag)
                customTagOpen = false
            }
        )
    }

    if (subjectPickerOpen) {
        AppPickerDialog(
            apps = state.installedApps,
            selected = state.selectedSubject?.packages ?: emptySet(),
            title = "${state.tag} apps",
            onDismiss = { subjectPickerOpen = false },
            onConfirm = { packages ->
                viewModel.setSubjectPackages(state.tag, packages)
                subjectPickerOpen = false
            }
        )
    }

    if (sessionSettingsOpen) {
        SessionSettingsSheet(
            state = state,
            onDismiss = { sessionSettingsOpen = false },
            onStrictMode = viewModel::setStrictMode,
            onMaxPauses = viewModel::setMaxPauses,
            onAmbience = viewModel::setAmbience
        )
    }

    if (confirmEnd) {
        EndSessionDialog(
            elapsedSeconds = state.elapsedSeconds,
            plannedSeconds = state.totalSeconds,
            onKeepGoing = { confirmEnd = false },
            onSave = {
                confirmEnd = false
                viewModel.stop()
            },
            onDiscard = {
                confirmEnd = false
                viewModel.discardSession()
            }
        )
    }
}

@Composable
private fun DurationRow(
    targetMinutes: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    var customOpen by remember { mutableStateOf(false) }
    val customSelected = targetMinutes !in DURATION_OPTIONS

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)
    ) {
        DURATION_OPTIONS.forEach { minutes ->
            FrostChip(
                text = "${minutes}m",
                selected = targetMinutes == minutes,
                enabled = enabled,
                onClick = { onSelect(minutes) }
            )
        }
        FrostChip(
            text = if (customSelected) formatDuration(targetMinutes * 60L) else "Custom",
            selected = customSelected,
            enabled = enabled,
            onClick = { customOpen = true }
        )
    }

    if (customOpen) {
        CustomDurationDialog(
            initialMinutes = targetMinutes,
            onDismiss = { customOpen = false },
            onSave = { minutes ->
                onSelect(minutes)
                customOpen = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectSection(
    state: FocusUiState,
    onSelectTag: (String) -> Unit,
    onCustomTag: () -> Unit,
    onOpenShieldApps: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Subject",
                style = FrostTheme.typography.labelLarge,
                color = FrostTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.weight(1f))
            if (state.tag.isNotBlank() && !state.isRunning) {
                TextButton(onClick = { onSelectTag("") }) {
                    Text(
                        text = "Clear",
                        style = FrostTheme.typography.labelLarge,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)
        ) {
            state.subjects.forEach { subject ->
                val selected = state.tag.equals(subject.name, ignoreCase = true)
                FrostChip(
                    text = if (subject.hasApps) {
                        "${subject.name} · ${subject.packages.size}"
                    } else {
                        subject.name
                    },
                    selected = selected,
                    dotColor = Color(subject.colorArgb),
                    onClick = {
                        onSelectTag(if (selected) "" else subject.name)
                    }
                )
            }
            FrostChip(
                text = if (state.tag.isNotBlank() && state.selectedSubject == null) {
                    state.tag
                } else {
                    "+ New"
                },
                selected = state.tag.isNotBlank() && state.selectedSubject == null,
                onClick = onCustomTag
            )
        }

        val count = state.plannedPackages.size
        FrostCard(
            onClick = onOpenShieldApps,
            contentPadding = PaddingValues(
                horizontal = FrostTheme.spacing.lg,
                vertical = FrostTheme.spacing.md
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (count > 0) {
                        FrostTheme.colors.success
                    } else {
                        FrostTheme.colors.textSecondary
                    }
                )
                Spacer(modifier = Modifier.width(FrostTheme.spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (count > 0) {
                            "Blocks $count app${if (count == 1) "" else "s"}"
                        } else {
                            "No apps selected"
                        },
                        style = FrostTheme.typography.titleMedium,
                        color = FrostTheme.colors.textPrimary
                    )
                    Text(
                        text = if (count > 0) {
                            if (state.selectedSubject?.hasApps == true) {
                                "${state.tag} app set"
                            } else {
                                "Focus-session app list"
                            }
                        } else {
                            "This session won't block anything."
                        },
                        style = FrostTheme.typography.bodySmall,
                        color = FrostTheme.colors.textSecondary
                    )
                }
                Text(
                    text = "Edit",
                    style = FrostTheme.typography.labelLarge,
                    color = FrostTheme.colors.primary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = FrostTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun SessionSettingsRow(
    state: FocusUiState,
    onOpen: () -> Unit
) {
    val chips = buildList {
        add(if (state.strictMode) "Strict" else "Flexible")
        add(pauseOptionLabel(state.maxPauses))
        ambiencePillLabel(state.ambienceKey)?.let { add(it) }
        if (state.mode != FocusMode.STOPWATCH) add("${state.targetMinutes}m")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        chips.forEach { chip ->
            FrostChip(
                text = chip,
                selected = false,
                onClick = onOpen
            )
        }
        TextButton(onClick = onOpen) {
            Text(
                text = "Session settings",
                style = FrostTheme.typography.labelLarge,
                color = FrostTheme.colors.primary
            )
        }
    }
}

@Composable
private fun ActiveSessionPreview(
    state: FocusUiState,
    padding: PaddingValues,
    onReturn: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(
                start = FrostTheme.spacing.screenHorizontal,
                end = FrostTheme.spacing.screenHorizontal,
                top = FrostTheme.spacing.sm,
                bottom = FrostTheme.spacing.huge
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.lg)
    ) {
        HeroCard(contentPadding = PaddingValues(FrostTheme.spacing.xxl)) {
            Text(
                text = when {
                    state.phase == FocusPhase.BREAK -> "BREAK"
                    state.isRunning -> "FOCUSING"
                    else -> "PAUSED"
                },
                style = FrostTheme.typography.labelMedium,
                color = FrostTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
            Text(
                text = if (state.mode == FocusMode.STOPWATCH) {
                    formatClock(state.elapsedSeconds)
                } else {
                    formatClock(state.remainingSeconds)
                },
                style = FrostTheme.typography.displayTimer,
                color = FrostTheme.colors.textPrimary
            )
            Text(
                text = state.tag.trim().ifBlank { "No subject" },
                style = FrostTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary
            )
        }

        Text(
            text = if (state.phase == FocusPhase.BREAK) {
                "Phones are allowed until the break ends."
            } else {
                "The session keeps running while you look around."
            },
            style = FrostTheme.typography.bodyMedium,
            color = FrostTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )

        FrostButton(
            text = "Return to session",
            onClick = onReturn,
            modifier = Modifier.fillMaxWidth()
        )

        if (state.phase != FocusPhase.BREAK && !state.strictActive) {
            FrostButton(
                text = if (state.isRunning) "Pause" else "Resume",
                onClick = if (state.isRunning) onPause else onResume,
                enabled = !state.isRunning || state.pauseAllowance.canPause,
                variant = FrostButtonVariant.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
        }

        FrostButton(
            text = "End session",
            onClick = onEnd,
            variant = FrostButtonVariant.GHOST,
            icon = Icons.Default.Stop,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LastSessionCard(session: SessionEntity) {
    FrostCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(FrostTheme.colors.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = FrostTheme.colors.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(FrostTheme.spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Last session",
                    style = FrostTheme.typography.labelMedium,
                    color = FrostTheme.colors.textSecondary
                )
                Text(
                    text = "${session.tag.trim().ifBlank { "Untagged" }} · " +
                        formatDuration(session.durationSeconds.toLong()),
                    style = FrostTheme.typography.titleMedium,
                    color = FrostTheme.colors.textPrimary
                )
            }
            Text(
                text = formatSessionTime(session.endedAt),
                style = FrostTheme.typography.bodySmall,
                color = FrostTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun FocusDial(
    progress: Float,
    timeText: String,
    subtitle: String
) {
    val colors = FrostTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FrostTheme.spacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(248.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(236.dp)) {
                val strokeWidth = 12.dp.toPx()
                val arcSize = Size(size.width, size.height)
                drawArc(
                    color = colors.surfaceContainerHigh,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = arcSize
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            colors.primary,
                            colors.secondary,
                            colors.primary
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = progress.coerceIn(0f, 1f) * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = arcSize
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeText,
                    style = FrostTheme.typography.displayTimer,
                    color = FrostTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
                Text(
                    text = subtitle,
                    style = FrostTheme.typography.bodyMedium,
                    color = FrostTheme.colors.secondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionSettingsSheet(
    state: FocusUiState,
    onDismiss: () -> Unit,
    onStrictMode: (Boolean) -> Unit,
    onMaxPauses: (Int) -> Unit,
    onAmbience: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = FrostTheme.colors.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = FrostTheme.spacing.screenHorizontal,
                    end = FrostTheme.spacing.screenHorizontal,
                    bottom = FrostTheme.spacing.huge
                ),
            verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.md)
        ) {
            Text(
                text = "Session settings",
                style = FrostTheme.typography.titleLarge,
                color = FrostTheme.colors.textPrimary
            )
            FrostSwitchRow(
                title = "Strict mode",
                checked = state.strictMode,
                onCheckedChange = onStrictMode,
                description = "No pausing or early exit until the timer ends."
            )

            Text(
                text = "Pauses per session",
                style = FrostTheme.typography.labelLarge,
                color = FrostTheme.colors.textSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)) {
                PAUSE_OPTIONS.forEach { option ->
                    FrostChip(
                        text = pauseOptionLabel(option),
                        selected = state.maxPauses == option,
                        onClick = { onMaxPauses(option) }
                    )
                }
            }

            Text(
                text = "Sound",
                style = FrostTheme.typography.labelLarge,
                color = FrostTheme.colors.textSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)
            ) {
                AmbienceSound.entries.forEach { sound ->
                    FrostChip(
                        text = sound.label,
                        selected = ambienceFromKey(state.ambienceKey) == sound,
                        onClick = { onAmbience(sound.key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomDurationDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var value by remember { mutableStateOf(initialMinutes.toString()) }
    val minutes = value.toIntOrNull()
    val valid = minutes != null &&
        minutes in FocusSettings.MIN_DURATION_MINUTES..FocusSettings.MAX_DURATION_MINUTES

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FrostTheme.colors.surfaceContainer,
        title = {
            Text(
                text = "Custom length",
                style = FrostTheme.typography.headlineMedium,
                color = FrostTheme.colors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)) {
                Text(
                    text = "1 to 480 minutes.",
                    style = FrostTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { input ->
                        if (input.length <= 3 && input.all { it.isDigit() }) value = input
                    },
                    singleLine = true,
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FrostTheme.colors.primary,
                        unfocusedBorderColor = FrostTheme.colors.outline,
                        focusedTextColor = FrostTheme.colors.textPrimary,
                        unfocusedTextColor = FrostTheme.colors.textPrimary,
                        cursorColor = FrostTheme.colors.primary,
                        focusedLabelColor = FrostTheme.colors.primary,
                        unfocusedLabelColor = FrostTheme.colors.textSecondary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { minutes?.let(onSave) },
                enabled = valid
            ) {
                Text(
                    text = "Set",
                    style = FrostTheme.typography.labelLarge,
                    color = FrostTheme.colors.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = FrostTheme.typography.labelLarge,
                    color = FrostTheme.colors.textSecondary
                )
            }
        }
    )
}

@Composable
private fun TagDialog(
    initialTag: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialTag) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FrostTheme.colors.surfaceContainer,
        title = {
            Text(
                text = "New subject",
                style = FrostTheme.typography.headlineMedium,
                color = FrostTheme.colors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)) {
                Text(
                    text = "Name this session so it shows up in your subject breakdown.",
                    style = FrostTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        if (input.length <= FocusSettings.MAX_TAG_LENGTH) text = input
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Subject") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FrostTheme.colors.primary,
                        unfocusedBorderColor = FrostTheme.colors.outline,
                        focusedTextColor = FrostTheme.colors.textPrimary,
                        unfocusedTextColor = FrostTheme.colors.textPrimary,
                        cursorColor = FrostTheme.colors.primary,
                        focusedLabelColor = FrostTheme.colors.primary,
                        unfocusedLabelColor = FrostTheme.colors.textSecondary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = text.isNotBlank()
            ) {
                Text(
                    text = "Save",
                    style = FrostTheme.typography.labelLarge,
                    color = FrostTheme.colors.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = FrostTheme.typography.labelLarge,
                    color = FrostTheme.colors.textSecondary
                )
            }
        }
    )
}

private fun openAccessibilitySettings(context: android.content.Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
