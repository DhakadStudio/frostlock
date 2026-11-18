package com.frostlock.app.ui.components.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.shell.rememberIsTabletopPosture
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Adaptive two-pane container for expanded layouts.
 *
 * Side by side: a fixed-width primary pane (usually status or selection) and a
 * flexible secondary pane (usually detail or a list), split by a hairline.
 *
 * In tabletop posture the panes stack vertically instead — display content on
 * top, controls below the hinge — which is how a foldable in half-open mode
 * wants to be used.
 *
 * Panes scroll independently; each pane owns its own scroll container.
 */
@Composable
fun TwoPaneLayout(
    modifier: Modifier = Modifier,
    primaryWidth: Dp = 400.dp,
    primary: @Composable () -> Unit,
    secondary: @Composable () -> Unit
) {
    if (rememberIsTabletopPosture()) {
        Column(modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) { primary() }
            HorizontalDivider(color = FrostTheme.colors.outline.copy(alpha = 0.35f))
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) { secondary() }
        }
    } else {
        Row(modifier = modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(primaryWidth)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopStart
            ) {
                primary()
            }
            VerticalDivider(color = FrostTheme.colors.outline.copy(alpha = 0.35f))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopStart
            ) {
                secondary()
            }
        }
    }
}
