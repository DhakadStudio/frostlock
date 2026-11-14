package com.frostlock.app.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.PauseCircle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.ui.theme.AccentFrost
import com.frostlock.app.ui.theme.DangerRed
import com.frostlock.app.ui.theme.GlacierBlue
import com.frostlock.app.ui.theme.PrimaryIce
import com.frostlock.app.ui.theme.SuccessGreen
import com.frostlock.app.ui.theme.SurfaceCard
import com.frostlock.app.ui.theme.SurfaceHigh
import com.frostlock.app.ui.theme.TextPrimary
import com.frostlock.app.ui.theme.TextSecondary
import com.frostlock.app.ui.util.buildSessionSummary
import com.frostlock.app.ui.util.formatDuration
import com.frostlock.app.ui.util.stampsFor

/**
 * The post-session summary: what was planned, what was actually focused, the
 * subject/mode, the streak after saving, and a local study-diary reflection.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SessionReflectionDialog(
    session: SessionEntity,
    streakDays: Int,
    onSave: (String) -> Unit,
    onPickPhoto: (Uri) -> Unit,
    onRemovePhoto: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf("") }
    val summary = buildSessionSummary(session, streakDays)
    val outcomeColor = if (summary.completed) SuccessGreen else AccentFrost

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (summary.completed) "Session complete" else "Session saved",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "${summary.subjectLabel} · ${summary.modeLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentFrost
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(SurfaceHigh.copy(alpha = 0.62f))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "FOCUSED",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = formatDuration(summary.focusedSeconds.toLong()),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(outcomeColor.copy(alpha = 0.13f))
                                    .padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (summary.completed) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.PauseCircle
                                    },
                                    contentDescription = null,
                                    tint = outcomeColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = summary.outcomeLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = outcomeColor
                                )
                            }
                        }

                        summary.progress?.let { progress ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(SurfaceCard)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(PrimaryIce, GlacierBlue)
                                            )
                                        )
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SummaryMetric(
                                label = "Planned",
                                value = summary.plannedLabel,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryMetric(
                                label = "Streak",
                                value = if (summary.streakDays == 1) {
                                    "1 day"
                                } else {
                                    "${summary.streakDays} days"
                                },
                                icon = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                val stamps = stampsFor(session)
                if (stamps.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "STAMPS EARNED",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            stamps.forEach { stamp ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(AccentFrost.copy(alpha = 0.12f))
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
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
                }

                SessionPhotoEditor(
                    name = session.photoPath,
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
                Text(text = "Save reflection", color = AccentFrost)
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

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (icon) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(PrimaryIce.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AcUnit,
                    contentDescription = null,
                    tint = AccentFrost,
                    modifier = Modifier.size(13.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(2.dp))
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
        }
    }
}
