package com.frostlock.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.frostlock.app.ui.components.navigation.FrostNavigationBar
import com.frostlock.app.ui.session.FocusSessionHostViewModel
import com.frostlock.app.ui.session.ImmersiveSessionHost
import com.frostlock.app.ui.session.RunningSessionBar
import com.frostlock.app.ui.screens.onboarding.OnboardingScreen
import com.frostlock.app.ui.screens.onboarding.OnboardingViewModel
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Root host: onboarding, then the four-tab main graph.
 *
 * One [NavHostController] owns the whole back stack, including the nested tab
 * graphs, so system back and predictive back behave like a single app rather
 * than two hosts fighting over the dispatcher.
 *
 * Phase 6: the active session is hosted by the shell-level [ImmersiveSessionHost]
 * above the NavHost, so tab switches never tear it down. A session that was
 * minimised lives as a running-session bar in the bottom bar instead.
 */
@Composable
fun AppNavHost(
    startDestination: String,
    onFinishApp: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val viewModel: FocusSessionHostViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    val pendingSummaryId by viewModel.pendingSummaryId.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTab = TopLevelDestination.fromRoute(currentRoute)
    val immersive = state.immersiveVisible || state.justCompleted
    val showNavigation =
        currentTab != null && !immersive && currentRoute !in Destinations.immersiveRoutes

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = FrostTheme.colors.background,
            bottomBar = {
                if (showNavigation) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (state.hasStarted && !state.immersiveVisible && !state.justCompleted) {
                            RunningSessionBar(
                                state = state,
                                onOpen = viewModel::showImmersive,
                                onPause = viewModel::pause,
                                onResume = viewModel::startOrResume,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 16.dp,
                                        end = 16.dp,
                                        top = 8.dp,
                                        bottom = 4.dp
                                    )
                            )
                        }
                        FrostNavigationBar(
                            items = TopLevelDestination.entries.map { it.navItem },
                            selectedRoute = currentTab?.route ?: Destinations.Focus.HOME,
                            onSelect = navController::navigateToTab
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(padding)
            ) {
                composable(Destinations.ONBOARDING) {
                    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
                    OnboardingScreen(
                        viewModel = onboardingViewModel,
                        onFinished = {
                            navController.navigate(Destinations.MAIN) {
                                popUpTo(Destinations.ONBOARDING) { inclusive = true }
                            }
                        },
                        onExit = onFinishApp
                    )
                }

                navigation(
                    route = Destinations.MAIN,
                    startDestination = Destinations.Focus.GRAPH
                ) {
                    mainNavGraph(navController)
                }
            }
        }

        ImmersiveSessionHost(
            state = state,
            onMinimize = viewModel::minimize,
            onPause = viewModel::pause,
            onResume = viewModel::startOrResume,
            onStop = viewModel::stop,
            onDiscard = viewModel::discardSession,
            onEndStrict = viewModel::endStrictSession,
            onSkipBreak = viewModel::skipBreak
        )
    }

    LaunchedEffect(pendingSummaryId) {
        val id = pendingSummaryId ?: return@LaunchedEffect
        viewModel.consumeSummary()
        navController.navigate(Destinations.Focus.summary(id)) {
            launchSingleTop = true
        }
    }
}
