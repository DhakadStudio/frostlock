package com.frostlock.app.ui.screens.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.ui.components.SessionPhotoEditor
import com.frostlock.app.ui.components.controls.FrostButton
import com.frostlock.app.ui.components.controls.FrostButtonVariant
import com.frostlock.app.ui.components.surface.FrostCard
import com.frostlock.app.ui.components.surface.HeroCard
import com.frostlock.app.ui.shell.FrostDetailScaffold
import com.frostlock.app.ui.theme.FrostTheme
import com.frostlock.app.ui.util.buildSessionSummary
import com.frostlock.app.ui.util.formatDuration

/**
 * Closes the loop after a session: planned vs actual, the subject and
 * intention, the streak, a note and an optional photo.
 *
 * This replaces the old reflection dialog and is the only place a session is
 * saved or discarded.
 */
@Composable
fun SessionSummaryScreen(
    onDone: () -> Unit,
    continues: Boolean = false,
    viewModel: SessionSummaryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var confirmDiscard by remember { mutableStateOf(false) }

    LaunchedEffect(state.finished) {
        if (state.finished) onDone()
    }

    FrostDetailScaffold(
        title = "Session summary",
        onBack = viewModel::done
    ) { padding ->
        when {
            state.loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
            state.session == null -> MissingSummary(
                padding = padding,
                onBack = viewModel::done
            )
            else -> SummaryContent(
                session = requireNotNull(state.session),
                streakDays = state.streakDays,
                note = state.note,
                busy = state.busy,
                padding = padding,
                onNoteChange = viewModel::onNoteChange,
                onPickPhoto = viewModel::attachPhoto,
                onRemovePhoto = viewModel::removePhoto,
                continues = continues,
                onSaveAndEnd = viewModel::saveAndEnd,
                onDone = viewModel::done,
                onDiscard = { confirmDiscard = true }
            )
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            containerColor = FrostTheme.colors.surfaceContainer,
            title = {
                Text(
                    text = "Discard this session?",
                    style = FrostTheme.typography.headlineMedium,
                    color = FrostTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "It will leave no trace in your history or stats. " +
                        "Your intention stays for the next attempt.",
                    style = FrostTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    viewModel.discard()
                }) {
                    Text(
                        text = "Discard",
                        style = FrostTheme.typography.labelLarge,
                        color = FrostTheme.colors.danger
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(
                        text = "Keep",
                        style = FrostTheme.typography.labelLarge,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }
        )
    }
}

@Composable
private fun SummaryContent(
    session: SessionEntity,
    streakDays: Int,
    note: String,
    busy: Boolean,
    padding: PaddingValues,
    onNoteChange: (String) -> Unit,
    onPickPhoto: (android.net.Uri) -> Unit,
    onRemovePhoto: () -> Unit,
    continues: Boolean,
    onSaveAndEnd: () -> Unit,
    onDone: () -> Unit,
    onDiscard: () -> Unit
) {
    val summary = buildSessionSummary(session, streakDays)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(
            start = FrostTheme.spacing.screenHorizontal,
            end = FrostTheme.spacing.screenHorizontal,
            top = FrostTheme.spacing.sm,
            bottom = FrostTheme.spacing.huge
        ),
        verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.md)
    ) {
        item {
            HeroCard(contentPadding = PaddingValues(FrostTheme.spacing.xxl)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = FrostTheme.colors.success
                    )
                    Spacer(modifier = Modifier.padding(start = FrostTheme.spacing.sm))
                    Text(
                        text = "Session saved",
                        style = FrostTheme.typography.labelLarge,
                        color = FrostTheme.colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
                Text(
                    text = "${formatDuration(summary.focusedSeconds.toLong())} focused",
                    style = FrostTheme.typography.headlineLarge,
                    color = FrostTheme.colors.textPrimary
                )
                Text(
                    text = if (summary.completed) {
                        "You finished the whole round."
                    } else {
                        "You stopped early — the time still counts."
                    },
                    style = FrostTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
            }
        }

        item {
            FrostCard {
                Text(
                    text = "Planned vs actual",
                    style = FrostTheme.typography.titleMedium,
                    color = FrostTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
                SummaryRow("Planned", summary.plannedLabel)
                SummaryRow("Focused", formatDuration(summary.focusedSeconds.toLong()))
                SummaryRow("Mode", summary.modeLabel)
                SummaryRow("Outcome", summary.outcomeLabel)
            }
        }

        item {
            FrostCard {
                Text(
                    text = "Subject and intention",
                    style = FrostTheme.typography.titleMedium,
                    color = FrostTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
                SummaryRow("Subject", summary.subjectLabel)
                if (session.intent.isNotBlank()) {
                    Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
                    Text(
                        text = "“${session.intent}”",
                        style = FrostTheme.typography.bodyLarge,
                        color = FrostTheme.colors.textSecondary
                    )
                } else {
                    SummaryRow("Intention", "Not set")
                }
            }
        }

        item {
            FrostCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = FrostTheme.colors.secondary
                    )
                    Spacer(modifier = Modifier.padding(start = FrostTheme.spacing.sm))
                    Text(
                        text = streakLabel(streakDays),
                        style = FrostTheme.typography.titleMedium,
                        color = FrostTheme.colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
                Text(
                    text = "Every saved session counts toward your streak.",
                    style = FrostTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
            }
        }

        item {
            FrostCard {
                Text(
                    text = "Reflection",
                    style = FrostTheme.typography.titleMedium,
                    color = FrostTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "One line about what you moved forward.",
                            color = FrostTheme.colors.textSecondary
                        )
                    },
                    supportingText = {
                        Text(
                            text = "${note.length}/${SessionEntity.MAX_NOTE_LENGTH}",
                            color = FrostTheme.colors.textSecondary
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FrostTheme.colors.primary,
                        unfocusedBorderColor = FrostTheme.colors.outline,
                        focusedTextColor = FrostTheme.colors.textPrimary,
                        unfocusedTextColor = FrostTheme.colors.textPrimary,
                        cursorColor = FrostTheme.colors.primary,
                        focusedLabelColor = FrostTheme.colors.primary,
                        unfocusedLabelColor = FrostTheme.colors.textSecondary
                    )
                )
                Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
                SessionPhotoEditor(
                    name = session.photoPath,
                    onPick = onPickPhoto,
                    onRemove = onRemovePhoto
                )
            }
        }

        item {
            // A Pomodoro round can finish while the session continues into a
            // break; there "Done" saves the note and keeps the intention for
            // the next round instead of claiming the session ended.
            FrostButton(
                text = if (continues) "Done" else "Save and end",
                onClick = if (continues) onDone else onSaveAndEnd,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            FrostButton(
                text = "Discard session",
                onClick = onDiscard,
                enabled = !busy,
                variant = FrostButtonVariant.GHOST,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Text(
                text = "Saved only on this device.",
                style = FrostTheme.typography.bodySmall,
                color = FrostTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FrostTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = FrostTheme.typography.bodyMedium,
            color = FrostTheme.colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = FrostTheme.typography.bodyMedium,
            color = FrostTheme.colors.textPrimary
        )
    }
}

@Composable
private fun MissingSummary(padding: PaddingValues, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(FrostTheme.spacing.screenHorizontal),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "This session is no longer available.",
            style = FrostTheme.typography.titleLarge,
            color = FrostTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
        Text(
            text = "It may have been discarded from another screen.",
            style = FrostTheme.typography.bodyMedium,
            color = FrostTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(FrostTheme.spacing.xl))
        FrostButton(
            text = "Back to Focus",
            onClick = onBack,
            variant = FrostButtonVariant.SECONDARY,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

internal fun streakLabel(days: Int): String = when {
    days <= 0 -> "A fresh start"
    days == 1 -> "Day 1 of your streak"
    else -> "Day $days of your streak"
}
