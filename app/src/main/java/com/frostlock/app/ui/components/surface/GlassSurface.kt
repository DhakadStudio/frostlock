package com.frostlock.app.ui.components.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.theme.FrostShapes
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Direction A surface treatment: frosted depth.
 *
 * Three primitives, shared by every screen so the app has one material story:
 *
 * 1. [frostPageBackground] — a vertical wash plus a faint accent bloom behind
 *    the header. It replaces the flat background colour everywhere.
 * 2. [frostGlass] — a translucent card that lets the page wash read through,
 *    with a hairline edge that is bright on top and fades down: light from
 *    above, not an outline.
 * 3. [frostPanel] — an opaque gradient for surfaces that float above a scrim
 *    (dialogs and sheets), so text stays crisp while the panel still has depth.
 */

/** Page canvas: soft vertical wash with an accent bloom at the top. */
@Composable
fun Modifier.frostPageBackground(): Modifier {
    val colors = FrostTheme.colors
    return drawBehind {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(colors.pageTop, colors.pageBottom),
                startY = 0f,
                endY = size.height
            )
        )
        // Bloom behind the header. It fades to a transparent version of the
        // same colour, so it never greys out the middle of the gradient.
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(colors.headerGlow, colors.headerGlow.copy(alpha = 0f)),
                center = Offset(size.width / 2f, size.height * 0.02f),
                radius = size.width * 0.85f
            )
        )
    }
}

/**
 * Translucent glass for cards sitting on the page.
 *
 * [strong] is for surfaces that must separate from other glass, such as the
 * floating navigation bar or an elevated card.
 */
@Composable
fun Modifier.frostGlass(
    shape: Shape = FrostShapes.large,
    strong: Boolean = false,
    borderWidth: Dp = 1.dp
): Modifier {
    val colors = FrostTheme.colors
    val fill = if (strong) colors.glassFillStrong else colors.glassFill
    return this
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    if (strong) fill else colors.glassFillStrong,
                    fill
                )
            ),
            shape = shape
        )
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(colors.glassEdgeTop, colors.glassEdgeBottom)
            ),
            shape = shape
        )
}

/**
 * Opaque gradient for dialogs and sheets.
 *
 * The panel is lighter at the top and carries a bright top hairline, which is
 * what makes it read as a raised object instead of a grey slab.
 */
@Composable
fun Modifier.frostPanel(
    shape: Shape = FrostShapes.extraLarge,
    borderWidth: Dp = 1.dp
): Modifier {
    val colors = FrostTheme.colors
    return this
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(colors.panelTop, colors.panelBottom)
            ),
            shape = shape
        )
        .border(
            width = borderWidth,
            brush = Brush.verticalGradient(
                colors = listOf(colors.panelEdgeTop, colors.panelEdgeBottom)
            ),
            shape = shape
        )
}

/** Convenience wrapper when a screen needs a background box around content. */
@Composable
fun FrostPageBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .frostPageBackground(),
        content = content
    )
}
