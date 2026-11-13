package com.frostlock.app.ui.screens.focus

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.data.InstalledApp
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.AppPickerDialog
import com.frostlock.app.ui.components.SessionReflectionDialog
import com.frostlock.app.ui.theme.AccentFrost
import com.frostlock.app.ui.theme.BackgroundDark
import com.frostlock.app.ui.theme.GradientFrost
import com.frostlock.app.ui.theme.GlacierBlue
import com.frostlock.app.ui.theme.PrimaryIce
import com.frostlock.app.ui.theme.StrokeSoft
import com.frostlock.app.ui.theme.SuccessGreen
import com.frostlock.app.ui.theme.SurfaceCard
import com.frostlock.app.ui.theme.SurfaceHigh
import com.frostlock.app.ui.theme.TextPrimary
import com.frostlock.app.ui.theme.TextSecondary
import com.frostlock.app.ui.util.formatClock
import com.frostlock.app.ui.util.formatDuration

private val DURATION_OPTIONS = listOf(10, 15, 25, 30, 45, 60, 90)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusScreen(
    viewModel: FocusViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var customTagOpen by remember { mutableStateOf(false) }
    var subjectPickerOpen by remember { mutableStateOf(false) }
    var immersiveMinimized by remember { mutableStateOf(false) }
    var testApp by remember { mutableStateOf<InstalledApp?>(null) }

    // A new run opens the calm, full-screen timer again.
    LaunchedEffect(state.isRunning) {
        if (state.isRunning) immersiveMinimized = false
    }

    if (state.isRunning && !immersiveMinimized) {
        FocusImmersiveOverlay(
            state = state,
            onMinimize = { immersiveMinimized = true },
            onPause = viewModel::pause,
            onResume = viewModel::startOrResume,
            onStop = viewModel::stop,
            onEndStrict = viewModel::endStrictSession,
            onSkipBreak = viewModel::skipBreak,
            onTestBlock = { testApp = it }
        )
    }

    testApp?.let { app ->
        TestBlockDialog(
            app = app,
            onOpen = {
                context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { intent ->
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
                testApp = null
            },
            onDismiss = { testApp = null }
        )
    }

    state.lastSession?.let { session ->
        SessionReflectionDialog(
            session = session,
            streakDays = state.lastSessionStreak,
            onSave = viewModel::updateLastSessionNote,
            onDelete = viewModel::deleteLastSession,
            onDismiss = viewModel::dismissReflection
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Focus",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = "One task. No switching.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Mode segmented control
        SegmentedControl(
            options = FocusMode.entries.map { it.label to it.name },
            selectedKey = state.mode.name,
            onSelect = { name ->
                FocusMode.entries.firstOrNull { it.name == name }?.let { viewModel.selectMode(it) }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Duration chips: quick presets plus a free-form custom length.
        if (state.mode != FocusMode.STOPWATCH) {
            var customOpen by remember { mutableStateOf(false) }
            val customSelected = state.targetMinutes !in DURATION_OPTIONS

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DURATION_OPTIONS.forEach { minutes ->
                    DurationChip(
                        text = "${minutes}m",
                        selected = state.targetMinutes == minutes,
                        enabled = !state.isRunning,
                        onClick = { viewModel.selectMinutes(minutes) }
                    )
                }
                DurationChip(
                    text = if (customSelected) {
                        formatDuration(state.targetMinutes * 60L)
                    } else {
                        "Custom"
                    },
                    selected = customSelected,
                    enabled = !state.isRunning,
                    onClick = { customOpen = true }
                )
            }

            if (customOpen) {
                CustomDurationDialog(
                    initialMinutes = state.targetMinutes,
                    onDismiss = { customOpen = false },
                    onSave = { minutes ->
                        viewModel.selectMinutes(minutes)
                        customOpen = false
                    }
                )
            }
        }

        // A subject is a reusable app set as well as a stats label. Picking one
        // here decides which apps this session blocks.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Subject",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.weight(1f))
                if (state.tag.isNotBlank() && !state.isRunning) {
                    TextButton(onClick = { viewModel.selectTag("") }) {
                        Text(text = "Clear", color = TextSecondary)
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.subjects.forEach { subject ->
                    FilterChip(
                        selected = state.tag.equals(subject.name, ignoreCase = true),
                        enabled = !state.isRunning,
                        onClick = {
                            viewModel.selectTag(
                                if (state.tag.equals(subject.name, ignoreCase = true)) {
                                    ""
                                } else {
                                    subject.name
                                }
                            )
                        },
                        label = {
                            SubjectChipLabel(
                                name = subject.name,
                                count = subject.packages.size.takeIf { subject.hasApps },
                                colorArgb = subject.colorArgb
                            )
                        },
                        colors = subjectChipColors()
                    )
                }
                FilterChip(
                    selected = state.tag.isNotBlank() && state.selectedSubject == null,
                    enabled = !state.isRunning,
                    onClick = { customTagOpen = true },
                    label = {
                        Text(
                            if (state.tag.isNotBlank() && state.selectedSubject == null) {
                                state.tag
                            } else {
                                "Custom"
                            }
                        )
                    },
                    colors = subjectChipColors()
                )
            }

            if (state.tag.isNotBlank()) {
                val count = state.subjectBlockedCount
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(SurfaceCard)
                        .border(1.dp, StrokeSoft, MaterialTheme.shapes.medium)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (count > 0) {
                                "Blocks $count app${if (count == 1) "" else "s"}"
                            } else {
                                "No apps set for ${state.tag}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (count > 0) {
                                "This subject's own app list is used during the session."
                            } else {
                                "Falling back to the focus-session app list."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (count > 0) SuccessGreen else TextSecondary
                        )
                    }
                    if (!state.isRunning) {
                        TextButton(onClick = { subjectPickerOpen = true }) {
                            Text(
                                text = if (count > 0) "Edit apps" else "Set apps",
                                color = AccentFrost
                            )
                        }
                    }
                }
            }
        }

        // One concrete outcome keeps the session from drifting.
        OutlinedTextField(
            value = state.intent,
            onValueChange = viewModel::setIntent,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isRunning,
            singleLine = true,
            label = { Text("One thing for this session") },
            placeholder = { Text("e.g. Finish chapter 4", color = TextSecondary) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = AccentFrost,
                    modifier = Modifier.size(18.dp)
                )
            },
            supportingText = {
                Text(
                    text = if (state.intent.isBlank()) {
                        "Optional — it appears on the focus screen so you don't drift."
                    } else {
                        "${state.intent.length}/${FocusSettings.MAX_INTENT_LENGTH}"
                    },
                    color = TextSecondary
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryIce,
                unfocusedBorderColor = StrokeSoft,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = AccentFrost,
                focusedLabelColor = AccentFrost,
                unfocusedLabelColor = TextSecondary
            ),
            shape = MaterialTheme.shapes.medium
        )

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

        if (state.mode == FocusMode.POMODORO) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill(text = "Round ${state.round}")
                InfoPill(
                    text = if (state.breaksEnabled) {
                        "Break ${state.breakMinutes}m"
                    } else {
                        "Breaks off"
                    }
                )
            }
        }

        TimerRing(
            progress = state.progress,
            timeText = if (state.mode == FocusMode.STOPWATCH) {
                formatClock(state.elapsedSeconds)
            } else {
                formatClock(state.remainingSeconds)
            },
            subtitle = when {
                state.phase == FocusPhase.BREAK -> "Break — step away"
                state.breakFinished -> "Break over — round ${state.round} ready"
                state.justCompleted -> "Session complete"
                state.isRunning -> "Stay in the zone…"
                state.hasStarted && state.elapsedSeconds > 0 -> "Paused"
                state.mode == FocusMode.STOPWATCH -> "Elapsed"
                else -> "${state.targetMinutes} minute ${state.mode.label.lowercase()}"
            }
        )

        if (state.justCompleted) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = SuccessGreen
                )
                Text(
                    text = "Nice work! Your session has been saved.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SuccessGreen,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (state.phase == FocusPhase.BREAK) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Phones are allowed during the break. FrostLock will alert you when it ends and round ${state.round + 1} starts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                ActionCircle(
                    icon = Icons.Default.SkipNext,
                    label = "Skip break",
                    container = Color(0xFF20394F),
                    tint = TextPrimary,
                    onClick = { viewModel.skipBreak() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (state.strictActive) {
            Text(
                text = "Strict Mode is on. This session can't be paused or ended early.",
                style = MaterialTheme.typography.bodyMedium,
                color = AccentFrost,
                textAlign = TextAlign.Center
            )
            HoldToEndButton(
                onEnd = viewModel::endStrictSession,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (state.isRunning) {
                    ActionCircle(
                        icon = Icons.Default.Pause,
                        contentDescription = "Pause",
                        container = Color(0xFF20394F),
                        tint = TextPrimary,
                        onClick = { viewModel.pause() },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    ActionCircle(
                        icon = Icons.Default.PlayArrow,
                        label = when {
                            state.breakFinished -> "Start round ${state.round}"
                            state.justCompleted -> "Start again"
                            state.hasStarted && state.elapsedSeconds > 0 -> "Resume"
                            else -> "Start"
                        },
                        brush = Brush.linearGradient(GradientFrost),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        onClick = { viewModel.startOrResume() },
                        modifier = Modifier.weight(2f)
                    )
                }

                if (state.hasStarted || state.elapsedSeconds > 0 || state.justCompleted) {
                    ActionCircle(
                        icon = Icons.Default.Stop,
                        contentDescription = "Stop",
                        container = Color(0xFF20394F),
                        tint = com.frostlock.app.ui.theme.DangerRed,
                        onClick = { viewModel.stop() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = AccentFrost
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Pro tip",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Put your phone face down and pick one task. Sessions of 5 minutes or longer are saved to your stats.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SegmentedControl(
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(SurfaceCard)
            .border(1.dp, StrokeSoft, MaterialTheme.shapes.extraLarge)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (label, key) ->
            val selected = key == selectedKey
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        if (selected) {
                            Brush.linearGradient(GradientFrost)
                        } else {
                            androidx.compose.ui.graphics.SolidColor(Color.Transparent)
                        }
                    )
                    .clickable { onSelect(key) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Color.White else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun DurationChip(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(if (selected) SurfaceHigh else SurfaceCard)
            .border(
                1.dp,
                if (selected) AccentFrost else StrokeSoft,
                MaterialTheme.shapes.extraLarge
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = when {
                !enabled -> TextSecondary.copy(alpha = 0.5f)
                selected -> TextPrimary
                else -> TextSecondary
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomDurationDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var text by remember { mutableStateOf(initialMinutes.toString()) }
    val minutes = text.toIntOrNull()?.coerceIn(1, 480) ?: 0

    fun setValue(value: Int) {
        text = value.coerceIn(1, 480).toString()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "Session length",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Any length from 1 minute to 8 hours.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    StepperButton(text = "−") { setValue(minutes - 5) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (minutes > 0) formatDuration(minutes * 60L) else "—",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "length",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    StepperButton(text = "+") { setValue(minutes + 5) }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        if (input.length <= 3 && input.all(Char::isDigit)) text = input
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Minutes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIce,
                        unfocusedBorderColor = SurfaceHigh,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentFrost,
                        focusedLabelColor = AccentFrost,
                        unfocusedLabelColor = TextSecondary
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CUSTOM_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = minutes == preset,
                            onClick = { setValue(preset) },
                            label = { Text("${preset}m") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceHigh,
                                labelColor = TextSecondary,
                                selectedContainerColor = PrimaryIce,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (minutes >= 1) onSave(minutes) },
                enabled = minutes >= 1
            ) {
                Text(text = "Set", color = AccentFrost)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun StepperButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(SurfaceHigh)
            .border(1.dp, StrokeSoft, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
    }
}

private val CUSTOM_PRESETS = listOf(10, 20, 30, 45, 60, 90, 120)

@Composable
private fun ActionCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    brush: Brush? = null,
    container: Color? = null,
    label: String? = null
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                when {
                    brush != null -> brush
                    container != null -> androidx.compose.ui.graphics.SolidColor(container)
                    else -> androidx.compose.ui.graphics.SolidColor(SurfaceHigh)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = tint
                )
            }
        }
    }
}

@Composable
internal fun HoldToEndButton(
    onEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var holding by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(holding) {
        if (holding) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 3_000, easing = LinearEasing))
            if (progress.value >= 1f) onEnd()
        } else {
            progress.animateTo(0f, tween(durationMillis = 200))
        }
    }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(SurfaceCard)
            .border(1.dp, StrokeSoft, MaterialTheme.shapes.extraLarge)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        holding = true
                        tryAwaitRelease()
                        holding = false
                    }
                )
            }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .align(Alignment.CenterStart)
                .background(
                    Brush.horizontalGradient(listOf(PrimaryIce, GlacierBlue))
                )
        )
        Text(
            text = if (holding) "Keep holding to end…" else "Hold to end session",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun SubjectChipLabel(name: String, count: Int?, colorArgb: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(Color(colorArgb))
        )
        Text(if (count != null) "$name · $count" else name)
    }
}

@Composable
private fun subjectChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = SurfaceHigh,
    labelColor = TextSecondary,
    selectedContainerColor = PrimaryIce,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
)

@Composable
private fun InfoPill(text: String) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(SurfaceCard)
            .border(1.dp, StrokeSoft, MaterialTheme.shapes.extraLarge)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
    }
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
        containerColor = SurfaceCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "Subject",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Name this session so it shows up in your subject breakdown.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        if (input.length <= 24) text = input
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Subject") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIce,
                        unfocusedBorderColor = SurfaceHigh,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentFrost,
                        focusedLabelColor = AccentFrost,
                        unfocusedLabelColor = TextSecondary
                    ),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = text.isNotBlank()
            ) {
                Text(text = "Save", color = AccentFrost)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun TimerRing(
    progress: Float,
    timeText: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(progress.coerceIn(0f, 1f), tween(durationMillis = 300))
    }

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow
        Canvas(modifier = Modifier.size(268.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrimaryIce.copy(alpha = 0.22f),
                        Color.Transparent
                    )
                )
            )
        }
        Canvas(modifier = Modifier.size(250.dp)) {
            val strokeWidth = 16.dp.toPx()
            val arcSize = Size(size.width, size.height)

            drawArc(
                color = Color(0xFF152838),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = arcSize
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(PrimaryIce, GlacierBlue, PrimaryIce)),
                startAngle = -90f,
                sweepAngle = animatedProgress.value * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = arcSize
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 52.sp),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AccentFrost
            )
        }
    }
}
