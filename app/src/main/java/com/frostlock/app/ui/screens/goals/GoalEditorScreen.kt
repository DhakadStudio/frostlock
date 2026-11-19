package com.frostlock.app.ui.screens.goals

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.data.local.prefs.FocusGoal
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.FrostDatePickerDialog
import com.frostlock.app.ui.components.PrimaryButton
import com.frostlock.app.ui.components.SectionHeader
import com.frostlock.app.ui.util.goalMinutesLabel
import com.frostlock.app.ui.util.goalTargetLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.frostlock.app.ui.components.surface.frostPageBackground
import com.frostlock.app.ui.theme.FrostTheme

/** Create or edit one local focus goal. */
@Composable
fun GoalEditorScreen(
    onBack: () -> Unit,
    viewModel: GoalEditorViewModel = hiltViewModel()
) {
    val saved by viewModel.goal.collectAsState()
    val today = remember { LocalDate.now() }

    var name by remember(saved) { mutableStateOf(saved?.name.orEmpty()) }
    var date by remember(saved) {
        mutableStateOf(saved?.targetDate ?: today.plusDays(30).toString())
    }
    var hoursText by remember(saved) {
        mutableStateOf(((saved?.targetMinutes ?: DEFAULT_HOURS * 60) / 60).toString())
    }
    var pickerOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .frostPageBackground()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = FrostTheme.colors.textPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (viewModel.isEditing) "Edit goal" else "New focus goal",
                    style = MaterialTheme.typography.headlineMedium,
                    color = FrostTheme.colors.textPrimary
                )
                Text(
                    text = "Stored only on this device",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GoalPreview(
                name = name.ifBlank { "Your goal" },
                date = date,
                hours = hoursText.toIntOrNull() ?: DEFAULT_HOURS
            )

            SectionHeader(title = "Goal")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            if (it.length <= FocusGoal.MAX_NAME_LENGTH) {
                                name = it
                                error = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Name", color = FrostTheme.colors.textSecondary) },
                        placeholder = { Text("e.g. Final exams", color = FrostTheme.colors.textSecondary) },
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

                    Text(
                        text = "Target focus time",
                        style = MaterialTheme.typography.labelMedium,
                        color = FrostTheme.colors.textSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 25, 50, 100).forEach { preset ->
                            HoursChip(
                                hours = preset,
                                selected = hoursText.toIntOrNull() == preset,
                                onClick = {
                                    hoursText = preset.toString()
                                    error = null
                                }
                            )
                        }
                    }

                    Text(
                        text = "Custom hours",
                        style = MaterialTheme.typography.labelMedium,
                        color = FrostTheme.colors.textSecondary
                    )
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = {
                            if (it.length <= 3 && it.all(Char::isDigit)) {
                                hoursText = it
                                error = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("10", color = FrostTheme.colors.textSecondary) },
                        suffix = { Text("hours", color = FrostTheme.colors.textSecondary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

            SectionHeader(title = "Target date")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(FrostTheme.colors.surfaceContainerHigh)
                            .clickable { pickerOpen = true }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = FrostTheme.colors.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formatGoalDate(date),
                                style = MaterialTheme.typography.titleMedium,
                                color = FrostTheme.colors.textPrimary
                            )
                            Text(
                                text = goalTargetLabel(date, today),
                                style = MaterialTheme.typography.bodySmall,
                                color = FrostTheme.colors.textSecondary
                            )
                        }
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelLarge,
                            color = FrostTheme.colors.secondary
                        )
                    }
                    Text(
                        text = "Only sessions finished before this date count toward " +
                            "the goal. No calendar permission is needed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }

            error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = FrostTheme.colors.danger
                )
            }

            if (viewModel.isEditing) {
                TextButton(onClick = { confirmDelete = true }) {
                    Text(text = "Delete goal", color = FrostTheme.colors.danger)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            PrimaryButton(
                text = if (viewModel.isEditing) "Save goal" else "Set goal",
                onClick = {
                    val hours = hoursText.toIntOrNull()
                    when {
                        name.isBlank() -> error = "Give the goal a name."
                        hours == null || hours < 1 || hours > 500 ->
                            error = "Enter a target between 1 and 500 hours."
                        else -> {
                            viewModel.save(name, date, hours * 60)
                            onBack()
                        }
                    }
                }
            )
        }
    }

    if (pickerOpen) {
        FrostDatePickerDialog(
            initialDate = date,
            title = "Goal date",
            subtitle = "Sessions count until this day ends",
            confirmLabel = "Set date",
            onDismiss = { pickerOpen = false },
            onConfirm = { picked ->
                date = picked
                pickerOpen = false
            }
        )
    }

    if (confirmDelete) {
        DeleteGoalDialog(
            name = saved?.name ?: "this goal",
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                viewModel.remove()
                onBack()
            }
        )
    }
}

@Composable
private fun GoalPreview(name: String, date: String, hours: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(FrostTheme.colors.heroGradient))
            .border(1.dp, FrostTheme.colors.outline, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(FrostTheme.colors.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = FrostTheme.colors.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "GOAL PREVIEW",
                style = MaterialTheme.typography.labelSmall,
                color = FrostTheme.colors.textSecondary
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            color = FrostTheme.colors.textPrimary
        )
        Text(
            text = "${goalMinutesLabel(hours.coerceAtLeast(1) * 60L)} · ${goalTargetLabel(date)}",
            style = MaterialTheme.typography.bodyMedium,
            color = FrostTheme.colors.secondary
        )
    }
}

@Composable
private fun HoursChip(hours: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) FrostTheme.colors.primary else FrostTheme.colors.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "${hours}h",
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) FrostTheme.colors.onPrimary else FrostTheme.colors.textSecondary
        )
    }
}

@Composable
private fun DeleteGoalDialog(
    name: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Delete goal?",
                style = MaterialTheme.typography.headlineMedium,
                color = FrostTheme.colors.textPrimary
            )
            Text(
                text = "\"$name\" will be removed from this device. Your focus " +
                    "sessions stay in the diary and stats.",
                style = MaterialTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(text = "Keep it", color = FrostTheme.colors.textSecondary)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(CircleShape)
                        .background(FrostTheme.colors.danger)
                        .clickable(onClick = onConfirm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelLarge,
                        color = FrostTheme.colors.textPrimary
                    )
                }
            }
        }
    }
}

private const val DEFAULT_HOURS = 10

private fun formatGoalDate(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("EEE, d MMMM yyyy", Locale.US))
}.getOrDefault(iso)
