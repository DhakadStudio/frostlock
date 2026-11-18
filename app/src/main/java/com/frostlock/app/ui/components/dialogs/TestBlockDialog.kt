package com.frostlock.app.ui.components.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.frostlock.app.data.InstalledApp
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Debug-only check that a blocked app really shows the block wall.
 *
 * Reachable from a long-press on the focusing screen's shield line, so it never
 * competes with the real session controls.
 */
@Composable
fun TestBlockDialog(
    app: InstalledApp,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FrostTheme.colors.surfaceContainer,
        titleContentColor = FrostTheme.colors.textPrimary,
        textContentColor = FrostTheme.colors.textSecondary,
        icon = {
            if (app.icon != null) {
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(FrostTheme.colors.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = FrostTheme.colors.success
                    )
                }
            }
        },
        title = {
            Text(
                text = "Try opening ${app.label}",
                style = FrostTheme.typography.headlineMedium,
                color = FrostTheme.colors.textPrimary
            )
        },
        text = {
            Text(
                text = "Open the app to see FrostLock's block screen — then come " +
                    "right back.",
                style = FrostTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onOpen) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = FrostTheme.colors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Open ${app.label}",
                    color = FrostTheme.colors.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Later", color = FrostTheme.colors.textSecondary)
            }
        }
    )
}
