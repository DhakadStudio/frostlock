package com.frostlock.app.ui.screens.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.MoodChip
import com.frostlock.app.ui.components.PrimaryButton
import com.frostlock.app.ui.components.SessionPhotoEditor
import com.frostlock.app.ui.components.SessionPhotoThumbnail
import com.frostlock.app.ui.components.SessionPhotoViewer
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
import com.frostlock.app.ui.util.DiaryDay
import com.frostlock.app.ui.util.formatDuration
import com.frostlock.app.ui.util.formatSessionTime
import com.frostlock.app.ui.util.modeIcon
import com.frostlock.app.ui.util.modeLabel
import com.frostlock.app.ui.util.stampsFor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The study diary: every reflection written after a session, grouped by the day
 * it was finished. Notes can be rewritten in place, and silent sessions can be
 * filled in later from the "All sessions" view.
 */
@Composable
fun DiaryScreen(
    onBack: () -> Unit,
    onStartFocus: () -> Unit,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var editingId by remember { mutableStateOf<Long?>(null) }

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
                        text = "Study diary",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Reflections from your focus sessions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        item { DiaryHero(noteCount = state.summary.noteCount, dayCount = state.summary.dayCount, focusedSeconds = state.summary.focusedSeconds) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MoodChip(
                    text = "With notes",
                    selected = state.onlyWithNotes,
                    onClick = { viewModel.setOnlyWithNotes(true) }
                )
                MoodChip(
                    text = "All sessions",
                    selected = !state.onlyWithNotes,
                    onClick = { viewModel.setOnlyWithNotes(false) }
                )
            }
        }

        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search notes, subjects, timers", color = TextSecondary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIce,
                    unfocusedBorderColor = StrokeSoft,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentFrost,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                shape = MaterialTheme.shapes.medium
            )
        }

        if (state.isEmpty) {
            item {
                EmptyDiary(
                    hasAnySessions = state.hasAnySessions,
                    isSearching = state.query.isNotBlank(),
                    onStartFocus = onStartFocus
                )
            }
        } else {
            state.days.forEach { day ->
                item(key = "day-${day.date}") {
                    DayHeader(day = day)
                }
                items(day.sessions, key = { "session-${it.id}" }) { session ->
                    DiaryEntryCard(
                        session = session,
                        onEdit = { editingId = session.id }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
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
private fun DiaryHero(noteCount: Int, dayCount: Int, focusedSeconds: Long) {
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(PrimaryIce, GlacierBlue))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (noteCount == 1) "1 reflection" else "$noteCount reflections",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = if (dayCount == 0) {
                        "One line after each session, kept on this device"
                    } else {
                        "Across $dayCount ${if (dayCount == 1) "day" else "days"} · on this device"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            if (focusedSeconds > 0L) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDuration(focusedSeconds),
                        style = MaterialTheme.typography.titleLarge,
                        color = AccentFrost
                    )
                    Text(
                        text = "reflected on",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DayHeader(day: DiaryDay) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = diaryDateLabel(day.date),
            style = MaterialTheme.typography.titleMedium,
            color = AccentFrost,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${day.sessions.size} · ${formatDuration(day.totalSeconds)}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiaryEntryCard(session: SessionEntity, onEdit: () -> Unit) {
    var viewingPhoto by remember { mutableStateOf(false) }

    AppCard(modifier = Modifier.clickable(onClick = onEdit)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(SurfaceHigh, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = modeIcon(session.mode),
                        contentDescription = null,
                        tint = AccentFrost,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.tag.trim().ifBlank { modeLabel(session.mode) },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = buildString {
                            append(formatSessionTime(session.endedAt))
                            append(" · ")
                            append(modeLabel(session.mode))
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDuration(session.durationSeconds.toLong()),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (session.completed) "Completed" else "Stopped",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (session.completed) SuccessGreen else TextSecondary
                    )
                }
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

            val stamps = stampsFor(session)
            if (stamps.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stamps.forEach { stamp ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(AccentFrost.copy(alpha = 0.10f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = stamp.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentFrost
                            )
                        }
                    }
                }
            }

            if (session.note.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceHigh.copy(alpha = 0.65f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "“${session.note}”",
                        style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        color = TextPrimary,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "No reflection yet — tap to add one",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
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
private fun EmptyDiary(
    hasAnySessions: Boolean,
    isSearching: Boolean,
    onStartFocus: () -> Unit
) {
    AppCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AcUnit,
                    contentDescription = null,
                    tint = AccentFrost,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = when {
                    isSearching -> "No entries match that search."
                    hasAnySessions -> "No reflections yet."
                    else -> "Your diary starts with one session."
                },
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = when {
                    isSearching -> "Try a different word, or switch to All sessions."
                    hasAnySessions -> "Finish a session and write one line about what you learned."
                    else -> "Pick a subject, focus, then write one line about what you moved forward."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            if (!hasAnySessions) {
                PrimaryButton(
                    text = "Start a focus session",
                    onClick = onStartFocus,
                    modifier = Modifier.padding(top = 4.dp)
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
        containerColor = com.frostlock.app.ui.theme.SurfaceCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (initialNote.isBlank()) "Add a reflection" else "Edit reflection",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = subject,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentFrost
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        Text("What did you learn or finish?", color = TextSecondary)
                    },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryIce,
                        unfocusedBorderColor = SurfaceHigh,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentFrost
                    ),
                    shape = MaterialTheme.shapes.medium
                )
                Text(
                    text = "${note.length}/${SessionEntity.MAX_NOTE_LENGTH} · stays on this device",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(note) }) {
                Text(text = "Save", color = AccentFrost)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (initialNote.isNotBlank()) {
                    TextButton(onClick = { onSave("") }) {
                        Text(text = "Clear", color = DangerRed)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(text = "Cancel", color = TextSecondary)
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
