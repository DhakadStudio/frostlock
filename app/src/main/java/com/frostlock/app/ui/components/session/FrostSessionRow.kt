package com.frostlock.app.ui.components.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.ui.components.surface.FrostCard
import com.frostlock.app.ui.theme.FrostTheme
import com.frostlock.app.ui.util.formatDuration
import com.frostlock.app.ui.util.formatSessionTime
import com.frostlock.app.ui.util.modeIcon
import com.frostlock.app.ui.util.modeLabel

/**
 * One saved session in a list, on the redesign tokens.
 *
 * The subject tag is the title when present, because that is what the user
 * recognises; the timer mode becomes the fallback. A reflection is previewed,
 * never repeated in full.
 */
@Composable
fun FrostSessionRow(
    session: SessionEntity,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val title = session.tag.trim().ifBlank { modeLabel(session.mode) }
    val duration = formatDuration(session.durationSeconds.toLong())
    val time = formatSessionTime(session.endedAt)

    FrostCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(
            horizontal = FrostTheme.spacing.lg,
            vertical = FrostTheme.spacing.md
        )
    ) {
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(title)
                    append(", ")
                    append(duration)
                    append(", ")
                    append(time)
                    if (session.note.isNotBlank()) append(", has a reflection")
                }
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
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
            Spacer(modifier = Modifier.width(FrostTheme.spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = FrostTheme.typography.titleMedium,
                    color = FrostTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = time,
                    style = FrostTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary
                )
                if (session.note.isNotBlank()) {
                    Text(
                        text = "“${session.note}”",
                        style = FrostTheme.typography.bodySmall,
                        color = FrostTheme.colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(FrostTheme.spacing.sm))
            Text(
                text = duration,
                style = FrostTheme.typography.titleMedium,
                color = FrostTheme.colors.textPrimary
            )
        }
    }
}
