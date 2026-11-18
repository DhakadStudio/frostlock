package com.frostlock.app.ui.components.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import com.frostlock.app.ui.theme.FrostShapes
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Gradient card for the one hero moment on a screen.
 *
 * Used by the Focus timer, the Shield status and the Insights today card. Do not
 * stack two of these on one screen: the gradient is the emphasis.
 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    shape: Shape = FrostShapes.hero,
    contentPadding: PaddingValues = PaddingValues(FrostTheme.spacing.xxl),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(FrostTheme.colors.heroGradient),
                shape = shape
            )
            .padding(contentPadding),
        content = content
    )
}
