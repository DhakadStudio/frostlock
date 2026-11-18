package com.frostlock.app.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.theme.FrostTheme

/**
 * A single-value progress ring with a slot in the middle.
 *
 * The ring shows goal progress; the caller puts the duration inside it. The
 * track is always visible, so zero progress still reads as a ring rather than a
 * blank space.
 */
@Composable
fun GoalRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 150.dp,
    strokeWidth: Dp = 12.dp,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = FrostTheme.colors
    val safeProgress = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .size(diameter)
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = colors.surfaceContainerHigh,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            if (safeProgress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(colors.actionGradient),
                    startAngle = -90f,
                    sweepAngle = 360f * safeProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        content()
    }
}
