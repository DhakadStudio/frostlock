package com.frostlock.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.MoodChip
import com.frostlock.app.ui.components.SectionHeader
import com.frostlock.app.ui.theme.AccentPurple
import com.frostlock.app.ui.theme.BackgroundDark
import com.frostlock.app.ui.theme.GradientViolet
import com.frostlock.app.ui.theme.PrimaryPurple
import com.frostlock.app.ui.theme.StrokeSoft
import com.frostlock.app.ui.theme.SuccessGreen
import com.frostlock.app.ui.theme.SurfaceHigh
import com.frostlock.app.ui.theme.TextPrimary
import com.frostlock.app.ui.theme.TextSecondary

private val GOAL_OPTIONS = listOf(30, 60, 90, 120, 180, 240)

@Composable
fun ProfileScreen(
    onNavigateToDesignPreview: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = "Tune FrostLock to your goals.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Free badge hero
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF2E2560), Color(0xFF1B1637))
                    )
                )
                .border(1.dp, StrokeSoft, MaterialTheme.shapes.medium)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(GradientViolet)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "F",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "FrostLock Pro",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Free forever · no ads · no subscriptions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SuccessGreen
                )
            }
        }

        SectionHeader(title = "Daily goal")
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "How much focused time do you want each day?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GOAL_OPTIONS.take(3).forEach { minutes ->
                        MoodChip(
                            text = formatGoal(minutes),
                            selected = state.dailyGoalMinutes == minutes,
                            onClick = { viewModel.setDailyGoalMinutes(minutes) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GOAL_OPTIONS.drop(3).forEach { minutes ->
                        MoodChip(
                            text = formatGoal(minutes),
                            selected = state.dailyGoalMinutes == minutes,
                            onClick = { viewModel.setDailyGoalMinutes(minutes) }
                        )
                    }
                }
            }
        }

        SectionHeader(title = "Strict Mode")
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "No early exit",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Sessions can't be paused or stopped until the timer runs out.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = state.strictMode,
                    onCheckedChange = viewModel::setStrictMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryPurple,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceHigh
                    )
                )
            }
        }

        SectionHeader(title = "Pomodoro breaks")
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Break between rounds",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "After each Pomodoro round, blocking pauses and a break timer starts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = state.breaksEnabled,
                        onCheckedChange = viewModel::setBreaksEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryPurple,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SurfaceHigh
                        )
                    )
                }
                if (state.breaksEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 5, 10).forEach { minutes ->
                            MoodChip(
                                text = "${minutes}m",
                                selected = state.breakMinutes == minutes,
                                onClick = { viewModel.setBreakMinutes(minutes) }
                            )
                        }
                    }
                }
            }
        }

        SectionHeader(title = "Focus Guard")
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gentle nudges",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "A reminder every 10 minutes during a focus session, so a quick check doesn't become a scroll.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = state.focusGuardEnabled,
                    onCheckedChange = viewModel::setFocusGuardEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryPurple,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceHigh
                    )
                )
            }
        }

        SectionHeader(title = "Home screen widget")
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Long-press your home screen → Widgets → FrostLock to see today's focus time, goal progress and streak at a glance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        SectionHeader(title = "Coming soon")
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•  Website blocker", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("•  Study diary & photos", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("•  Focus groups & leaderboards", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }

        SectionHeader(title = "About")
        AppCard {
            Text(
                text = "FrostLock is a free focus timer with a built-in app blocker. Track your sessions, build a daily streak, and keep distracting apps out of your way — plus scheduled auto-blocking and daily app limits.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun formatGoal(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "${rest}m"
        rest == 0 -> "${hours}h"
        else -> "${hours}h ${rest}m"
    }
}
