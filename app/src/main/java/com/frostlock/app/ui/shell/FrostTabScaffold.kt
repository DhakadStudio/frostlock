package com.frostlock.app.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.frostlock.app.ui.components.surface.frostPageBackground
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Tab-root scaffold: large title, optional subtitle, optional actions, and a
 * slot for the navigation bar.
 *
 * The header is width-capped to match content, so a tablet does not stretch the
 * title across the whole screen. The page itself is the shared frosted wash, so
 * the header sits directly on it instead of on a flat block of colour.
 */
@Composable
fun FrostTabScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationBar: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Box(modifier = modifier.fillMaxSize().frostPageBackground()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { FrostTabHeader(title = title, subtitle = subtitle, actions = actions) },
            bottomBar = navigationBar,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
        ) { padding -> content(padding) }
    }
}

@Composable
private fun FrostTabHeader(
    title: String,
    subtitle: String?,
    actions: @Composable RowScope.() -> Unit
) {
    // The root host already applies the status-bar inset to the whole graph,
    // so the header must not add it a second time (that was the dead space
    // above the large titles).
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = FrostTheme.spacing.contentMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = FrostTheme.spacing.screenHorizontal)
                .padding(top = FrostTheme.spacing.xl, bottom = FrostTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = FrostTheme.typography.headlineLarge,
                    color = FrostTheme.colors.textPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = FrostTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }
            actions()
        }
    }
}
