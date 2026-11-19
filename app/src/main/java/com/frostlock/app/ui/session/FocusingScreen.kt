package com.frostlock.app.ui.session

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frostlock.app.data.InstalledApp
import com.frostlock.app.focus.FocusMode
import com.frostlock.app.focus.FocusPhase
import com.frostlock.app.focus.FocusUiState
import com.frostlock.app.ui.components.controls.FrostButton
import com.frostlock.app.ui.components.controls.FrostButtonVariant
import com.frostlock.app.ui.components.controls.HoldToEndButton
import com.frostlock.app.ui.components.dialogs.EndSessionDialog
import com.frostlock.app.ui.components.dialogs.TestBlockDialog
import com.frostlock.app.ui.theme.FrostShapes
import com.frostlock.app.ui.theme.FrostTheme
import com.frostlock.app.ui.theme.rememberReducedMotion
import com.frostlock.app.ui.util.ambienceFromKey
import com.frostlock.app.ui.util.formatClock
import com.frostlock.app.ui.util.TimerAutoSize

/**
 * The full-screen session: only the work, the time and what is held.
 *
 * The shell owns the dialog window and system-bar hiding; this composable is
 * just the content, which keeps it previewable and testable.
 */
@Composable
fun FocusingScreen(
    state: FocusUiState,
    onMinimize: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onDiscard: () -> Unit,
    onEndStrict: () -> Unit,
    onSkipBreak: () -> Unit,
    onToggleAmbience: () -> Unit
) {
    var confirmStop by remember { mutableStateOf(false) }
    var testApp by remember { mutableStateOf<InstalledApp?>(null) }
    val context = LocalContext.current

    val ringScale = remember { Animatable(0.92f) }
    val ringAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        ringScale.animateTo(1f, FrostTheme.motion.slowSpatial())
        ringAlpha.animateTo(1f, FrostTheme.motion.defaultEffects())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FrostSessionBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = FrostTheme.spacing.xxl, vertical = FrostTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SubjectBadge(
                    subject = state.tag,
                    color = state.selectedSubject?.colorArgb ?: 0xFF7DD3FC.toInt(),
                    isBreak = state.phase == FocusPhase.BREAK
                )
                Spacer(modifier = Modifier.weight(1f))
                if (state.phase != FocusPhase.BREAK) {
                    val sound = ambienceFromKey(state.ambienceKey)
                    IconButton(onClick = onToggleAmbience) {
                        Icon(
                            imageVector = if (sound.isOff) {
                                Icons.Default.VolumeOff
                            } else {
                                Icons.Default.VolumeUp
                            },
                            contentDescription = if (sound.isOff) {
                                "Sound off — tap to play " +
                                    ambienceFromKey(state.lastAudibleAmbienceKey).label
                            } else {
                                "Playing ${sound.label} — tap to mute"
                            },
                            tint = if (sound.isOff) {
                                FrostTheme.colors.textSecondary
                            } else {
                                FrostTheme.colors.primary
                            }
                        )
                    }
                }
                if (state.strictActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(FrostTheme.spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = FrostTheme.colors.warning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Strict",
                            style = FrostTheme.typography.labelMedium,
                            color = FrostTheme.colors.warning
                        )
                    }
                } else {
                    IconButton(onClick = onMinimize) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimise session",
                            tint = FrostTheme.colors.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.6f))

            Text(
                text = when {
                    state.phase == FocusPhase.BREAK -> "BREAK"
                    state.isRunning -> "FOCUSING"
                    else -> "PAUSED"
                },
                style = FrostTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                color = FrostTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))

            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = ringScale.value
                    scaleY = ringScale.value
                    alpha = ringAlpha.value
                }
            ) {
                ImmersiveTimerRing(
                    progress = state.progress,
                    timeText = if (state.mode == FocusMode.STOPWATCH) {
                        formatClock(state.elapsedSeconds)
                    } else {
                        formatClock(state.remainingSeconds)
                    },
                    subtitle = when {
                        state.phase == FocusPhase.BREAK -> "Phones are allowed"
                        state.justCompleted -> "Session complete"
                        state.isRunning && !state.strictActive && !state.pauseAllowance.canPause ->
                            "Pause limit reached"
                        state.isRunning -> "Round ${state.round}"
                        else -> "Paused"
                    },
                    isBreak = state.phase == FocusPhase.BREAK
                )
            }

            Spacer(modifier = Modifier.height(FrostTheme.spacing.xxl))

            ShieldStatus(
                state = state,
                onLongPress = {
                    state.sessionPackages.firstOrNull()?.let { pkg ->
                        state.installedApps.firstOrNull { it.packageName == pkg }
                    }?.let { testApp = it }
                },
                onFixAccess = { openAccessibilitySettings(context) }
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = when {
                    state.intent.isNotBlank() && state.phase == FocusPhase.FOCUS ->
                        "“${state.intent}”"
                    state.phase == FocusPhase.BREAK ->
                        "Step away. The next round starts on its own."
                    state.strictActive ->
                        "No early exit. The timer is in charge."
                    state.isRunning && !state.pauseAllowance.canPause ->
                        "Pause limit reached — keep going or end the session."
                    state.sessionPackages.isEmpty() ->
                        "No apps selected yet — this is a pure focus timer."
                    else -> "One task. No switching."
                },
                style = FrostTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(FrostTheme.spacing.lg))

            when {
                state.phase == FocusPhase.BREAK -> {
                    FrostButton(
                        text = "Skip break",
                        onClick = onSkipBreak,
                        icon = Icons.Default.SkipNext,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                state.strictActive -> {
                    HoldToEndButton(
                        onEnd = onEndStrict,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)
                    ) {
                        FrostButton(
                            text = "End",
                            onClick = {
                                if (state.elapsedSeconds >= 5 && !state.justCompleted) {
                                    confirmStop = true
                                } else {
                                    onStop()
                                }
                            },
                            variant = FrostButtonVariant.SECONDARY,
                            icon = Icons.Default.Stop,
                            modifier = Modifier.weight(1f)
                        )
                        FrostButton(
                            text = when {
                                state.isRunning && !state.pauseAllowance.canPause -> "No pauses left"
                                state.isRunning -> "Pause"
                                else -> "Resume"
                            },
                            onClick = if (state.isRunning) onPause else onResume,
                            enabled = !state.isRunning || state.pauseAllowance.canPause,
                            icon = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            modifier = Modifier.weight(1.4f)
                        )
                    }
                }
            }
        }
    }

    if (confirmStop) {
        EndSessionDialog(
            elapsedSeconds = state.elapsedSeconds,
            plannedSeconds = state.totalSeconds,
            onKeepGoing = { confirmStop = false },
            onSave = {
                confirmStop = false
                onStop()
            },
            onDiscard = {
                confirmStop = false
                onDiscard()
            }
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
}

@Composable
private fun SubjectBadge(subject: String, color: Int, isBreak: Boolean) {
    Row(
        modifier = Modifier
            .clip(FrostShapes.pill)
            .background(FrostTheme.colors.surfaceContainerHigh.copy(alpha = 0.7f))
            .border(1.dp, FrostTheme.colors.outline.copy(alpha = 0.6f), FrostShapes.pill)
            .padding(
                horizontal = FrostTheme.spacing.md,
                vertical = FrostTheme.spacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isBreak) FrostTheme.colors.secondary else Color(color))
        )
        Text(
            text = subject.trim().ifBlank { "No subject" },
            style = FrostTheme.typography.labelLarge,
            color = FrostTheme.colors.textPrimary
        )
    }
}

@Composable
private fun ShieldStatus(
    state: FocusUiState,
    onLongPress: () -> Unit,
    onFixAccess: () -> Unit
) {
    val isBreak = state.phase == FocusPhase.BREAK
    val accessOff = !state.canBlockApps && state.sessionPackages.isNotEmpty()
    val tone = when {
        isBreak -> FrostTheme.colors.textSecondary
        accessOff -> FrostTheme.colors.warning
        state.sessionPackages.isEmpty() -> FrostTheme.colors.textSecondary
        else -> FrostTheme.colors.success
    }
    val title = when {
        isBreak -> "Phones are allowed"
        state.sessionPackages.isEmpty() -> "No apps held"
        accessOff -> "Not blocking — access is off"
        else -> "${state.heldCount} app${if (state.heldCount == 1) "" else "s"} held"
    }
    val detail = when {
        isBreak -> "Blocking is paused until the break ends."
        state.sessionPackages.isEmpty() -> "This session is a pure focus timer."
        accessOff -> "Turn on accessibility to hold these apps."
        state.selectedSubject?.hasApps == true && state.tag.isNotBlank() ->
            "${state.tag} app set"
        else -> "Focus-session app list"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FrostShapes.large)
            .background(FrostTheme.colors.surfaceContainer.copy(alpha = 0.72f))
            .border(1.dp, FrostTheme.colors.outline.copy(alpha = 0.5f), FrostShapes.large)
            .pointerInput(state.sessionPackages) {
                detectTapGestures(onLongPress = { onLongPress() })
            }
            .padding(
                horizontal = FrostTheme.spacing.lg,
                vertical = FrostTheme.spacing.md
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(tone.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = tone,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(FrostTheme.spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = FrostTheme.typography.titleMedium,
                color = if (accessOff) tone else FrostTheme.colors.textPrimary
            )
            Text(
                text = detail,
                style = FrostTheme.typography.bodySmall,
                color = FrostTheme.colors.textSecondary
            )
        }
        if (accessOff) {
            TextButton(onClick = onFixAccess) {
                Text(
                    text = "Fix",
                    style = FrostTheme.typography.labelLarge,
                    color = FrostTheme.colors.primary
                )
            }
        }
    }
}

@Composable
private fun ImmersiveTimerRing(
    progress: Float,
    timeText: String,
    subtitle: String,
    isBreak: Boolean
) {
    val colors = FrostTheme.colors
    val glow = if (isBreak) colors.secondary else colors.primary
    val reduced = rememberReducedMotion()
    val breath = rememberInfiniteTransition(label = "timer glow")
    val animatedGlowAlpha by breath.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "timerGlowAlpha"
    )
    // Reduced motion keeps a static mid-glow instead of a pulsing one.
    val glowAlpha = if (reduced) 0.22f else animatedGlowAlpha

    Box(
        modifier = Modifier.size(288.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(280.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(glow.copy(alpha = glowAlpha), Color.Transparent)
                )
            )
        }
        Canvas(modifier = Modifier.size(266.dp)) {
            val strokeWidth = 10.dp.toPx()
            drawCircle(
                color = colors.surfaceContainerLowest.copy(alpha = 0.62f),
                radius = size.width * 0.445f
            )
            drawCircle(
                color = colors.outline.copy(alpha = 0.35f),
                radius = size.width * 0.445f,
                style = Stroke(width = 1.dp.toPx())
            )

            drawArc(
                color = colors.surfaceContainerHigh.copy(alpha = 0.7f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        colors.primary,
                        colors.secondary,
                        colors.tertiary,
                        colors.primary
                    )
                ),
                startAngle = -90f,
                sweepAngle = progress.coerceIn(0f, 1f) * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeText,
                style = FrostTheme.typography.displayTimer,
                color = FrostTheme.colors.textPrimary,
                autoSize = TimerAutoSize.autoSize()
            )
            Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
            Text(
                text = subtitle,
                style = FrostTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary
            )
        }
    }
}

/** A quiet palette-aware backdrop: two soft glows over the app background. */
@Composable
private fun FrostSessionBackdrop() {
    val primary = FrostTheme.colors.primary
    val tertiary = FrostTheme.colors.tertiary
    val top = FrostTheme.colors.surfaceContainerLow
    val bottom = FrostTheme.colors.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, bottom)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val left = Offset(width * 0.18f, height * 0.20f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tertiary.copy(alpha = 0.22f), Color.Transparent),
                    center = left,
                    radius = width * 0.9f
                ),
                radius = width * 0.9f,
                center = left
            )
            val right = Offset(width * 0.86f, height * 0.48f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.20f), Color.Transparent),
                    center = right,
                    radius = width * 0.8f
                ),
                radius = width * 0.8f,
                center = right
            )
        }
    }
}

private fun openAccessibilitySettings(context: android.content.Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
