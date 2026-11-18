package com.frostlock.app.ui.components.surface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.theme.FrostShapes
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Access-missing card.
 *
 * Appears where the affected feature lives, explains what is read, and offers
 * the one action that fixes it. Never a toast, never a green "protected" state.
 */
@Composable
fun PermissionCard(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val warning = FrostTheme.colors.warning
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = FrostShapes.large,
        color = warning.copy(alpha = 0.10f),
        contentColor = FrostTheme.colors.textPrimary,
        border = BorderStroke(1.dp, warning.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(PaddingValues(FrostTheme.spacing.lg))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = warning
                )
                Spacer(modifier = Modifier.width(FrostTheme.spacing.sm))
                Text(
                    text = title,
                    style = FrostTheme.typography.titleMedium,
                    color = FrostTheme.colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
            Text(
                text = message,
                style = FrostTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary
            )
            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = FrostTheme.spacing.xs)
            ) {
                Text(
                    text = actionLabel,
                    style = FrostTheme.typography.labelLarge,
                    color = FrostTheme.colors.primary
                )
            }
        }
    }
}
