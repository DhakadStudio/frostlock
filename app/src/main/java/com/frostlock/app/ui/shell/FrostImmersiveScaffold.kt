package com.frostlock.app.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Full-screen host for Focusing, the session summary and the block wall.
 *
 * No top bar and no navigation bar: these flows own the whole window. System bar
 * visibility is controlled by the caller, because only it knows whether the
 * session is strict.
 */
@Composable
fun FrostImmersiveScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FrostTheme.colors.background),
        content = content
    )
}
