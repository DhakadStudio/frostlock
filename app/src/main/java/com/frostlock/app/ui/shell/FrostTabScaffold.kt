package com.frostlock.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Tab-root scaffold: large title, optional subtitle, optional actions, and a
 * slot for the navigation bar.
 *
 * The header is width-capped to match content, so a tablet does not stretch the
 * title across the whole screen.
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
    Scaffold(
        modifier = modifier,
        containerColor = FrostTheme.colors.background,
        topBar = { FrostTabHeader(title = title, subtitle = subtitle, actions = actions) },
        bottomBar = navigationBar,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    ) { padding -> content(padding) }
}

@Composable
private fun FrostTabHeader(
    title: String,
    subtitle: String?,
    actions: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(FrostTheme.colors.background)
            .statusBarsPadding(),
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
