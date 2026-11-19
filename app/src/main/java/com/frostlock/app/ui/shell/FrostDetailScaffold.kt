package com.frostlock.app.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.frostlock.app.ui.components.surface.frostPageBackground
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Standard detail scaffold: back arrow, small title, optional actions and an
 * optional bottom bar.
 *
 * The bottom bar stays visible here; only immersive flows hide navigation. The
 * page uses the shared frosted wash, and the app bar is transparent so the
 * header bloom reaches behind it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrostDetailScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    navigationBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Box(modifier = modifier.fillMaxSize().frostPageBackground()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = FrostTheme.typography.titleLarge,
                            color = FrostTheme.colors.textPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = FrostTheme.colors.textPrimary
                            )
                        }
                    },
                    actions = actions,
                    // The root host owns the status-bar inset.
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = FrostTheme.colors.textPrimary,
                        navigationIconContentColor = FrostTheme.colors.textPrimary,
                        actionIconContentColor = FrostTheme.colors.textPrimary
                    )
                )
            },
            bottomBar = navigationBar,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = FrostTheme.spacing.contentMaxWidth)
                ) {
                    content(padding)
                }
            }
        }
    }
}
