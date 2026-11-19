package com.frostlock.app.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.frostlock.app.ui.components.surface.frostPageBackground
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Base surface for a screen: app background, centred content column and a
 * readable maximum width.
 *
 * Set [scrollable] for simple content. Screens that own a `LazyColumn` leave it
 * false to avoid nesting two scroll containers.
 */
@Composable
fun FrostScreen(
    modifier: Modifier = Modifier,
    maxWidth: Dp = FrostTheme.spacing.contentMaxWidth,
    horizontalPadding: Dp = FrostTheme.spacing.screenHorizontal,
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .frostPageBackground(),
        contentAlignment = Alignment.TopCenter
    ) {
        val sized = Modifier
            .fillMaxHeight()
            .widthIn(max = maxWidth)
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)

        Column(
            modifier = if (scrollable) sized.verticalScroll(rememberScrollState()) else sized,
            content = content
        )
    }
}
