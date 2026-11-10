package com.focusflow.app.ui.screens.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusflow.app.ui.components.AppCard
import com.focusflow.app.ui.components.PrimaryButton
import com.focusflow.app.ui.components.SectionHeader
import com.focusflow.app.ui.components.SessionRow
import com.focusflow.app.ui.components.StatTile
import com.focusflow.app.ui.components.StreakRing
import com.focusflow.app.ui.theme.AccentPurple
import com.focusflow.app.ui.theme.BackgroundDark
import com.focusflow.app.ui.theme.StrokeSoft
import com.focusflow.app.ui.theme.SurfaceHigh
import com.focusflow.app.ui.theme.TextPrimary
import com.focusflow.app.ui.theme.TextSecondary
import com.focusflow.app.ui.util.formatDuration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    onStartFocus: () -> Unit,
    onOpenBlock: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Greeting
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = LocalDate.now().format(
                    DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
                ),
                style = MaterialTheme.typography.labelMedium,
                color = AccentPurple
            )
            Text(
                text = "Get in the zone",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
        }

        // Hero: today's progress
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF2E2560), Color(0xFF1B1637))
                    )
                )
                .border(1.dp, StrokeSoft, MaterialTheme.shapes.large)
                .padding(vertical = 28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StreakRing(
                    streakCount = state.streak,
                    progress = state.goalProgress
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = formatDuration(state.todaySeconds),
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary
                )
                Text(
                    text = if (state.goalReached) {
                        "Daily goal reached — beautiful work"
                    } else {
                        "focused today · goal ${formatDuration(state.dailyGoalSeconds)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentPurple,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Primary action
        PrimaryButton(
            text = "Start focus session",
            onClick = onStartFocus
        )

        // Quick stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatTile(
                title = "This week",
                value = formatDuration(state.weekSeconds),
                icon = Icons.Default.Work,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                title = "Sessions",
                value = state.totalSessions.toString(),
                icon = Icons.Default.Star,
                modifier = Modifier.weight(1f)
            )
        }

        // Quick links
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickAction(
                title = "Block apps",
                subtitle = "Protect your focus",
                icon = Icons.Default.Block,
                onClick = onOpenBlock,
                modifier = Modifier.weight(1f)
            )
            QuickAction(
                title = "Schedules",
                subtitle = "Auto-block weekly",
                icon = Icons.Default.CalendarMonth,
                onClick = onOpenBlock,
                modifier = Modifier.weight(1f)
            )
        }

        SectionHeader(title = "Recent sessions")

        if (state.recent.isEmpty()) {
            AppCard {
                Text(
                    text = "No sessions yet. Start your first focus session to build your streak.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            state.recent.forEach { session ->
                SessionRow(session = session)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun QuickAction(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(SurfaceHigh)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            com.focusflow.app.ui.theme.PrimaryPurple,
                            com.focusflow.app.ui.theme.PrimaryIndigo
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
