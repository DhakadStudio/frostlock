package com.frostlock.app.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.frostlock.app.ui.components.controls.FrostButton
import com.frostlock.app.ui.components.controls.FrostButtonVariant
import com.frostlock.app.ui.components.surface.frostGlass
import com.frostlock.app.ui.components.surface.frostPanel
import com.frostlock.app.ui.theme.FrostShapes
import com.frostlock.app.ui.theme.FrostTheme
import com.frostlock.app.ui.util.formatDuration

/**
 * Confirms the end of a session that has already run for a while.
 *
 * Saving keeps the work; discarding leaves no trace but keeps the intention for
 * the next attempt. The three choices are stacked in order of safety: save,
 * discard, then keep going.
 *
 * The panel is an opaque gradient with a bright top edge, so it reads as a
 * raised object above the dimmed page rather than a flat grey slab.
 */
@Composable
fun EndSessionDialog(
    elapsedSeconds: Int,
    plannedSeconds: Int,
    onKeepGoing: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    val colors = FrostTheme.colors
    val focused = formatDuration(elapsedSeconds.toLong())
    val planned = if (plannedSeconds > 0) formatDuration(plannedSeconds.toLong()) else null
    val shape = FrostTheme.shapes.extraLarge

    Dialog(
        onDismissRequest = onKeepGoing,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FrostTheme.spacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .shadow(elevation = 28.dp, shape = shape, clip = false)
                    .frostPanel(shape = shape)
                    .verticalScroll(rememberScrollState())
                    .padding(FrostTheme.spacing.xl)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        colors.danger.copy(alpha = 0.30f),
                                        colors.danger.copy(alpha = 0.10f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            tint = colors.danger,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(FrostTheme.spacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "End this session?",
                            style = FrostTheme.typography.titleLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Saving keeps this round in your history.",
                            style = FrostTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FrostTheme.spacing.lg))

                Row(horizontalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)) {
                    StatTile(
                        label = "Focused",
                        value = focused,
                        modifier = Modifier.weight(1f)
                    )
                    if (planned != null) {
                        StatTile(
                            label = "Planned",
                            value = planned,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(FrostTheme.spacing.xl))

                FrostButton(
                    text = "Save and end",
                    onClick = onSave,
                    icon = Icons.Default.Check,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
                FrostButton(
                    text = "Discard session",
                    onClick = onDiscard,
                    variant = FrostButtonVariant.SECONDARY,
                    tint = colors.danger,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
                FrostButton(
                    text = "Keep going",
                    onClick = onKeepGoing,
                    variant = FrostButtonVariant.GHOST,
                    tint = colors.textPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
                Text(
                    text = "Discarding leaves no trace and keeps your intention " +
                        "for next time.",
                    style = FrostTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}

/** One small labelled value inside the dialog, e.g. "Focused / 22s". */
@Composable
private fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .frostGlass(shape = FrostShapes.medium)
            .padding(FrostTheme.spacing.md)
    ) {
        Text(
            text = label.uppercase(),
            style = FrostTheme.typography.labelMedium,
            color = FrostTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
        Text(
            text = value,
            style = FrostTheme.typography.titleLarge,
            color = FrostTheme.colors.textPrimary
        )
    }
}
