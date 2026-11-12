package com.focusflow.app.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Timer
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
import com.focusflow.app.ui.components.SectionHeader
import com.focusflow.app.ui.components.SessionRow
import com.focusflow.app.ui.components.StatTile
import com.focusflow.app.ui.theme.AccentPurple
import com.focusflow.app.ui.theme.BackgroundDark
import com.focusflow.app.ui.theme.PrimaryIndigo
import com.focusflow.app.ui.theme.PrimaryPurple
import com.focusflow.app.ui.theme.StrokeSoft
import com.focusflow.app.ui.theme.SurfaceHigh
import com.focusflow.app.ui.theme.TextPrimary
import com.focusflow.app.ui.theme.TextSecondary
import com.focusflow.app.ui.theme.WarnAmber
import com.focusflow.app.ui.util.TagTotal
import com.focusflow.app.ui.util.formatDuration

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
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
                text = "Statistics",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = "Your focus, week by week.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Hero: today + streak side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF2E2560), Color(0xFF1B1637))
                        )
                    )
                    .border(1.dp, StrokeSoft, MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentPurple
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatDuration(state.todaySeconds),
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "focused so far",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF2E2560), Color(0xFF1B1637))
                        )
                    )
                    .border(1.dp, StrokeSoft, MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = WarnAmber,
                            modifier = Modifier.width(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Streak",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarnAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${state.streak}d",
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "keep it going",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // Grid stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatTile(
                title = "This week",
                value = formatDuration(state.weekSeconds),
                icon = Icons.Default.DateRange,
                modifier = Modifier.weight(1f)
            )
            StatTile(
                title = "Sessions",
                value = state.totalSessions.toString(),
                icon = Icons.Default.PlayCircle,
                modifier = Modifier.weight(1f)
            )
        }

        // Weekly chart
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Last 7 days",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                WeeklyChart(days = state.days)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.width(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Total ${formatDuration(state.totalSeconds)} across ${state.totalSessions} sessions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (state.tags.isNotEmpty()) {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "By subject",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    SubjectBreakdown(tags = state.tags)
                }
            }
        }

        SectionHeader(title = "Recent sessions")

        if (state.recent.isEmpty()) {
            AppCard {
                Text(
                    text = "No sessions yet.",
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
private fun SubjectBreakdown(tags: List<TagTotal>) {
    val max = (tags.maxOfOrNull { it.seconds } ?: 0L).coerceAtLeast(1L)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tags.forEach { tag ->
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(tag.colorArgb))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tag.tag,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatDuration(tag.seconds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(SurfaceHigh)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(
                                (tag.seconds.toFloat() / max.toFloat()).coerceIn(0.03f, 1f)
                            )
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(tag.colorArgb),
                                        Color(tag.colorArgb).copy(alpha = 0.45f)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyChart(days: List<DayBar>) {
    val max = (days.maxOfOrNull { it.seconds } ?: 0L).coerceAtLeast(1L)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { day ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (day.seconds > 0L) {
                    // Value above the bar
                    Text(
                        text = formatDuration(day.seconds),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (day.isToday) AccentPurple else TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                val fraction = if (day.seconds <= 0L) 0f else day.seconds.toFloat() / max.toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.82f * fraction + 0.03f)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(
                            if (day.isToday) {
                                Brush.verticalGradient(
                                    listOf(
                                        com.focusflow.app.ui.theme.PrimaryPurple,
                                        com.focusflow.app.ui.theme.PrimaryIndigo
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        com.focusflow.app.ui.theme.PrimaryPurple.copy(alpha = 0.55f),
                                        com.focusflow.app.ui.theme.PrimaryIndigo.copy(alpha = 0.35f)
                                    )
                                )
                            }
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (day.isToday) TextPrimary else TextSecondary
                )
            }
        }
    }
}
