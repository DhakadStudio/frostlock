package com.frostlock.app.ui.components.surface

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.theme.FrostTheme

/**
 * One row of a protection or settings list: icon, title, live status and a
 * chevron.
 *
 * Status is always text, never colour alone, and the row merges its children so
 * TalkBack reads "Apps, on, 4 apps held" as one item.
 */
@Composable
fun StatusRow(
    title: String,
    status: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    statusColor: Color = FrostTheme.colors.textSecondary,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = true
) {
    val description = "$title, $status"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(vertical = FrostTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FrostTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.width(FrostTheme.spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = FrostTheme.typography.titleMedium,
                color = FrostTheme.colors.textPrimary
            )
            Text(
                text = status,
                style = FrostTheme.typography.bodyMedium,
                color = statusColor
            )
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = FrostTheme.colors.textSecondary
            )
        }
    }
}
