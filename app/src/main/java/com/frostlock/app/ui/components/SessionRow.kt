package com.frostlock.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.ui.util.formatDuration
import com.frostlock.app.ui.util.formatSessionTime
import com.frostlock.app.ui.util.modeLabel
import com.frostlock.app.ui.util.modeIcon
import com.frostlock.app.ui.theme.FrostTheme

@Composable
fun SessionRow(
    session: SessionEntity,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(FrostTheme.colors.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = modeIcon(session.mode),
                    contentDescription = null,
                    tint = FrostTheme.colors.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (session.tag.isBlank()) {
                        modeLabel(session.mode)
                    } else {
                        "${modeLabel(session.mode)} · ${session.tag}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = FrostTheme.colors.textPrimary
                )
                Text(
                    text = formatSessionTime(session.endedAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
                if (session.note.isNotBlank()) {
                    Text(
                        text = "“${session.note}”",
                        style = MaterialTheme.typography.bodySmall,
                        color = FrostTheme.colors.textSecondary,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDuration(session.durationSeconds.toLong()),
                    style = MaterialTheme.typography.titleMedium,
                    color = FrostTheme.colors.textPrimary
                )
                if (session.completed) {
                    Text(
                        text = "Completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FrostTheme.colors.success
                    )
                }
            }
        }
    }
}
