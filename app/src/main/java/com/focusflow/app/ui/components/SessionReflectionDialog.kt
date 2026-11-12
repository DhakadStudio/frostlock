package com.focusflow.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.focusflow.app.data.local.db.SessionEntity
import com.focusflow.app.ui.theme.AccentPurple
import com.focusflow.app.ui.theme.DangerRed
import com.focusflow.app.ui.theme.PrimaryPurple
import com.focusflow.app.ui.theme.SurfaceCard
import com.focusflow.app.ui.theme.SurfaceHigh
import com.focusflow.app.ui.theme.TextPrimary
import com.focusflow.app.ui.theme.TextSecondary

/**
 * A tiny study diary shown after a session is saved. Regain lets students name a
 * session and reflect on it; FocusFlow keeps that idea but stores the note locally
 * next to the existing subject tag so it can appear in history and stats.
 */
@Composable
fun SessionReflectionDialog(
    subject: String,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf("") }
    val subjectLabel = subject.trim().ifBlank { "Untagged" }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Session saved",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "$subjectLabel · what did you focus on?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentPurple
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        if (it.length <= SessionEntity.MAX_NOTE_LENGTH) note = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Add a note for your study diary", color = TextSecondary)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = SurfaceHigh,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentPurple
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
                Text(text = "Save note", color = AccentPurple)
            }
        },
        dismissButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = onDelete) {
                    Text(text = "Delete", color = DangerRed)
                }
                TextButton(onClick = onDismiss) {
                    Text(text = "Skip", color = TextSecondary)
                }
            }
        }
    )
}
