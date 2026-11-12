package com.focusflow.app.ui.screens.focus

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.focusflow.app.data.InstalledApp
import com.focusflow.app.ui.theme.AccentPurple
import com.focusflow.app.ui.theme.DangerRed
import com.focusflow.app.ui.theme.SuccessGreen
import com.focusflow.app.ui.theme.TextPrimary
import com.focusflow.app.ui.theme.TextSecondary
import com.focusflow.app.ui.theme.WarnAmber
import com.focusflow.app.ui.util.formatClock
import kotlin.random.Random

/**
 * A calm, full-screen space for a running session, inspired by Regain's aurora
 * timer. The configuration stays in the Focus tab; once the timer starts this
 * overlay takes over so the only thing on screen is the work, the time and the
 * apps being protected.
 */
@Composable
fun FocusImmersiveOverlay(
    state: FocusUiState,
    onMinimize: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onEndStrict: () -> Unit,
    onSkipBreak: () -> Unit,
    onTestBlock: (InstalledApp) -> Unit
) {
    val dismissible = !state.strictActive
    val testApp = remember(state.sessionPackages, state.installedApps) {
        state.sessionPackages.firstOrNull()?.let { pkg ->
            state.installedApps.firstOrNull { it.packageName == pkg }
        }
    }

    Dialog(
        onDismissRequest = { if (dismissible) onMinimize() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = dismissible,
            decorFitsSystemWindows = false
        )
    ) {
        HideSystemBars()
        Box(modifier = Modifier.fillMaxSize()) {
            AuroraBackdrop()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SubjectBadge(
                        subject = state.tag,
                        color = 0xFFA78BFA.toInt(),
                        isBreak = state.phase == FocusPhase.BREAK
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (state.strictActive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = WarnAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Strict",
                                style = MaterialTheme.typography.labelMedium,
                                color = WarnAmber
                            )
                        }
                    } else {
                        RoundIconButton(
                            icon = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimise focus screen",
                            onClick = onMinimize
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.7f))

                Text(
                    text = if (state.phase == FocusPhase.BREAK) "BREAK" else state.mode.label.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                    color = Color.White.copy(alpha = 0.55f)
                )
                Spacer(modifier = Modifier.height(10.dp))

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
                        state.isRunning -> "Stay in the zone"
                        else -> "Paused"
                    },
                    isBreak = state.phase == FocusPhase.BREAK
                )

                Spacer(modifier = Modifier.height(26.dp))

                BlockingPill(
                    count = state.sessionPackages.size,
                    subject = state.tag,
                    subjectOwnsApps = state.selectedSubject?.hasApps == true,
                    testApp = testApp,
                    enabled = testApp != null,
                    onTestBlock = { testApp?.let(onTestBlock) }
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = when {
                        state.phase == FocusPhase.BREAK ->
                            "Breathe, stretch, and come back for round ${state.round + 1}."
                        state.strictActive ->
                            "No early exit. The timer is in charge."
                        state.sessionPackages.isEmpty() ->
                            "No apps selected yet — this is a pure focus timer."
                        else ->
                            "One task. No switching."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.58f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))

                if (state.phase == FocusPhase.BREAK) {
                    PrimaryOverlayButton(
                        label = "Skip break",
                        icon = Icons.Default.SkipNext,
                        onClick = onSkipBreak
                    )
                } else if (state.strictActive) {
                    HoldToEndButton(
                        onEnd = onEndStrict,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RoundIconButton(
                            icon = Icons.Default.Stop,
                            contentDescription = "End session",
                            onClick = onStop,
                            tint = DangerRed
                        )
                        PrimaryOverlayButton(
                            label = if (state.isRunning) "Pause" else "Resume",
                            icon = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            onClick = if (state.isRunning) onPause else onResume,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectBadge(subject: String, color: Int, isBreak: Boolean) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isBreak) Color(0xFF22D3EE) else Color(color))
        )
        Text(
            text = subject.trim().ifBlank { "Untagged" },
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun PrimaryOverlayButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF0A0A12),
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF0A0A12),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BlockingPill(
    count: Int,
    subject: String,
    subjectOwnsApps: Boolean,
    testApp: InstalledApp?,
    enabled: Boolean,
    onTestBlock: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(SuccessGreen.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when {
                    count > 0 -> "Blocking $count app${if (count == 1) "" else "s"}"
                    else -> "No apps selected"
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = when {
                    count > 0 && subjectOwnsApps && subject.isNotBlank() -> "$subject app set"
                    count > 0 -> "Focus-session app list"
                    else -> "Pick a subject to protect this session"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
        if (testApp != null) {
            TextButton(onClick = onTestBlock) {
                Text(
                    text = "Test",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (enabled) Color(0xFF7DD3FC) else TextSecondary
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
    val glow = if (isBreak) Color(0xFF22D3EE) else Color(0xFFA78BFA)
    Box(
        modifier = Modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(292.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(glow.copy(alpha = 0.28f), Color.Transparent)
                )
            )
        }
        Canvas(modifier = Modifier.size(278.dp)) {
            val strokeWidth = 10.dp.toPx()
            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFFA78BFA),
                        Color(0xFF22D3EE),
                        Color(0xFFF0ABFC),
                        Color(0xFFA78BFA)
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
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.62f)
            )
        }
    }
}

@Composable
private fun AuroraBackdrop() {
    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraDrift"
    )
    val stars = remember {
        List(78) {
            Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF04040B))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF07161A), Color(0xFF0B0A1B), Color(0xFF04040B))
                )
            )
            val tealCenter = Offset(width * (0.16f + drift * 0.18f), height * 0.20f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2DD4BF).copy(alpha = 0.34f),
                        Color.Transparent
                    ),
                    center = tealCenter,
                    radius = width * 0.92f
                ),
                radius = width * 0.92f,
                center = tealCenter
            )
            val violetCenter = Offset(width * (0.88f - drift * 0.14f), height * 0.46f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7C3AED).copy(alpha = 0.34f),
                        Color.Transparent
                    ),
                    center = violetCenter,
                    radius = width * 0.85f
                ),
                radius = width * 0.85f,
                center = violetCenter
            )
            val roseCenter = Offset(width * (0.30f + drift * 0.22f), height * 0.88f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF472B6).copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = roseCenter,
                    radius = width * 0.70f
                ),
                radius = width * 0.70f,
                center = roseCenter
            )
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            stars.forEach { (x, y, brightness) ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f + brightness * 0.14f),
                    radius = 0.9f + brightness * 1.6f,
                    center = Offset(x * size.width, y * size.height)
                )
            }
        }
    }
}

@Composable
private fun HideSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.parent as? DialogWindowProvider)?.window
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, view).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            if (window != null) {
                WindowInsetsControllerCompat(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

@Composable
fun TestBlockDialog(
    app: InstalledApp,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14141F),
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        icon = {
            if (app.icon != null) {
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = SuccessGreen
                    )
                }
            }
        },
        title = {
            Text(
                text = "Try opening ${app.label}",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Text(
                text = "We've blocked it for you. Open the app to see FocusFlow's block screen — then come right back.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onOpen) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Open ${app.label}", color = AccentPurple)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Later", color = TextSecondary)
            }
        }
    )
}
