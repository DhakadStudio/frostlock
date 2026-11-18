package com.frostlock.app.ui.components.controls

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Full-row toggle.
 *
 * The whole row is the target, with `Role.Switch`, so TalkBack announces label,
 * description and state as one control. The switch itself is not separately
 * focusable.
 */
@Composable
fun FrostSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(vertical = FrostTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = FrostTheme.typography.titleMedium,
                color = if (enabled) FrostTheme.colors.textPrimary else FrostTheme.colors.textSecondary
            )
            if (description != null) {
                Text(
                    text = description,
                    style = FrostTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
            }
        }
        Spacer(modifier = Modifier.width(FrostTheme.spacing.md))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FrostTheme.colors.onPrimary,
                checkedTrackColor = FrostTheme.colors.primary,
                uncheckedThumbColor = FrostTheme.colors.textSecondary,
                uncheckedTrackColor = FrostTheme.colors.surfaceContainerHighest,
                uncheckedBorderColor = FrostTheme.colors.outline
            )
        )
    }
}
