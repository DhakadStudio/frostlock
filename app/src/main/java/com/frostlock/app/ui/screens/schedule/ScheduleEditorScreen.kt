package com.frostlock.app.ui.screens.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.frostlock.app.data.InstalledApp
import com.frostlock.app.data.local.prefs.FocusSchedule
import com.frostlock.app.data.local.prefs.FocusSubject
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.AppPickerDialog
import com.frostlock.app.ui.components.FrostDatePickerDialog
import com.frostlock.app.ui.components.PrimaryButton
import com.frostlock.app.ui.util.RuleOverlap
import com.frostlock.app.ui.util.ruleOverlaps
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.frostlock.app.ui.components.surface.frostPageBackground
import com.frostlock.app.ui.theme.FrostTheme

private enum class TimeField { START, END }

private val WEEKDAYS = setOf(1, 2, 3, 4, 5)

/** Named windows shown as one-tap presets in the rule editor. */
internal data class TimePreset(
    val label: String,
    val startMinutes: Int,
    val endMinutes: Int
)

internal val TIME_PRESETS = listOf(
    TimePreset("Morning", 6 * 60, 9 * 60),
    TimePreset("Workday", 9 * 60, 17 * 60),
    TimePreset("Evening", 18 * 60, 22 * 60),
    TimePreset("Night", 22 * 60, 2 * 60)
)

internal fun matchingTimePreset(startMinutes: Int, endMinutes: Int): TimePreset? =
    TIME_PRESETS.firstOrNull {
        it.startMinutes == startMinutes && it.endMinutes == endMinutes
    }

/**
 * Full-screen rule editor. Rules carry a lot of settings (name, apps, repeat,
 * one-off dates and a time window), so they get a dedicated page instead of a
 * cramped dialog.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleEditorScreen(
    initial: FocusSchedule?,
    apps: List<InstalledApp>,
    subjects: List<FocusSubject>,
    existing: List<FocusSchedule> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (FocusSchedule) -> Unit
) {
    var label by remember { mutableStateOf(initial?.label ?: "Focus") }
    var days by remember { mutableStateOf(initial?.days ?: WEEKDAYS) }
    var dates by remember { mutableStateOf(initial?.dates ?: emptySet()) }
    var fromDate by remember { mutableStateOf(initial?.fromDate) }
    var untilDate by remember { mutableStateOf(initial?.untilDate) }
    var packages by remember { mutableStateOf(initial?.packages ?: emptySet()) }
    var startMinutes by remember { mutableIntStateOf(initial?.startMinutes ?: 9 * 60) }
    var endMinutes by remember { mutableIntStateOf(initial?.endMinutes ?: 17 * 60) }
    var error by remember { mutableStateOf<String?>(null) }
    var appPickerOpen by remember { mutableStateOf(false) }
    var datePickerOpen by remember { mutableStateOf(false) }
    var fromPickerOpen by remember { mutableStateOf(false) }
    var untilPickerOpen by remember { mutableStateOf(false) }
    var timeField by remember { mutableStateOf<TimeField?>(null) }

    val appByPackage = remember(apps) { apps.associateBy { it.packageName } }
    val reusableSubjects = remember(subjects) { subjects.filter { it.hasApps } }
    val draft = remember(initial?.id, label, days, dates, fromDate, untilDate, startMinutes, endMinutes) {
        FocusSchedule(
            id = initial?.id ?: 0L,
            label = label,
            days = if (dates.isEmpty()) days else emptySet(),
            dates = dates,
            fromDate = fromDate,
            untilDate = untilDate,
            startMinutes = startMinutes,
            endMinutes = endMinutes,
            enabled = true
        )
    }
    val overlaps = remember(draft, existing) { ruleOverlaps(draft, existing) }
    val save = {
        val problem = validateRule(label, days, dates, startMinutes, endMinutes, fromDate, untilDate)
        error = problem
        if (problem == null) {
            onSave(
                FocusSchedule(
                    id = initial?.id ?: System.currentTimeMillis(),
                    label = label.trim(),
                    days = if (dates.isEmpty()) days else emptySet(),
                    dates = dates,
                    fromDate = fromDate,
                    untilDate = untilDate,
                    startMinutes = startMinutes,
                    endMinutes = endMinutes,
                    packages = packages,
                    enabled = initial?.enabled ?: true
                )
            )
        }
    }

    BackHandler(onBack = onDismiss)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .frostPageBackground()
            .imePadding()
    ) {
        EditorTopBar(editing = initial != null, onBack = onDismiss)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NameCard(
                label = label,
                onLabelChange = {
                    label = it
                    error = null
                }
            )

            AppsCard(
                packages = packages,
                appByPackage = appByPackage,
                reusableSubjects = reusableSubjects,
                onChooseApps = { appPickerOpen = true },
                onCopySubject = {
                    packages = it
                    error = null
                },
                onRemoveApp = { packages = packages - it }
            )

            RepeatCard(
                days = days,
                dates = dates,
                onDaysChange = {
                    days = it
                    error = null
                },
                onAddDate = { datePickerOpen = true },
                onRemoveDate = { dates = dates - it }
            )

            DateRangeCard(
                fromDate = fromDate,
                untilDate = untilDate,
                onPickFrom = { fromPickerOpen = true },
                onPickUntil = { untilPickerOpen = true },
                onClearFrom = {
                    fromDate = null
                    error = null
                },
                onClearUntil = {
                    untilDate = null
                    error = null
                }
            )

            TimeCard(
                startMinutes = startMinutes,
                endMinutes = endMinutes,
                days = days,
                dates = dates,
                onEditStart = { timeField = TimeField.START },
                onEditEnd = { timeField = TimeField.END },
                onApplyPreset = { start, end ->
                    startMinutes = start
                    endMinutes = end
                    error = null
                }
            )

            if (overlaps.isNotEmpty()) {
                OverlapCard(overlaps = overlaps)
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FrostTheme.colors.danger
                )
            }
            PrimaryButton(
                text = if (initial == null) "Add rule" else "Save changes",
                onClick = save
            )
        }
    }

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
                    Text(text = "Add", color = FrostTheme.colors.secondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { datePickerOpen = false }) {
                    Text(text = "Cancel", color = FrostTheme.colors.textSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (fromPickerOpen) {
        FrostDatePickerDialog(
            initialDate = fromDate ?: LocalDate.now().toString(),
            title = "From date",
            subtitle = "The rule starts on this date",
            confirmLabel = "Set start",
            onDismiss = { fromPickerOpen = false },
            onConfirm = {
                fromDate = it
                fromPickerOpen = false
                error = null
            }
        )
    }

    if (untilPickerOpen) {
        FrostDatePickerDialog(
            initialDate = untilDate ?: fromDate ?: LocalDate.now().toString(),
            title = "Until date",
            subtitle = "The rule stops after this date",
            confirmLabel = "Set end",
            onDismiss = { untilPickerOpen = false },
            onConfirm = {
                untilDate = it
                untilPickerOpen = false
                error = null
            }
        )
    }

    timeField?.let { field ->
        val initialMinutes = if (field == TimeField.START) startMinutes else endMinutes
        FrostTimePickerDialog(
            title = if (field == TimeField.START) "Start time" else "End time",
            initialMinutes = initialMinutes,
            onDismiss = { timeField = null },
            onConfirm = { minutes ->
                if (field == TimeField.START) startMinutes = minutes else endMinutes = minutes
                timeField = null
            }
        )
    }
}

@Composable
private fun EditorTopBar(editing: Boolean, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = FrostTheme.colors.textPrimary
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (editing) "Edit rule" else "New rule",
                style = MaterialTheme.typography.headlineMedium,
                color = FrostTheme.colors.textPrimary
            )
            Text(
                text = "Name, apps, repeat and hours",
                style = MaterialTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary
            )
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(FrostTheme.colors.secondary.copy(alpha = 0.14f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "LOCAL",
                style = MaterialTheme.typography.labelSmall,
                color = FrostTheme.colors.secondary,
                letterSpacing = 0.12.em
            )
        }
    }
}

@Composable
private fun NameCard(label: String, onLabelChange: (String) -> Unit) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(
                icon = Icons.Default.Edit,
                title = "Rule name",
                subtitle = "Only you see this name."
            )
            OutlinedTextField(
                value = label,
                onValueChange = onLabelChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Study block", color = FrostTheme.colors.textSecondary) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FrostTheme.colors.primary,
                    unfocusedBorderColor = FrostTheme.colors.outline,
                    focusedTextColor = FrostTheme.colors.textPrimary,
                    unfocusedTextColor = FrostTheme.colors.textPrimary,
                    cursorColor = FrostTheme.colors.secondary
                ),
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}

@Composable
private fun AppsCard(
    packages: Set<String>,
    appByPackage: Map<String, InstalledApp>,
    reusableSubjects: List<FocusSubject>,
    onChooseApps: () -> Unit,
    onCopySubject: (Set<String>) -> Unit,
    onRemoveApp: (String) -> Unit
) {
    val chosen = packages.take(4).mapNotNull { appByPackage[it] }

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(
                icon = Icons.Default.Lock,
                title = "Apps to block",
                subtitle = when {
                    packages.isEmpty() -> "Falls back to the focus-session app list"
                    packages.size == 1 -> "1 app selected"
                    else -> "${packages.size} apps selected"
                }
            )

            if (packages.isEmpty()) {
                Text(
                    text = "Pick apps to lock while this rule runs, or leave it empty to reuse your focus-session list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    chosen.forEach { app ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppGlyph(app = app, size = 28.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = FrostTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(FrostTheme.colors.surfaceContainerHigh)
                                    .clickable { onRemoveApp(app.packageName) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove ${app.label}",
                                    tint = FrostTheme.colors.textSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                    if (packages.size > chosen.size) {
                        Text(
                            text = "+${packages.size - chosen.size} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = FrostTheme.colors.textSecondary
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onChooseApps,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, FrostTheme.colors.outline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FrostTheme.colors.secondary)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (packages.isEmpty()) "Choose apps" else "Edit selection",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (reusableSubjects.isNotEmpty()) {
                Text(
                    text = "Or copy a subject’s app list",
                    style = MaterialTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reusableSubjects.forEach { subject ->
                        FilterChip(
                            selected = packages == subject.packages,
                            onClick = { onCopySubject(subject.packages) },
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
                                containerColor = FrostTheme.colors.surfaceContainerHigh,
                                labelColor = FrostTheme.colors.textSecondary,
                                selectedContainerColor = FrostTheme.colors.primary,
                                selectedLabelColor = FrostTheme.colors.onPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepeatCard(
    days: Set<Int>,
    dates: Set<String>,
    onDaysChange: (Set<Int>) -> Unit,
    onAddDate: () -> Unit,
    onRemoveDate: (String) -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(
                icon = Icons.Default.CalendarMonth,
                title = "Repeat",
                subtitle = repeatSummary(days, dates)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickPill(
                    text = "Weekdays",
                    selected = dates.isEmpty() && days == WEEKDAYS,
                    modifier = Modifier.weight(1f),
                    onClick = { onDaysChange(WEEKDAYS) }
                )
                QuickPill(
                    text = "Every day",
                    selected = dates.isEmpty() && days.size == 7,
                    modifier = Modifier.weight(1f),
                    onClick = { onDaysChange((1..7).toSet()) }
                )
                QuickPill(
                    text = "Clear",
                    selected = false,
                    modifier = Modifier.weight(1f),
                    onClick = { onDaysChange(emptySet()) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DAY_OPTIONS.forEach { (value, name) ->
                    val selected = value in days
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) {
                                    Brush.linearGradient(FrostTheme.colors.actionGradient)
                                } else {
                                    SolidColor(FrostTheme.colors.surfaceContainerHigh)
                                }
                            )
                            .border(
                                1.dp,
                                if (selected) Color.Transparent else FrostTheme.colors.outline,
                                CircleShape
                            )
                            .clickable {
                                onDaysChange(
                                    if (selected) days - value else days + value
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) FrostTheme.colors.onPrimary else FrostTheme.colors.textSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            if (dates.isNotEmpty()) {
                Text(
                    text = "Date rules run once and ignore the weekdays above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(FrostTheme.colors.outline.copy(alpha = 0.6f))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Specific dates",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textPrimary
                    )
                    Text(
                        text = if (dates.isEmpty()) {
                            "Optional one-off days"
                        } else {
                            "${dates.size} added"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = FrostTheme.colors.textSecondary
                    )
                }
                TextButton(onClick = onAddDate) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Add date", color = FrostTheme.colors.secondary)
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
                            onClick = { onRemoveDate(iso) },
                            label = { Text(text = formatDate(iso)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove date",
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = FrostTheme.colors.surfaceContainerHigh,
                                labelColor = FrostTheme.colors.textPrimary,
                                selectedContainerColor = FrostTheme.colors.primary,
                                selectedLabelColor = FrostTheme.colors.onPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateRangeCard(
    fromDate: String?,
    untilDate: String?,
    onPickFrom: () -> Unit,
    onPickUntil: () -> Unit,
    onClearFrom: () -> Unit,
    onClearUntil: () -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(
                icon = Icons.Default.CalendarMonth,
                title = "Exam window",
                subtitle = when {
                    fromDate == null && untilDate == null -> "Optional: leave open for a permanent rule"
                    else -> "Only runs between these dates"
                }
            )

            RangeDateRow(
                label = "FROM",
                date = fromDate,
                emptyText = "Any start",
                onPick = onPickFrom,
                onClear = onClearFrom
            )

            RangeDateRow(
                label = "UNTIL",
                date = untilDate,
                emptyText = "No end",
                onPick = onPickUntil,
                onClear = onClearUntil
            )

            if (fromDate != null && untilDate != null) {
                Text(
                    text = "Runs ${formatDate(fromDate)} – ${formatDate(untilDate)}, then stops.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun RangeDateRow(
    label: String,
    date: String?,
    emptyText: String,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FrostTheme.colors.surfaceContainerHigh)
            .border(1.dp, FrostTheme.colors.outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onPick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = FrostTheme.colors.textSecondary
            )
            Text(
                text = date?.let { formatDate(it) } ?: emptyText,
                style = MaterialTheme.typography.bodyLarge,
                color = if (date == null) FrostTheme.colors.textSecondary else FrostTheme.colors.textPrimary
            )
        }
        if (date != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear $label date",
                    tint = FrostTheme.colors.textSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = FrostTheme.colors.secondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeCard(
    startMinutes: Int,
    endMinutes: Int,
    days: Set<Int>,
    dates: Set<String>,
    onEditStart: () -> Unit,
    onEditEnd: () -> Unit,
    onApplyPreset: (Int, Int) -> Unit
) {
    val duration = windowDuration(startMinutes, endMinutes)
    val overnight = endMinutes <= startMinutes

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(
                icon = Icons.Default.Schedule,
                title = "Time window",
                subtitle = "${formatTime(startMinutes)} – ${formatTime(endMinutes)}"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeTile(
                    label = "STARTS",
                    minutes = startMinutes,
                    onClick = onEditStart,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = FrostTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                TimeTile(
                    label = "ENDS",
                    minutes = endMinutes,
                    onClick = onEditEnd,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(FrostTheme.colors.surfaceContainerHigh)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildString {
                        append(duration)
                        append(" window")
                        if (overnight) append(" · crosses midnight")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = FrostTheme.colors.secondary
                )
            }

            Text(
                text = "Quick windows",
                style = MaterialTheme.typography.labelSmall,
                color = FrostTheme.colors.textSecondary
            )

            TIME_PRESETS.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { preset ->
                        PresetTile(
                            preset = preset,
                            selected = matchingTimePreset(startMinutes, endMinutes) == preset,
                            onClick = {
                                onApplyPreset(preset.startMinutes, preset.endMinutes)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Text(
                text = repeatSummary(days, dates) +
                    " · tap a time to change it. Overnight windows like 22:00 to 02:00 work too.",
                style = MaterialTheme.typography.bodySmall,
                color = FrostTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun OverlapCard(overlaps: List<RuleOverlap>) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = FrostTheme.colors.warning,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Overlaps another rule",
                    style = MaterialTheme.typography.titleMedium,
                    color = FrostTheme.colors.textPrimary
                )
            }
            Text(
                text = if (overlaps.size == 1) {
                    "This window blocks at the same time as:"
                } else {
                    "This window blocks at the same time as ${overlaps.size} other rules:"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary
            )
            overlaps.forEach { overlap ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "“${overlap.other.label}”",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FrostTheme.colors.warning,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = overlapDetail(overlap),
                        style = MaterialTheme.typography.labelSmall,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }
            Text(
                text = "Both rules stay active — this is only a heads-up so you can tell them apart.",
                style = MaterialTheme.typography.labelSmall,
                color = FrostTheme.colors.textSecondary
            )
        }
    }
}

private fun overlapDetail(overlap: RuleOverlap): String = when {
    overlap.dates.isNotEmpty() ->
        overlap.dates.sorted().joinToString(", ") { formatDate(it) }
    overlap.days.isNotEmpty() -> daysSummary(overlap.days)
    else -> ""
}

@Composable
private fun PresetTile(
    preset: TimePreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .clip(shape)
            .background(if (selected) FrostTheme.colors.primary.copy(alpha = 0.16f) else FrostTheme.colors.surfaceContainerHigh)
            .border(1.dp, if (selected) FrostTheme.colors.primary else FrostTheme.colors.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = preset.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) FrostTheme.colors.secondary else FrostTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = FrostTheme.colors.secondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Text(
            text = "${formatTime(preset.startMinutes)} – ${formatTime(preset.endMinutes)}",
            style = MaterialTheme.typography.bodySmall,
            color = FrostTheme.colors.textSecondary
        )
    }
}

@Composable
private fun TimeTile(
    label: String,
    minutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(FrostTheme.colors.surfaceContainerHigh)
            .border(1.dp, FrostTheme.colors.outline, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = FrostTheme.colors.secondary,
                letterSpacing = 0.12.em,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = FrostTheme.colors.secondary,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatTime(minutes),
            style = MaterialTheme.typography.headlineMedium,
            color = FrostTheme.colors.textPrimary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrostTimePickerDialog(
    title: String,
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(FrostTheme.colors.surfaceContainer)
                .border(1.dp, FrostTheme.colors.outline, RoundedCornerShape(28.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(FrostTheme.colors.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = FrostTheme.colors.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = FrostTheme.colors.textPrimary
                    )
                    Text(
                        text = "24-hour · tap the dial or type",
                        style = MaterialTheme.typography.bodySmall,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TimePicker(
                state = state,
                layoutType = TimePickerLayoutType.Vertical,
                colors = TimePickerDefaults.colors(
                    clockDialColor = FrostTheme.colors.surfaceContainerHigh,
                    clockDialSelectedContentColor = FrostTheme.colors.onPrimary,
                    clockDialUnselectedContentColor = FrostTheme.colors.textPrimary,
                    selectorColor = FrostTheme.colors.primary,
                    containerColor = Color.Transparent,
                    timeSelectorSelectedContainerColor = FrostTheme.colors.primary,
                    timeSelectorSelectedContentColor = FrostTheme.colors.onPrimary,
                    timeSelectorUnselectedContainerColor = FrostTheme.colors.surfaceContainerHigh,
                    timeSelectorUnselectedContentColor = FrostTheme.colors.textSecondary
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Cancel", color = FrostTheme.colors.textSecondary)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(FrostTheme.colors.actionGradient))
                        .clickable { onConfirm(state.hour * 60 + state.minute) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Set ${formatTime(state.hour * 60 + state.minute)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = FrostTheme.colors.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(FrostTheme.colors.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FrostTheme.colors.secondary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = FrostTheme.colors.textPrimary
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun QuickPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 36.dp)
            .clip(CircleShape)
            .background(
                if (selected) {
                    Brush.linearGradient(FrostTheme.colors.actionGradient)
                } else {
                    SolidColor(FrostTheme.colors.surfaceContainerHigh)
                }
            )
            .border(
                1.dp,
                if (selected) Color.Transparent else FrostTheme.colors.outline,
                CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) FrostTheme.colors.onPrimary else FrostTheme.colors.textSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun AppGlyph(app: InstalledApp, size: androidx.compose.ui.unit.Dp) {
    if (app.icon != null) {
        Image(
            bitmap = app.icon,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .background(FrostTheme.colors.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.label.take(1).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = FrostTheme.colors.secondary
            )
        }
    }
}

internal fun repeatSummary(days: Set<Int>, dates: Set<String>): String = when {
    dates.isNotEmpty() -> if (dates.size == 1) "Once, on 1 date" else "Once, on ${dates.size} dates"
    days.isEmpty() -> "No days selected"
    days.size == 7 -> "Every day"
    days == WEEKDAYS -> "Weekdays"
    else -> daysSummary(days)
}

internal fun validateRule(
    label: String,
    days: Set<Int>,
    dates: Set<String>,
    startMinutes: Int,
    endMinutes: Int,
    fromDate: String? = null,
    untilDate: String? = null
): String? = when {
    label.isBlank() -> "Give this rule a name."
    days.isEmpty() && dates.isEmpty() -> "Pick at least one day or date."
    startMinutes == endMinutes -> "Start and end can't be the same."
    fromDate != null && untilDate != null && untilDate < fromDate ->
        "The end date can't be before the start date."
    else -> null
}

internal fun windowDuration(startMinutes: Int, endMinutes: Int): String {
    val total = if (endMinutes > startMinutes) {
        endMinutes - startMinutes
    } else {
        24 * 60 - startMinutes + endMinutes
    }
    val hours = total / 60
    val minutes = total % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}
