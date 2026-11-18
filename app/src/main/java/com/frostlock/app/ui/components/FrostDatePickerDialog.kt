package com.frostlock.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Themed 24-hour date dialog shared by the deadline and focus-goal editors.
 * Returns the picked date as an ISO `yyyy-MM-dd` string.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrostDatePickerDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    title: String = "Target date",
    subtitle: String = "The countdown updates instantly",
    confirmLabel: String = "Set date"
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = toUtcMillis(initialDate)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(FrostTheme.colors.surfaceContainer)
                .border(1.dp, FrostTheme.colors.outline, RoundedCornerShape(28.dp))
                .padding(16.dp)
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
                        imageVector = Icons.Default.CalendarMonth,
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
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            DatePicker(
                state = state,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.Transparent,
                    titleContentColor = FrostTheme.colors.textPrimary,
                    headlineContentColor = FrostTheme.colors.textPrimary,
                    weekdayContentColor = FrostTheme.colors.textSecondary,
                    subheadContentColor = FrostTheme.colors.textSecondary,
                    navigationContentColor = FrostTheme.colors.textSecondary,
                    yearContentColor = FrostTheme.colors.textPrimary,
                    currentYearContentColor = FrostTheme.colors.secondary,
                    selectedYearContentColor = FrostTheme.colors.onPrimary,
                    selectedYearContainerColor = FrostTheme.colors.primary,
                    dayContentColor = FrostTheme.colors.textPrimary,
                    selectedDayContentColor = FrostTheme.colors.onPrimary,
                    selectedDayContainerColor = FrostTheme.colors.primary,
                    todayContentColor = FrostTheme.colors.secondary,
                    todayDateBorderColor = FrostTheme.colors.secondary,
                    dividerColor = FrostTheme.colors.outline
                )
            )

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
                        .clickable {
                            state.selectedDateMillis?.let { millis ->
                                onConfirm(fromUtcMillis(millis).toString())
                            } ?: onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = confirmLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = FrostTheme.colors.onPrimary
                    )
                }
            }
        }
    }
}

private fun toUtcMillis(iso: String): Long = runCatching {
    LocalDate.parse(iso)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
}.getOrElse { System.currentTimeMillis() }

private fun fromUtcMillis(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
