package com.frostlock.app.ui.components.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.theme.FrostShapes
import com.frostlock.app.ui.theme.FrostTheme

/** Emphasis level for [FrostButton]. */
enum class FrostButtonVariant {
    /** The single key action on a screen. Gradient, pill. */
    PRIMARY,

    /** Supporting action. Outlined pill. */
    SECONDARY,

    /** Inline or low-emphasis action. Text only. */
    GHOST,

    /** Destructive action. */
    DANGER
}

/**
 * The one button component for the redesign.
 *
 * Only [FrostButtonVariant.PRIMARY] uses the action gradient, and only one
 * primary button should appear per screen.
 */
@Composable
fun FrostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: FrostButtonVariant = FrostButtonVariant.PRIMARY,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    tint: Color? = null
) {
    val colors = FrostTheme.colors
    when (variant) {
        FrostButtonVariant.PRIMARY -> {
            Box(
                modifier = modifier
                    .heightIn(min = 52.dp)
                    .alpha(if (enabled) 1f else 0.5f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.horizontalGradient(colors.actionGradient),
                            shape = FrostShapes.pill
                        )
                )
                Button(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = FrostShapes.pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = colors.onPrimary,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = colors.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = FrostTheme.spacing.xxl)
                ) {
                    ButtonContent(text = text, icon = icon)
                }
            }
        }

        FrostButtonVariant.SECONDARY -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = FrostShapes.pill,
                border = BorderStroke(1.dp, colors.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = tint ?: colors.textPrimary,
                    disabledContentColor = colors.textSecondary
                ),
                contentPadding = PaddingValues(horizontal = FrostTheme.spacing.xxl)
            ) {
                ButtonContent(text = text, icon = icon)
            }
        }

        FrostButtonVariant.GHOST -> {
            TextButton(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = tint ?: colors.primary,
                    disabledContentColor = colors.textSecondary
                )
            ) {
                ButtonContent(text = text, icon = icon)
            }
        }

        FrostButtonVariant.DANGER -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = FrostShapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.danger,
                    contentColor = colors.background,
                    disabledContainerColor = colors.danger.copy(alpha = 0.4f),
                    disabledContentColor = colors.background.copy(alpha = 0.6f)
                ),
                contentPadding = PaddingValues(horizontal = FrostTheme.spacing.xxl)
            ) {
                ButtonContent(text = text, icon = icon)
            }
        }
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.width(FrostTheme.spacing.sm))
        }
        Text(text = text, style = FrostTheme.typography.labelLarge, maxLines = 1)
    }
}
