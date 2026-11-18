package com.frostlock.app.ui.screens.schedule

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.frostlock.app.data.local.prefs.FocusSchedule
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.PrimaryButton
import com.frostlock.app.ui.components.SectionHeader
import com.frostlock.app.ui.theme.AccentFrost
import com.frostlock.app.ui.theme.BackgroundDark
import com.frostlock.app.ui.theme.DangerRed
import com.frostlock.app.ui.theme.PrimaryIce
import com.frostlock.app.ui.theme.SuccessGreen
import com.frostlock.app.ui.theme.SurfaceHigh
import com.frostlock.app.ui.theme.TextPrimary
import com.frostlock.app.ui.theme.TextSecondary
import com.frostlock.app.ui.theme.WarnAmber
import com.frostlock.app.ui.util.UpcomingSession
import com.frostlock.app.ui.util.hasOverlap
import com.frostlock.app.ui.util.upcomingCountdown
import com.frostlock.app.ui.util.upcomingSessions
import com.frostlock.app.ui.util.upcomingWhen
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val schedules by viewModel.schedules.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val activeIds by viewModel.activeIds.collectAsState()
    val scheduleStreak by viewModel.scheduleStreak.collectAsState()
    val remindersEnabled by viewModel.remindersEnabled.collectAsState()

    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FocusSchedule?>(null) }

    val labels = remember(apps) { apps.associate { it.packageName to it.label } }

    // Refresh the "Coming up" countdowns once a minute while the screen is open.
    val now by produceState(initialValue = ZonedDateTime.now()) {
        while (true) {
            delay(60_000)
            value = ZonedDateTime.now()
        }
    }
    val upcoming = remember(schedules, now) { upcomingSessions(schedules, now) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshActive()
                viewModel.refreshSubjects()
                viewModel.refreshStreak()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        text = "Blocking rules",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Different apps, days, dates and times",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        item {
            AppCard {
                Text(
                    text = "Each rule blocks its own set of apps, so “Study” and “Work” can lock different apps at different times. Rules run independently of the focus-blocking switch. When rules overlap, every matching rule applies.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentFrost
                )
            }
        }

        item {
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
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = AccentFrost,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Start & end reminders",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Local alerts when a rule starts or ends.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = viewModel::setRemindersEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = PrimaryIce,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SurfaceHigh
                        )
                    )
                }
            }
        }

        if (schedules.isNotEmpty()) {
            item {
                UpcomingCard(upcoming = upcoming, now = now)
            }
            item {
                StreakCard(days = scheduleStreak)
            }
        }

        item {
            SectionHeader(
                title = if (schedules.isEmpty()) {
                    "Your rules"
                } else {
                    "Your rules (${schedules.size})"
                }
            )
        }

        if (schedules.isEmpty()) {
            item {
                AppCard {
                    Text(
                        text = "No rules yet. Add one to lock a set of apps on chosen days, dates and times.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(schedules, key = { it.id }) { schedule ->
                ScheduleRow(
                    schedule = schedule,
                    labels = labels,
                    isActive = schedule.id in activeIds,
                    overlaps = hasOverlap(schedule, schedules),
                    onToggle = { viewModel.setEnabled(schedule.id, it) },
                    onEdit = {
                        editing = schedule
                        editorOpen = true
                    },
                    onDelete = { viewModel.delete(schedule.id) }
                )
            }
        }

        item {
            PrimaryButton(
                text = "Add rule",
                onClick = {
                    editing = null
                    editorOpen = true
                }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (editorOpen) {
        ScheduleEditorScreen(
            initial = editing,
            apps = apps,
            subjects = subjects,
            existing = schedules,
            onDismiss = { editorOpen = false },
            onSave = {
                viewModel.save(it)
                editorOpen = false
            }
        )
    }
}

@Composable
private fun ScheduleRow(
    schedule: FocusSchedule,
    labels: Map<String, String>,
    isActive: Boolean,
    overlaps: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = schedule.label,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        if (isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(SuccessGreen.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Active now",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SuccessGreen
                                )
                            }
                        }
                        if (overlaps) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.extraLarge)
                                    .background(WarnAmber.copy(alpha = 0.14f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Overlaps",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarnAmber
                                )
                            }
                        }
                    }
                    Text(
                        text = when {
                            schedule.dates.isNotEmpty() ->
                                schedule.dates.sorted().joinToString(", ") { formatDate(it) }
                            else -> daysSummary(schedule.days)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    dateRangeSummary(schedule.fromDate, schedule.untilDate)?.let { range ->
                        Text(
                            text = range,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "${formatTime(schedule.startMinutes)} – ${formatTime(schedule.endMinutes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentFrost
                    )
                }
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = PrimaryIce,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceHigh
                    )
                )
            }

            Text(
                text = appsSummary(schedule.packages, labels),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Text(text = "Edit", color = AccentFrost)
                }
                TextButton(onClick = onDelete) {
                    Text(text = "Delete", color = DangerRed)
                }
            }
        }
    }
}

@Composable
private fun StreakCard(days: Int) {
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
                    imageVector = Icons.Default.AcUnit,
                    contentDescription = null,
                    tint = if (days > 0) AccentFrost else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (days > 0) {
                        "Schedule streak · $days ${if (days == 1) "day" else "days"}"
                    } else {
                        "Schedule streak"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = if (days > 0) {
                        "You focused inside a rule window on $days scheduled ${if (days == 1) "day" else "days"} in a row."
                    } else {
                        "Start a session during a rule window to build one."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun UpcomingCard(upcoming: List<UpcomingSession>, now: ZonedDateTime) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(title = "Coming up") {
                Text(
                    text = "NEXT 7 DAYS",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentFrost
                )
            }
            if (upcoming.isEmpty()) {
                Text(
                    text = "No rule windows in the next 7 days. Paused rules and one-off dates that already passed are skipped.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                upcoming.forEach { session ->
                    UpcomingRow(session = session, now = now)
                }
            }
        }
    }
}

@Composable
private fun UpcomingRow(session: UpcomingSession, now: ZonedDateTime) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(SurfaceHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = AccentFrost,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.label,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = upcomingWhen(session.start, now) + " · " + when (session.packageCount) {
                    0 -> "focus apps"
                    1 -> "1 app"
                    else -> "${session.packageCount} apps"
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(SurfaceHigh)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = upcomingCountdown(session.start, now),
                style = MaterialTheme.typography.labelSmall,
                color = AccentFrost
            )
        }
    }
}

internal fun formatTime(minutes: Int): String =
    String.format(Locale.US, "%02d:%02d", (minutes / 60) % 24, minutes % 60)

internal fun formatDate(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
}.getOrDefault(iso)

/** Optional exam-month fence shown under the repeat line on a rule card. */
internal fun dateRangeSummary(fromDate: String?, untilDate: String?): String? = when {
    fromDate != null && untilDate != null -> "${formatDate(fromDate)} – ${formatDate(untilDate)}"
    fromDate != null -> "From ${formatDate(fromDate)}"
    untilDate != null -> "Until ${formatDate(untilDate)}"
    else -> null
}

internal fun daysSummary(days: Set<Int>): String {
    if (days.isEmpty()) return "No days selected"
    if (days.size == 7) return "Every day"
    return DAY_OPTIONS
        .filter { it.first in days }
        .joinToString(", ") { it.second }
}

private fun appsSummary(packages: Set<String>, labels: Map<String, String>): String {
    if (packages.isEmpty()) return "No apps chosen — uses the focus-session app list"
    val names = packages.map { labels[it] ?: it }.sorted()
    val preview = names.take(3).joinToString(", ")
    return if (names.size > 3) "$preview +${names.size - 3} more" else preview
}

internal val DAY_OPTIONS = listOf(
    1 to "Mon",
    2 to "Tue",
    3 to "Wed",
    4 to "Thu",
    5 to "Fri",
    6 to "Sat",
    7 to "Sun"
)
