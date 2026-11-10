package com.focusflow.app.ui.screens.schedule

import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusflow.app.data.local.prefs.FocusSchedule
import com.focusflow.app.ui.components.AppCard
import com.focusflow.app.ui.components.PrimaryButton
import com.focusflow.app.ui.components.SectionHeader
import com.focusflow.app.ui.theme.AccentPurple
import com.focusflow.app.ui.theme.BackgroundDark
import com.focusflow.app.ui.theme.DangerRed
import com.focusflow.app.ui.theme.PrimaryPurple
import com.focusflow.app.ui.theme.SurfaceCard
import com.focusflow.app.ui.theme.SurfaceHigh
import com.focusflow.app.ui.theme.TextPrimary
import com.focusflow.app.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val schedules by viewModel.schedules.collectAsState()
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FocusSchedule?>(null) }

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
                Text(
                    text = "Schedules",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }
        }

        item {
            Text(
                text = "Automatically block your chosen apps during weekly focus windows.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        item {
            AppCard {
                Text(
                    text = "Schedules use the apps you've blocked in App Blocker. Keep Blocking switched on for them to work.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentPurple
                )
            }
        }

        item {
            SectionHeader(
                title = if (schedules.isEmpty()) {
                    "Your schedules"
                } else {
                    "Your schedules (${schedules.size})"
                }
            )
        }

        if (schedules.isEmpty()) {
            item {
                AppCard {
                    Text(
                        text = "No schedules yet. Add one to lock distracting apps at the same time every week.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(schedules, key = { it.id }) { schedule ->
                ScheduleRow(
                    schedule = schedule,
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
                text = "Add schedule",
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
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AppCard {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = schedule.label,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = daysSummary(schedule.days),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${formatTime(schedule.startMinutes)} – ${formatTime(schedule.endMinutes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentPurple
                    )
                }
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextPrimary,
                        checkedTrackColor = PrimaryPurple,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceHigh
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Text(text = "Edit", color = AccentPurple)
                }
                TextButton(onClick = onDelete) {
                    Text(text = "Delete", color = DangerRed)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleEditorDialog(
    initial: FocusSchedule?,
    onDismiss: () -> Unit,
    onSave: (FocusSchedule) -> Unit
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf(initial?.label ?: "Focus") }
    var days by remember { mutableStateOf(initial?.days ?: setOf(1, 2, 3, 4, 5)) }
    var startMinutes by remember { mutableIntStateOf(initial?.startMinutes ?: 9 * 60) }
    var endMinutes by remember { mutableIntStateOf(initial?.endMinutes ?: 17 * 60) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(text = if (initial == null) "New schedule" else "Edit schedule")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
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
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = SurfaceHigh,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentPurple,
                        focusedLabelColor = AccentPurple,
                        unfocusedLabelColor = TextSecondary
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                Text(
                    text = "Repeat on",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

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
                                selectedContainerColor = PrimaryPurple,
                                selectedLabelColor = TextPrimary
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            pickTime(context, startMinutes) { startMinutes = it }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Start ${formatTime(startMinutes)}")
                    }
                    OutlinedButton(
                        onClick = {
                            pickTime(context, endMinutes) { endMinutes = it }
                        },
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
                        label.isBlank() -> error = "Give this schedule a name."
                        days.isEmpty() -> error = "Pick at least one day."
                        startMinutes == endMinutes -> error = "Start and end can't be the same."
                        else -> onSave(
                            FocusSchedule(
                                id = initial?.id ?: System.currentTimeMillis(),
                                label = label.trim(),
                                days = days,
                                startMinutes = startMinutes,
                                endMinutes = endMinutes,
                                enabled = initial?.enabled ?: true
                            )
                        )
                    }
                }
            ) {
                Text(text = "Save", color = AccentPurple)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextSecondary)
            }
        }
    )
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

private fun daysSummary(days: Set<Int>): String {
    if (days.isEmpty()) return "No days selected"
    if (days.size == 7) return "Every day"
    return DAY_OPTIONS
        .filter { it.first in days }
        .joinToString(", ") { it.second }
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
