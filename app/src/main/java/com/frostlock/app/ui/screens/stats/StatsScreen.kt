package com.frostlock.app.ui.screens.stats

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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.ui.components.AchievementBadge
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.SectionHeader
import com.frostlock.app.ui.components.SessionRow
import com.frostlock.app.ui.components.StatTile
import com.frostlock.app.ui.theme.AccentFrost
import com.frostlock.app.ui.theme.GradientHero
import com.frostlock.app.ui.theme.BackgroundDark
import com.frostlock.app.ui.theme.GlacierBlue
import com.frostlock.app.ui.theme.PrimaryIce
import com.frostlock.app.ui.theme.StrokeSoft
import com.frostlock.app.ui.theme.SurfaceHigh
import com.frostlock.app.ui.theme.TextPrimary
import com.frostlock.app.ui.theme.TextSecondary
import com.frostlock.app.ui.util.AchievementCategory
import com.frostlock.app.ui.util.FocusLevel
import com.frostlock.app.ui.util.MonthlySummary
import com.frostlock.app.ui.util.TagTotal
import com.frostlock.app.ui.util.formatDuration
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun StatsScreen(
    onOpenAchievements: () -> Unit,
    onOpenDiary: () -> Unit,
    onOpenWeeklyReport: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenLevels: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val previewBadges = remember(state.achievements) {
        AchievementCategory.entries.mapNotNull { category ->
            val inCategory = state.achievements.filter { it.category == category }
            inCategory.lastOrNull { it.unlocked } ?: inCategory.firstOrNull()
        }
    }

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
                            GradientHero
                        )
                    )
                    .border(1.dp, StrokeSoft, MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentFrost
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
                            GradientHero
                        )
                    )
                    .border(1.dp, StrokeSoft, MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AcUnit,
                            contentDescription = null,
                            tint = AccentFrost,
                            modifier = Modifier.width(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Streak",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AccentFrost
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

        LevelCard(level = state.level, onClick = onOpenLevels)

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

        MonthlyCard(summary = state.monthly)

        AppCard(modifier = Modifier.clickable(onClick = onOpenCalendar)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = AccentFrost,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Focus calendar",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Browse any month and tap a day to see its sessions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }

        // Achievements preview
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionHeader(title = "Achievements") {
                    androidx.compose.material3.TextButton(onClick = onOpenAchievements) {
                        Text(
                            text = "View all",
                            color = AccentFrost,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                if (previewBadges.isEmpty()) {
                    Text(
                        text = "Finish a focus session to earn your first badge.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        previewBadges.forEach { achievement ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AchievementBadge(achievement = achievement, size = 58.dp)
                                Text(
                                    text = achievement.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (achievement.unlocked) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }
                    Text(
                        text = "${state.unlockedCount} of ${state.achievements.size} badges collected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // Weekly chart
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(title = "Last 7 days") {
                    androidx.compose.material3.TextButton(onClick = onOpenWeeklyReport) {
                        Text(
                            text = "Report",
                            color = AccentFrost,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
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

        SectionHeader(title = "Recent sessions") {
            androidx.compose.material3.TextButton(onClick = onOpenDiary) {
                Text(
                    text = "Diary",
                    color = AccentFrost,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

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
private fun LevelCard(level: FocusLevel, onClick: () -> Unit) {
    AppCard(modifier = Modifier.clickable(onClick = onClick)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AcUnit,
                        contentDescription = null,
                        tint = AccentFrost,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FREEZE LEVEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "Level ${level.level} · ${level.title}",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                }
                Text(
                    text = if (level.totalMinutes >= 60) {
                        "${level.totalMinutes / 60}h ${level.totalMinutes % 60}m total"
                    } else {
                        "${level.totalMinutes}m total"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentFrost
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceHigh)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(level.progress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.horizontalGradient(listOf(PrimaryIce, GlacierBlue)))
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${level.minutesIntoLevel}m / ${level.minutesForNextLevel}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${level.minutesToNextLevel}m to level ${level.level + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentFrost
                )
            }
        }
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
private fun MonthlyCard(summary: MonthlySummary) {
    val current = summary.current
    val max = (summary.months.maxOfOrNull { it.seconds } ?: 0L).coerceAtLeast(1L)
    val monthName = current.month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
    val sessionLabel = if (current.sessions == 1) "1 session" else "${current.sessions} sessions"

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monthly rhythm",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "$monthName · $sessionLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = formatDuration(current.seconds),
                    style = MaterialTheme.typography.titleLarge,
                    color = AccentFrost
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                summary.months.forEach { month ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        val fraction = month.seconds.toFloat() / max.toFloat()
                        val isCurrent = month.month == current.month
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.78f * fraction + 0.03f)
                                .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                                .background(
                                    Brush.verticalGradient(
                                        if (isCurrent) {
                                            listOf(PrimaryIce, GlacierBlue)
                                        } else {
                                            listOf(
                                                PrimaryIce.copy(alpha = 0.48f),
                                                GlacierBlue.copy(alpha = 0.28f)
                                            )
                                        }
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = month.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrent) AccentFrost else TextSecondary
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                MonthMetric(
                    value = current.activeDays.toString(),
                    label = "Active days",
                    modifier = Modifier.weight(1f)
                )
                MonthMetric(
                    value = if (current.bestDaySeconds > 0L) {
                        formatDuration(current.bestDaySeconds)
                    } else {
                        "—"
                    },
                    label = "Best day",
                    modifier = Modifier.weight(1f)
                )
                MonthMetric(
                    value = if (current.activeDays > 0) {
                        formatDuration(summary.averagePerActiveDay)
                    } else {
                        "—"
                    },
                    label = "Avg / active day",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MonthMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
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
                        color = if (day.isToday) AccentFrost else TextSecondary
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
                                        com.frostlock.app.ui.theme.PrimaryIce,
                                        com.frostlock.app.ui.theme.GlacierBlue
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(
                                        com.frostlock.app.ui.theme.PrimaryIce.copy(alpha = 0.55f),
                                        com.frostlock.app.ui.theme.GlacierBlue.copy(alpha = 0.35f)
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
