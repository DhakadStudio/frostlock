package com.frostlock.app.ui.screens.shield

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.ui.components.controls.FrostSwitchRow
import com.frostlock.app.ui.components.surface.FrostCard
import com.frostlock.app.ui.shell.FrostDetailScaffold
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Shield → Apps → Advanced.
 *
 * Protection surfaces that are not a plain app list. All three only act while a
 * focus session or an active schedule is running, so they cannot surprise the
 * user outside focus.
 */
@Composable
fun AdvancedScreen(
    onBack: () -> Unit,
    viewModel: AdvancedViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    FrostDetailScaffold(title = "Advanced", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = FrostTheme.spacing.xl,
                    vertical = FrostTheme.spacing.md
                ),
            verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.md)
        ) {
            Text(
                text = "These controls apply during focus sessions and active schedules.",
                style = FrostTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary
            )

            FrostCard {
                FrostSwitchRow(
                    title = "Home screen",
                    description = "Prevent leaving focus by opening the home screen.",
                    checked = state.homeScreen,
                    onCheckedChange = viewModel::setHomeScreen
                )
            }

            FrostCard {
                FrostSwitchRow(
                    title = "YouTube home feed",
                    description = "Keep YouTube for search and subscriptions during focus.",
                    checked = state.youtubeHome,
                    onCheckedChange = viewModel::setYouTubeHome
                )
            }

            FrostCard {
                FrostSwitchRow(
                    title = "Play Store search",
                    description = "A search for a blocked app opens its install page, so that search is held during focus.",
                    checked = state.playStore,
                    onCheckedChange = viewModel::setPlayStore
                )
            }
        }
    }
}
