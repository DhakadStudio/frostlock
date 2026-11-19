package com.frostlock.app.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.PrimaryButton
import com.frostlock.app.ui.util.GoalProgress
import com.frostlock.app.ui.util.goalDaysLabel
import com.frostlock.app.ui.util.goalMinutesLabel
import com.frostlock.app.ui.util.goalStatusLabel
import com.frostlock.app.ui.components.surface.frostPageBackground
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Local focus goals — exams, projects or personal targets. Progress comes from
 * the sessions already saved on this device; no calendar, account or network is
 * involved.
 */
@Composable
fun GoalsScreen(
    onBack: () -> Unit,
    onAddGoal: () -> Unit,
    onEditGoal: (Long) -> Unit,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .frostPageBackground()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = FrostTheme.colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Focus goals",
                    style = MaterialTheme.typography.headlineMedium,
                    color = FrostTheme.colors.textPrimary
                )
                Text(
                    text = "Local targets for exams and projects",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
            }
        }

        if (state.goals.isEmpty()) {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(FrostTheme.colors.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = FrostTheme.colors.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "No goals yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = FrostTheme.colors.textPrimary
                    )
                    Text(
                        text = "Set a target date and a focus-time goal. FrostLock " +
                            "counts only the sessions you finish before that date, " +
                            "all on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.goals.forEach { progress ->
                    GoalCard(
                        progress = progress,
                        onClick = { onEditGoal(progress.goal.id) }
                    )
                }
            }
        }

        PrimaryButton(
            text = if (state.goals.isEmpty()) "Set a focus goal" else "Add another goal",
            onClick = onAddGoal
        )
    }
}

@Composable
private fun GoalCard(progress: GoalProgress, onClick: () -> Unit) {
    val statusColor = when {
        progress.reached -> FrostTheme.colors.success
        progress.expired -> FrostTheme.colors.textSecondary
        progress.onTrack -> FrostTheme.colors.secondary
        else -> FrostTheme.colors.warning
    }

    AppCard(modifier = Modifier.clickable(onClick = onClick)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(FrostTheme.colors.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = progress.goal.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = FrostTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${goalStatusLabel(progress)} · ${goalDaysLabel(progress)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = FrostTheme.colors.textSecondary
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(FrostTheme.colors.surfaceContainerHigh)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.percent.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                if (progress.reached) {
                                    listOf(FrostTheme.colors.success, FrostTheme.colors.success)
                                } else {
                                    listOf(FrostTheme.colors.primary, FrostTheme.colors.glacier)
                                }
                            )
                        )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${goalMinutesLabel(progress.focusedMinutes)} of " +
                        goalMinutesLabel(progress.goal.targetMinutes.toLong()),
                    style = MaterialTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${(progress.percent * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (progress.reached) FrostTheme.colors.success else FrostTheme.colors.secondary
                )
            }
        }
    }
}
