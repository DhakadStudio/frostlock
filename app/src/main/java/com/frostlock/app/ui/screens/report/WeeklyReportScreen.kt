package com.frostlock.app.ui.screens.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.PrimaryButton
import com.frostlock.app.ui.components.StatTile
import com.frostlock.app.ui.theme.AccentFrost
import com.frostlock.app.ui.theme.BackgroundDark
import com.frostlock.app.ui.theme.DangerRed
import com.frostlock.app.ui.theme.GlacierBlue
import com.frostlock.app.ui.theme.PrimaryIce
import com.frostlock.app.ui.theme.StrokeSoft
import com.frostlock.app.ui.theme.SuccessGreen
import com.frostlock.app.ui.theme.SurfaceHigh
import com.frostlock.app.ui.theme.TextPrimary
import com.frostlock.app.ui.theme.TextSecondary
import com.frostlock.app.ui.util.WeekDay
import com.frostlock.app.ui.util.WeekSubject
import com.frostlock.app.ui.util.WeeklyReport
import com.frostlock.app.ui.util.formatDuration
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * A local, offline weekly review: totals, a comparison with the previous week,
 * the daily rhythm, subject split and reflections. No account or upload.
 */
@Composable
fun WeeklyReportScreen(
    onBack: () -> Unit,
    onOpenDiary: () -> Unit,
    onStartFocus: () -> Unit,
    viewModel: WeeklyReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val report = state.report
    val period = if (state.isCurrentWeek) "this week" else "that week"

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundDark),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Weekly report",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Your focus, one week at a time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        item {
            WeekSwitcher(
                label = state.label,
                range = state.range,
                canGoForward = state.canGoForward,
                onPrevious = viewModel::previousWeek,
                onNext = viewModel::nextWeek
            )
        }

        item { HeroCard(report = report, isCurrentWeek = state.isCurrentWeek) }

        if (report.totalSeconds > 0L) {
            item { DailyRhythm(report = report) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatTile(
                        title = "Sessions",
                        value = report.sessions.toString(),
                        icon = Icons.Default.PlayCircle,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        title = "Avg session",
                        value = formatDuration(report.averageSessionSeconds),
                        icon = Icons.Default.Timer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatTile(
                        title = "Active days",
                        value = "${report.activeDays} / 7",
                        icon = Icons.Default.DateRange,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        title = "Reflections",
                        value = report.reflections.toString(),
                        icon = Icons.Default.EditNote,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            report.bestDay?.let { best ->
                item { BestDayCard(day = best) }
            }
        }

        if (report.subjects.isNotEmpty()) {
            item {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "By subject",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        SubjectBreakdown(subjects = report.subjects)
                    }
                }
            }
        }

        if (report.totalSeconds > 0L) {
            item {
                AppCard(modifier = Modifier.clickable(onClick = onOpenDiary)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SurfaceHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = AccentFrost,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (report.reflections == 0) {
                                    "No reflections yet"
                                } else {
                                    "${report.reflections} ${if (report.reflections == 1) "reflection" else "reflections"} $period"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Open the study diary",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        } else {
            item {
                AppCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SurfaceHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AcUnit,
                                contentDescription = null,
                                tint = AccentFrost,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "No focus logged $period",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "A single session is enough to start the graph.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        PrimaryButton(
                            text = "Start a focus session",
                            onClick = onStartFocus,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun WeekSwitcher(
    label: String,
    range: String,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous week",
                    tint = AccentFrost
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = range,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            IconButton(onClick = onNext, enabled = canGoForward) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next week",
                    tint = if (canGoForward) AccentFrost else SurfaceHigh
                )
            }
        }
    }
}

@Composable
private fun HeroCard(report: WeeklyReport, isCurrentWeek: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0B3B52), Color(0xFF071A28)))
            )
            .border(1.dp, StrokeSoft, MaterialTheme.shapes.large)
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isCurrentWeek) "FOCUSED THIS WEEK" else "FOCUSED THAT WEEK",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentFrost,
                    modifier = Modifier.weight(1f)
                )
                ChangeChip(report = report)
            }
            Text(
                text = formatDuration(report.totalSeconds),
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary
            )
            Text(
                text = if (report.sessions == 0) {
                    "No sessions logged"
                } else {
                    buildString {
                        append(report.sessions)
                        append(if (report.sessions == 1) " session" else " sessions")
                        append(" across ")
                        append(report.activeDays)
                        append(if (report.activeDays == 1) " day" else " days")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            if (report.previousSeconds > 0L) {
                Text(
                    text = "Week before: ${formatDuration(report.previousSeconds)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ChangeChip(report: WeeklyReport) {
    val change = report.changePercent
    val (text, color, icon) = when {
        change == null && report.totalSeconds > 0L ->
            Triple("Fresh start", SuccessGreen, null)
        change == null ->
            Triple("No data", TextSecondary, null)
        change > 0 -> Triple("+$change%", SuccessGreen, Icons.Default.ArrowUpward)
        change < 0 -> Triple("$change%", DangerRed, Icons.Default.ArrowDownward)
        else -> Triple("Same as last week", TextSecondary, null)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(SurfaceHigh.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
private fun DailyRhythm(report: WeeklyReport) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Daily rhythm",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                report.bestDay?.let { best ->
                    Text(
                        text = "Best: ${best.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentFrost
                    )
                }
            }
            WeeklyBars(days = report.days, bestDate = report.bestDay?.date)
        }
    }
}

@Composable
private fun WeeklyBars(days: List<WeekDay>, bestDate: java.time.LocalDate?) {
    val max = (days.maxOfOrNull { it.seconds } ?: 0L).coerceAtLeast(1L)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { day ->
            val isBest = day.date == bestDate
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                if (day.seconds > 0L) {
                    Text(
                        text = formatDuration(day.seconds),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isBest) AccentFrost else TextSecondary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                val fraction = day.seconds.toFloat() / max.toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.78f * fraction + 0.04f)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(
                            when {
                                isBest -> Brush.verticalGradient(
                                    listOf(PrimaryIce, GlacierBlue)
                                )
                                day.isToday -> Brush.verticalGradient(
                                    listOf(PrimaryIce.copy(alpha = 0.75f), GlacierBlue.copy(alpha = 0.55f))
                                )
                                else -> Brush.verticalGradient(
                                    listOf(PrimaryIce.copy(alpha = 0.4f), GlacierBlue.copy(alpha = 0.22f))
                                )
                            }
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        isBest -> AccentFrost
                        day.isToday -> TextPrimary
                        else -> TextSecondary
                    }
                )
            }
        }
    }
}

@Composable
private fun BestDayCard(day: WeekDay) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = AccentFrost,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Best day",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = day.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.US)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDuration(day.seconds),
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentFrost
                )
                Text(
                    text = "${day.sessions} ${if (day.sessions == 1) "session" else "sessions"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SubjectBreakdown(subjects: List<WeekSubject>) {
    val max = (subjects.maxOfOrNull { it.seconds } ?: 0L).coerceAtLeast(1L)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        subjects.forEach { subject ->
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(subject.colorArgb))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subject.tag,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatDuration(subject.seconds),
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
                                (subject.seconds.toFloat() / max.toFloat()).coerceIn(0.03f, 1f)
                            )
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(subject.colorArgb),
                                        Color(subject.colorArgb).copy(alpha = 0.45f)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
