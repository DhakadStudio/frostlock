package com.focusflow.app.ui.screens.focus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusflow.app.ui.components.AppCard
import com.focusflow.app.ui.theme.AccentPurple
import com.focusflow.app.ui.theme.BackgroundDark
import com.focusflow.app.ui.theme.GradientViolet
import com.focusflow.app.ui.theme.PrimaryIndigo
import com.focusflow.app.ui.theme.PrimaryPurple
import com.focusflow.app.ui.theme.StrokeSoft
import com.focusflow.app.ui.theme.SuccessGreen
import com.focusflow.app.ui.theme.SurfaceCard
import com.focusflow.app.ui.theme.SurfaceHigh
import com.focusflow.app.ui.theme.TextPrimary
import com.focusflow.app.ui.theme.TextSecondary
import com.focusflow.app.ui.util.formatClock

private val DURATION_OPTIONS = listOf(15, 25, 45, 60)

@Composable
fun FocusScreen(
    viewModel: FocusViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

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

        // Duration chips
        if (state.mode != FocusMode.STOPWATCH) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DURATION_OPTIONS.forEach { minutes ->
                    DurationChip(
                        text = "${minutes}m",
                        selected = state.targetMinutes == minutes,
                        onClick = { viewModel.selectMinutes(minutes) }
                    )
                }
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

        if (state.strictActive) {
            Text(
                text = "Strict Mode is on. This session can't be paused or ended early.",
                style = MaterialTheme.typography.bodyMedium,
                color = AccentPurple,
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
                        container = Color(0xFF2B2650),
                        tint = TextPrimary,
                        onClick = { viewModel.pause() },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    ActionCircle(
                        icon = Icons.Default.PlayArrow,
                        label = when {
                            state.justCompleted -> "Start again"
                            state.hasStarted && state.elapsedSeconds > 0 -> "Resume"
                            else -> "Start"
                        },
                        brush = Brush.linearGradient(GradientViolet),
                        tint = Color.White,
                        onClick = { viewModel.startOrResume() },
                        modifier = Modifier.weight(2f)
                    )
                }

                if (state.hasStarted || state.elapsedSeconds > 0 || state.justCompleted) {
                    ActionCircle(
                        icon = Icons.Default.Stop,
                        contentDescription = "Stop",
                        container = Color(0xFF2B2650),
                        tint = com.focusflow.app.ui.theme.DangerRed,
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
                    tint = AccentPurple
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
                            Brush.linearGradient(GradientViolet)
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
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(if (selected) SurfaceHigh else SurfaceCard)
            .border(
                1.dp,
                if (selected) AccentPurple else StrokeSoft,
                MaterialTheme.shapes.extraLarge
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) TextPrimary else TextSecondary
        )
    }
}

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
private fun HoldToEndButton(
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
                    Brush.horizontalGradient(listOf(PrimaryPurple, PrimaryIndigo))
                )
        )
        Text(
            text = if (holding) "Keep holding to end…" else "Hold to end session",
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary
        )
    }
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
                        PrimaryPurple.copy(alpha = 0.22f),
                        Color.Transparent
                    )
                )
            )
        }
        Canvas(modifier = Modifier.size(250.dp)) {
            val strokeWidth = 16.dp.toPx()
            val arcSize = Size(size.width, size.height)

            drawArc(
                color = Color(0xFF221E3D),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = arcSize
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(PrimaryPurple, PrimaryIndigo, PrimaryPurple)),
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
                color = AccentPurple
            )
        }
    }
}
