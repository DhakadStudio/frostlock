package com.frostlock.app.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.theme.FrostTheme
import com.frostlock.app.ui.util.formatDuration

/**
 * Confirms the end of a session that has already run for a while.
 *
 * Saving keeps the work; discarding leaves no trace but keeps the intention for
 * the next attempt.
 */
@Composable
fun EndSessionDialog(
    elapsedSeconds: Int,
    plannedSeconds: Int,
    onKeepGoing: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    val focused = formatDuration(elapsedSeconds.toLong())
    val planned = if (plannedSeconds > 0) formatDuration(plannedSeconds.toLong()) else null

    AlertDialog(
        onDismissRequest = onKeepGoing,
        containerColor = FrostTheme.colors.surfaceContainer,
        titleContentColor = FrostTheme.colors.textPrimary,
        textContentColor = FrostTheme.colors.textSecondary,
        title = {
            Text(
                text = "End this session?",
                style = FrostTheme.typography.headlineMedium,
                color = FrostTheme.colors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)) {
                Text(
                    text = if (planned != null) {
                        "You've focused $focused of $planned."
                    } else {
                        "You've focused $focused."
                    },
                    style = FrostTheme.typography.bodyLarge,
                    color = FrostTheme.colors.textPrimary
                )
                Text(
                    text = "Save it to your history and stats, or discard it so it " +
                        "leaves no trace. Discarding keeps your intention for next time.",
                    style = FrostTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(
                    text = "Save and end",
                    style = FrostTheme.typography.labelLarge,
                    color = FrostTheme.colors.primary
                )
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = onDiscard) {
                    Text(
                        text = "Discard",
                        style = FrostTheme.typography.labelLarge,
                        color = FrostTheme.colors.danger
                    )
                }
                TextButton(onClick = onKeepGoing) {
                    Text(
                        text = "Keep going",
                        style = FrostTheme.typography.labelLarge,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }
        }
    )
}
