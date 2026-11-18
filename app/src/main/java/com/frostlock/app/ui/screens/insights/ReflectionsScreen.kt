package com.frostlock.app.ui.screens.insights

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.ui.components.SessionPhotoEditor
import com.frostlock.app.ui.components.SessionPhotoThumbnail
import com.frostlock.app.ui.components.SessionPhotoViewer
import com.frostlock.app.ui.components.controls.FrostButton
import com.frostlock.app.ui.components.controls.FrostSearchField
import com.frostlock.app.ui.components.surface.FrostCard
import com.frostlock.app.ui.shell.FrostDetailScaffold
import com.frostlock.app.ui.theme.FrostTheme
import com.frostlock.app.ui.util.DiaryDay
import com.frostlock.app.ui.util.formatDuration
import com.frostlock.app.ui.util.formatSessionTime
import com.frostlock.app.ui.util.modeIcon
import com.frostlock.app.ui.util.modeLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Reflections: the qualitative archive.
 *
 * Only sessions with a note or photo appear here. The full session list lives
 * in History, so this page never becomes a second analytics screen.
 */
@Composable
fun ReflectionsScreen(
    onBack: () -> Unit,
    onStartFocus: () -> Unit,
    viewModel: ReflectionsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var editingId by remember { mutableStateOf<Long?>(null) }

    FrostDetailScaffold(title = "Reflections", onBack = onBack) { padding ->
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
                Text(
                    text = "One line after each session, kept on this device.",
                    style = FrostTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
            }

            item {
                ReflectionsHero(
                    noteCount = state.summary.noteCount,
                    dayCount = state.summary.dayCount
                )
            }

            item {
                FrostSearchField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    placeholder = "Search reflections"
                )
            }

            if (state.isEmpty) {
                item {
                    EmptyReflections(
                        hasAnySessions = state.hasAnySessions,
                        isSearching = state.query.isNotBlank(),
                        onStartFocus = onStartFocus
                    )
                }
            } else {
                state.days.forEach { day ->
                    item(key = "day-${day.date}") { DayHeader(day = day) }
                    items(day.sessions, key = { "reflection-${it.id}" }) { session ->
                        ReflectionCard(
                            session = session,
                            onEdit = { editingId = session.id }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Reflections never leave this device.",
                    style = FrostTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = FrostTheme.spacing.lg)
                )
            }
        }
    }

    val editingSession = editingId?.let { id ->
        state.days.firstNotNullOfOrNull { day -> day.sessions.firstOrNull { it.id == id } }
    }
    editingSession?.let { session ->
        NoteEditorDialog(
            subject = session.tag.trim().ifBlank { modeLabel(session.mode) },
            initialNote = session.note,
            photoName = session.photoPath,
            onSave = { note ->
                viewModel.updateNote(session.id, note)
                editingId = null
            },
            onPickPhoto = { uri -> viewModel.attachPhoto(session.id, uri) },
            onRemovePhoto = { viewModel.removePhoto(session.id) },
            onDismiss = { editingId = null }
        )
    }
}

@Composable
private fun ReflectionsHero(noteCount: Int, dayCount: Int) {
    FrostCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(FrostTheme.colors.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = FrostTheme.colors.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(FrostTheme.spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (noteCount == 1) "1 reflection" else "$noteCount reflections",
                    style = FrostTheme.typography.titleLarge,
                    color = FrostTheme.colors.textPrimary
                )
                Text(
                    text = if (dayCount == 0) {
                        "One line after each session"
                    } else {
                        "Across $dayCount ${if (dayCount == 1) "day" else "days"} · on this device"
                    },
                    style = FrostTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun DayHeader(day: DiaryDay) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = FrostTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = diaryDateLabel(day.date),
            style = FrostTheme.typography.titleMedium,
            color = FrostTheme.colors.secondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${day.sessions.size} · ${formatDuration(day.totalSeconds)}",
            style = FrostTheme.typography.bodySmall,
            color = FrostTheme.colors.textSecondary
        )
    }
}

@Composable
private fun ReflectionCard(session: SessionEntity, onEdit: () -> Unit) {
    var viewingPhoto by remember { mutableStateOf(false) }

    FrostCard(onClick = onEdit) {
        Column(verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(FrostTheme.colors.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = modeIcon(session.mode),
                        contentDescription = null,
                        tint = FrostTheme.colors.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(FrostTheme.spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.tag.trim().ifBlank { modeLabel(session.mode) },
                        style = FrostTheme.typography.titleMedium,
                        color = FrostTheme.colors.textPrimary
                    )
                    Text(
                        text = "${formatSessionTime(session.endedAt)} · ${modeLabel(session.mode)}",
                        style = FrostTheme.typography.bodySmall,
                        color = FrostTheme.colors.textSecondary
                    )
                }
                Text(
                    text = formatDuration(session.durationSeconds.toLong()),
                    style = FrostTheme.typography.titleMedium,
                    color = FrostTheme.colors.textPrimary
                )
            }

            if (session.photoPath.isNotBlank()) {
                SessionPhotoThumbnail(
                    name = session.photoPath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    corner = 14.dp,
                    onClick = { viewingPhoto = true }
                )
            }

            if (session.note.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(FrostTheme.colors.surfaceContainerHigh.copy(alpha = 0.65f))
                        .padding(horizontal = FrostTheme.spacing.md, vertical = FrostTheme.spacing.sm)
                ) {
                    Text(
                        text = "“${session.note}”",
                        style = FrostTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        color = FrostTheme.colors.textPrimary,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.EditNote,
                        contentDescription = null,
                        tint = FrostTheme.colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(FrostTheme.spacing.xs))
                    Text(
                        text = "Photo only — tap to add a reflection",
                        style = FrostTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }
        }
    }

    if (viewingPhoto) {
        SessionPhotoViewer(
            name = session.photoPath,
            onDismiss = { viewingPhoto = false }
        )
    }
}

@Composable
private fun EmptyReflections(
    hasAnySessions: Boolean,
    isSearching: Boolean,
    onStartFocus: () -> Unit
) {
    FrostCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = FrostTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(FrostTheme.colors.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AcUnit,
                    contentDescription = null,
                    tint = FrostTheme.colors.secondary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = when {
                    isSearching -> "No reflections match that search."
                    hasAnySessions -> "No reflections yet."
                    else -> "Your reflections start with one session."
                },
                style = FrostTheme.typography.titleMedium,
                color = FrostTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = when {
                    isSearching -> "Try a different word."
                    hasAnySessions ->
                        "Finish a session and write one line about what you learned."
                    else ->
                        "Pick a subject, focus, then write one line about what you moved forward."
                },
                style = FrostTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
            if (!hasAnySessions) {
                FrostButton(
                    text = "Start a focus session",
                    onClick = onStartFocus,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun NoteEditorDialog(
    subject: String,
    initialNote: String,
    photoName: String,
    onSave: (String) -> Unit,
    onPickPhoto: (android.net.Uri) -> Unit,
    onRemovePhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf(initialNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FrostTheme.colors.surfaceContainer,
        titleContentColor = FrostTheme.colors.textPrimary,
        textContentColor = FrostTheme.colors.textSecondary,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.xs)) {
                Text(
                    text = if (initialNote.isBlank()) "Add a reflection" else "Edit reflection",
                    style = FrostTheme.typography.titleLarge
                )
                Text(
                    text = subject,
                    style = FrostTheme.typography.bodyMedium,
                    color = FrostTheme.colors.secondary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.md)) {
                SessionPhotoEditor(
                    name = photoName,
                    onPick = onPickPhoto,
                    onRemove = onRemovePhoto
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        if (it.length <= SessionEntity.MAX_NOTE_LENGTH) note = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "What did you learn or finish?",
                            color = FrostTheme.colors.textSecondary
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FrostTheme.colors.primary,
                        unfocusedBorderColor = FrostTheme.colors.outline,
                        focusedTextColor = FrostTheme.colors.textPrimary,
                        unfocusedTextColor = FrostTheme.colors.textPrimary,
                        cursorColor = FrostTheme.colors.secondary
                    )
                )
                Text(
                    text = "${note.length}/${SessionEntity.MAX_NOTE_LENGTH} · stays on this device",
                    style = FrostTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(note) }) {
                Text(text = "Save", color = FrostTheme.colors.secondary)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(FrostTheme.spacing.xs)) {
                if (initialNote.isNotBlank()) {
                    TextButton(onClick = { onSave("") }) {
                        Text(text = "Clear", color = FrostTheme.colors.danger)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(text = "Cancel", color = FrostTheme.colors.textSecondary)
                }
            }
        }
    )
}

private fun diaryDateLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.US))
    }
}
