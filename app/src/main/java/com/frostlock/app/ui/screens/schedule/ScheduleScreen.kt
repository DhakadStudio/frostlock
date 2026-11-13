package com.frostlock.app.ui.screens.schedule

import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.frostlock.app.data.InstalledApp
import com.frostlock.app.data.local.prefs.FocusSchedule
import com.frostlock.app.data.local.prefs.FocusSubject
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.AppPickerDialog
import com.frostlock.app.ui.components.PrimaryButton
import com.frostlock.app.ui.components.SectionHeader
import com.frostlock.app.ui.theme.AccentFrost
import com.frostlock.app.ui.theme.BackgroundDark
import com.frostlock.app.ui.theme.DangerRed
import com.frostlock.app.ui.theme.GradientFrost
import com.frostlock.app.ui.theme.PrimaryIce
import com.frostlock.app.ui.theme.StrokeSoft
import com.frostlock.app.ui.theme.SuccessGreen
import com.frostlock.app.ui.theme.SurfaceCard
import com.frostlock.app.ui.theme.SurfaceHigh
import com.frostlock.app.ui.theme.TextPrimary
import com.frostlock.app.ui.theme.TextSecondary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
    val remindersEnabled by viewModel.remindersEnabled.collectAsState()

    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FocusSchedule?>(null) }

    val labels = remember(apps) { apps.associate { it.packageName to it.label } }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshActive()
                viewModel.refreshSubjects()
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
                    text = "Each rule blocks its own set of apps, so “Study” and “Work” can lock different apps at different times. Rules run independently of the focus-blocking switch.",
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
        ScheduleEditorDialog(
            initial = editing,
            apps = apps,
            subjects = subjects,
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleEditorDialog(
    initial: FocusSchedule?,
    apps: List<InstalledApp>,
    subjects: List<FocusSubject>,
    onDismiss: () -> Unit,
    onSave: (FocusSchedule) -> Unit
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf(initial?.label ?: "Focus") }
    var days by remember { mutableStateOf(initial?.days ?: setOf(1, 2, 3, 4, 5)) }
    var dates by remember { mutableStateOf(initial?.dates ?: emptySet()) }
    var packages by remember { mutableStateOf(initial?.packages ?: emptySet()) }
    var startMinutes by remember { mutableIntStateOf(initial?.startMinutes ?: 9 * 60) }
    var endMinutes by remember { mutableIntStateOf(initial?.endMinutes ?: 17 * 60) }
    var error by remember { mutableStateOf<String?>(null) }
    var appPickerOpen by remember { mutableStateOf(false) }
    var datePickerOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(text = if (initial == null) "New blocking rule" else "Edit blocking rule")
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIce,
                        unfocusedBorderColor = SurfaceHigh,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentFrost,
                        focusedLabelColor = AccentFrost,
                        unfocusedLabelColor = TextSecondary
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                Text(
                    text = "Apps to block (${packages.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                OutlinedButton(
                    onClick = { appPickerOpen = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = AccentFrost,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (packages.isEmpty()) {
                            "Choose apps"
                        } else {
                            "Edit selection"
                        },
                        color = AccentFrost
                    )
                }

                val reusableSubjects = subjects.filter { it.hasApps }
                if (reusableSubjects.isNotEmpty()) {
                    Text(
                        text = "Or copy apps from a subject",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        reusableSubjects.forEach { subject ->
                            FilterChip(
                                selected = packages == subject.packages,
                                onClick = { packages = subject.packages },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Color(subject.colorArgb))
                                        )
                                        Text("${subject.name} · ${subject.packages.size}")
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SurfaceHigh,
                                    labelColor = TextSecondary,
                                    selectedContainerColor = PrimaryIce,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                Text(
                    text = "Repeat on",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        days = setOf(1, 2, 3, 4, 5)
                        dates = emptySet()
                        error = null
                    }) {
                        Text(text = "Weekdays", color = AccentFrost)
                    }
                    TextButton(onClick = {
                        days = setOf(1, 2, 3, 4, 5, 6, 7)
                        dates = emptySet()
                        error = null
                    }) {
                        Text(text = "Every day", color = AccentFrost)
                    }
                    TextButton(onClick = {
                        days = emptySet()
                        error = null
                    }) {
                        Text(text = "Clear", color = TextSecondary)
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DAY_OPTIONS.forEach { (value, name) ->
                        FilterChip(
                            selected = value in days,
                            onClick = {
                                days = if (value in days) days - value else days + value
                                error = null
                            },
                            label = { Text(text = name) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceHigh,
                                labelColor = TextSecondary,
                                selectedContainerColor = PrimaryIce,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Specific dates",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { datePickerOpen = true }) {
                        Text(text = "Add date", color = AccentFrost)
                    }
                }

                if (dates.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dates.sorted().forEach { iso ->
                            FilterChip(
                                selected = true,
                                onClick = { dates = dates - iso },
                                label = { Text(text = formatDate(iso)) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove date",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SurfaceHigh,
                                    labelColor = TextPrimary,
                                    selectedContainerColor = PrimaryIce,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                    Text(
                        text = "Date rules run once on those dates; weekdays above are ignored.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { pickTime(context, startMinutes) { startMinutes = it } },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Start ${formatTime(startMinutes)}")
                    }
                    OutlinedButton(
                        onClick = { pickTime(context, endMinutes) { endMinutes = it } },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "End ${formatTime(endMinutes)}")
                    }
                }

                Text(
                    text = "Windows can run overnight, e.g. 22:00 – 02:00.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DangerRed
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        label.isBlank() -> error = "Give this rule a name."
                        days.isEmpty() && dates.isEmpty() -> error = "Pick at least one day or date."
                        startMinutes == endMinutes -> error = "Start and end can't be the same."
                        else -> onSave(
                            FocusSchedule(
                                id = initial?.id ?: System.currentTimeMillis(),
                                label = label.trim(),
                                days = if (dates.isEmpty()) days else emptySet(),
                                dates = dates,
                                startMinutes = startMinutes,
                                endMinutes = endMinutes,
                                packages = packages,
                                enabled = initial?.enabled ?: true
                            )
                        )
                    }
                }
            ) {
                Text(text = "Save", color = AccentFrost)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextSecondary)
            }
        }
    )

    if (appPickerOpen) {
        AppPickerDialog(
            apps = apps,
            selected = packages,
            onDismiss = { appPickerOpen = false },
            onConfirm = {
                packages = it
                appPickerOpen = false
            }
        )
    }

    if (datePickerOpen) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { datePickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val iso = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                                .toString()
                            dates = dates + iso
                        }
                        datePickerOpen = false
                    }
                ) {
                    Text(text = "Add", color = AccentFrost)
                }
            },
            dismissButton = {
                TextButton(onClick = { datePickerOpen = false }) {
                    Text(text = "Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}


private fun pickTime(context: Context, initialMinutes: Int, onPick: (Int) -> Unit) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onPick(hour * 60 + minute) },
        initialMinutes / 60,
        initialMinutes % 60,
        DateFormat.is24HourFormat(context)
    ).show()
}

private fun formatTime(minutes: Int): String =
    String.format(Locale.US, "%02d:%02d", (minutes / 60) % 24, minutes % 60)

private fun formatDate(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
}.getOrDefault(iso)

private fun daysSummary(days: Set<Int>): String {
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

private val DAY_OPTIONS = listOf(
    1 to "Mon",
    2 to "Tue",
    3 to "Wed",
    4 to "Thu",
    5 to "Fri",
    6 to "Sat",
    7 to "Sun"
)
