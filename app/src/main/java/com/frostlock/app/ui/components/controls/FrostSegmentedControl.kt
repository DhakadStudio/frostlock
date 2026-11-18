package com.frostlock.app.ui.components.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.frostlock.app.ui.theme.FrostShapes
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Pill segmented control for mutually exclusive views (timer mode, period).
 *
 * Selection changes both fill and shape, so it does not depend on colour alone.
 */
@Composable
fun <T> FrostSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(FrostTheme.colors.surfaceContainerHigh, FrostShapes.pill)
            .padding(FrostTheme.spacing.xs)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                onClick = { onSelect(option) },
                modifier = Modifier.weight(1f),
                shape = FrostShapes.pill,
                color = if (isSelected) FrostTheme.colors.primary else Color.Transparent,
                contentColor = if (isSelected) {
                    FrostTheme.colors.onPrimary
                } else {
                    FrostTheme.colors.textSecondary
                }
            ) {
                Box(
                    modifier = Modifier.padding(vertical = FrostTheme.spacing.sm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label(option),
                        style = FrostTheme.typography.labelLarge,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
