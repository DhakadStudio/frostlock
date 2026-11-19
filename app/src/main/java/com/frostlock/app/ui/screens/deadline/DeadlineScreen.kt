package com.frostlock.app.ui.screens.deadline

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.data.local.prefs.FocusDeadline
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.FrostDatePickerDialog
import com.frostlock.app.ui.components.PrimaryButton
import com.frostlock.app.ui.components.SectionHeader
import com.frostlock.app.ui.util.deadlineCountdown
import com.frostlock.app.ui.util.deadlineIsUrgent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.frostlock.app.ui.components.surface.frostPageBackground
import com.frostlock.app.ui.theme.FrostTheme

/**
 * A full page for the local deadline countdown. One name, one date, and a
 * preview of what the home card will say.
 */
@Composable
fun DeadlineScreen(
    onBack: () -> Unit,
    viewModel: DeadlineViewModel = hiltViewModel()
) {
    val saved by viewModel.deadline.collectAsState()
    val today = remember { LocalDate.now() }

    var title by remember(saved) { mutableStateOf(saved?.title.orEmpty()) }
    var date by remember(saved) {
        mutableStateOf(
            saved?.date ?: today.plusDays(7).toString()
        )
    }
    var pickerOpen by remember { mutableStateOf(false) }

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
                    text = "Focus deadline",
                    style = MaterialTheme.typography.headlineMedium,
                    color = FrostTheme.colors.textPrimary
                )
                Text(
                    text = "A local countdown to what matters",
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
            PreviewCard(
                title = title,
                date = date,
                today = today
            )

            SectionHeader(title = "Target")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(FrostTheme.colors.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = FrostTheme.colors.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "What are you counting down to?",
                                style = MaterialTheme.typography.titleMedium,
                                color = FrostTheme.colors.textPrimary
                            )
                            Text(
                                text = "An exam, a launch, a personal goal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = FrostTheme.colors.textSecondary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            if (it.length <= FocusDeadline.MAX_TITLE_LENGTH) title = it
                        },
                        modifier = Modifier.fillMaxWidth(),
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
                }
            }

            SectionHeader(title = "Date")
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
                                text = "Target date",
                                style = MaterialTheme.typography.labelSmall,
                                color = FrostTheme.colors.textSecondary
                            )
                            Text(
                                text = formatDeadlineDate(date),
                                style = MaterialTheme.typography.titleMedium,
                                color = FrostTheme.colors.textPrimary
                            )
                        }
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelLarge,
                            color = FrostTheme.colors.secondary
                        )
                    }

                    Text(
                        text = "The countdown is stored only on this device. Nothing is uploaded and no calendar permission is needed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }

            if (saved != null) {
                TextButton(
                    onClick = {
                        viewModel.clear()
                        onBack()
                    }
                ) {
                    Text(text = "Clear deadline", color = FrostTheme.colors.danger)
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
                text = if (saved == null) "Set deadline" else "Save deadline",
                onClick = {
                    if (title.isNotBlank()) {
                        viewModel.save(title, date)
                        onBack()
                    }
                }
            )
        }
    }

    if (pickerOpen) {
        FrostDatePickerDialog(
            initialDate = date,
            onDismiss = { pickerOpen = false },
            onConfirm = { picked ->
                date = picked
                pickerOpen = false
            }
        )
    }
}

@Composable
private fun PreviewCard(title: String, date: String, today: LocalDate) {
    val preview = FocusDeadline(
        title = title.ifBlank { "Your deadline" },
        date = date
    )
    val countdown = deadlineCountdown(preview, today)
    val urgent = deadlineIsUrgent(preview, today)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.verticalGradient(
                    FrostTheme.colors.heroGradient
                )
            )
            .border(1.dp, FrostTheme.colors.outline, MaterialTheme.shapes.large)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(FrostTheme.colors.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = if (urgent) FrostTheme.colors.warning else FrostTheme.colors.secondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "COUNTDOWN",
                    style = MaterialTheme.typography.labelSmall,
                    color = FrostTheme.colors.textSecondary
                )
            }
            Text(
                text = countdown ?: "Pick a date",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = if (urgent) FrostTheme.colors.warning else FrostTheme.colors.textPrimary
            )
            Text(
                text = preview.title,
                style = MaterialTheme.typography.titleLarge,
                color = FrostTheme.colors.textPrimary
            )
            Text(
                text = formatDeadlineDate(date),
                style = MaterialTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary
            )
        }
    }
}

private fun formatDeadlineDate(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("EEE, d MMMM yyyy", Locale.US))
}.getOrDefault(iso)
