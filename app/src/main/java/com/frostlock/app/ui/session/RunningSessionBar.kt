package com.frostlock.app.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frostlock.app.focus.FocusMode
import com.frostlock.app.focus.FocusPhase
import com.frostlock.app.focus.FocusUiState
import com.frostlock.app.ui.components.surface.frostGlass
import com.frostlock.app.ui.theme.FrostShapes
import com.frostlock.app.ui.theme.FrostTheme
import com.frostlock.app.ui.util.ambienceFromKey
import com.frostlock.app.ui.util.formatClock

/**
 * The running-session bar shown above the navigation bar after a session is
 * minimised.
 *
 * Tapping it returns to the full-screen session; the pause button is the one
 * quick action that does not need the whole screen.
 */
@Composable
fun RunningSessionBar(
    state: FocusUiState,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onToggleAmbience: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBreak = state.phase == FocusPhase.BREAK
    val canPause = state.isRunning && !isBreak && state.pauseAllowance.canPause
    val tone = when {
        isBreak -> FrostTheme.colors.secondary
        state.isRunning -> FrostTheme.colors.success
        else -> FrostTheme.colors.warning
    }

    val shape = FrostShapes.large
    Row(
        modifier = modifier
            .fillMaxWidth()
            .frostGlass(shape = shape, strong = true)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onOpen)
            .semantics { contentDescription = "Open running session" }
            .padding(
                horizontal = FrostTheme.spacing.lg,
                vertical = FrostTheme.spacing.sm
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
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
                text = runningSessionTitle(state),
                style = FrostTheme.typography.titleMedium,
                color = FrostTheme.colors.textPrimary
            )
            Text(
                text = runningSessionDetail(state),
                style = FrostTheme.typography.bodySmall,
                color = FrostTheme.colors.textSecondary
            )
        }
        if (!isBreak) {
            val sound = ambienceFromKey(state.ambienceKey)
            IconButton(onClick = onToggleAmbience) {
                Icon(
                    imageVector = if (sound.isOff) {
                        Icons.Default.VolumeOff
                    } else {
                        Icons.Default.VolumeUp
                    },
                    contentDescription = if (sound.isOff) "Play sound" else "Mute sound",
                    tint = if (sound.isOff) {
                        FrostTheme.colors.textSecondary
                    } else {
                        FrostTheme.colors.primary
                    }
                )
            }
            IconButton(
                onClick = { if (state.isRunning) onPause() else onResume() },
                enabled = !state.isRunning || canPause
            ) {
                Icon(
                    imageVector = if (state.isRunning) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = if (state.isRunning) "Pause session" else "Resume session",
                    tint = if (!state.isRunning || canPause) {
                        FrostTheme.colors.textPrimary
                    } else {
                        FrostTheme.colors.textSecondary
                    }
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(FrostShapes.pill)
                .background(FrostTheme.colors.surfaceContainer)
                .padding(horizontal = FrostTheme.spacing.md, vertical = FrostTheme.spacing.xs)
        ) {
            Text(
                text = runningSessionTime(state),
                style = FrostTheme.typography.labelLarge,
                color = tone
            )
        }
    }
}

/** "Focusing · 24:58", "Break · 04:12" or "Paused · 12:34". */
internal fun runningSessionTitle(state: FocusUiState): String {
    val prefix = when {
        state.phase == FocusPhase.BREAK -> "Break"
        !state.isRunning -> "Paused"
        else -> "Focusing"
    }
    return "$prefix · ${runningSessionTime(state)}"
}

private fun runningSessionTime(state: FocusUiState): String =
    if (state.mode == FocusMode.STOPWATCH) {
        formatClock(state.elapsedSeconds)
    } else {
        formatClock(state.remainingSeconds)
    }

/** Subject and round, or an honest line when there is nothing else to say. */
internal fun runningSessionDetail(state: FocusUiState): String {
    val subject = state.tag.trim().ifBlank { "No subject" }
    return when {
        state.phase == FocusPhase.BREAK -> "$subject · break before round ${state.round + 1}"
        state.mode == FocusMode.STOPWATCH -> "$subject · stopwatch"
        else -> "$subject · round ${state.round}"
    }
}
