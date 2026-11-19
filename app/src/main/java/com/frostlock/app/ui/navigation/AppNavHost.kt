package com.frostlock.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.frostlock.app.ui.components.navigation.FrostNavigationRail
import com.frostlock.app.ui.session.FocusSessionHostViewModel
import com.frostlock.app.ui.session.ImmersiveSessionHost
import com.frostlock.app.ui.session.RunningSessionBar
import com.frostlock.app.ui.screens.onboarding.OnboardingScreen
import com.frostlock.app.ui.screens.onboarding.OnboardingViewModel
import com.frostlock.app.ui.shell.FrostWindowSizeClass
import com.frostlock.app.ui.shell.rememberFrostWindowSizeClass
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Root host: onboarding, then the four-tab main graph.
 *
 * One [NavHostController] owns the whole back stack, including the nested tab
 * graphs, so system back and predictive back behave like a single app rather
 * than two hosts fighting over the dispatcher.
 *
 * Compact widths get a floating bottom bar; medium and expanded widths get a
 * navigation rail on the leading edge instead.
 *
 * Phase 6: the active session is hosted by the shell-level [ImmersiveSessionHost]
 * above the NavHost, so tab switches never tear it down. A session that was
 * minimised lives as a running-session bar above the navigation instead.
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
    val showMiniPlayer = state.hasStarted && !state.immersiveVisible && !state.justCompleted

    val widthClass = rememberFrostWindowSizeClass()
    val useRail = showNavigation && widthClass != FrostWindowSizeClass.COMPACT

    val navItems = TopLevelDestination.entries.map { it.navItem }
    val selectedRoute = currentTab?.route ?: Destinations.Focus.HOME

    Box(modifier = Modifier.fillMaxSize()) {
        if (useRail) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FrostTheme.colors.background)
            ) {
                FrostNavigationRail(
                    items = navItems,
                    selectedRoute = selectedRoute,
                    onSelect = navController::navigateToTab,
                    modifier = Modifier.fillMaxHeight()
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        SessionNavHost(
                            navController = navController,
                            startDestination = startDestination,
                            onFinishApp = onFinishApp,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (showMiniPlayer) {
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
                                    bottom = 12.dp
                                )
                        )
                    }
                }
            }
        } else {
            Scaffold(
                containerColor = FrostTheme.colors.background,
                bottomBar = {
                    if (showNavigation) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (showMiniPlayer) {
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
                                items = navItems,
                                selectedRoute = selectedRoute,
                                onSelect = navController::navigateToTab
                            )
                        }
                    }
                }
            ) { padding ->
                SessionNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    onFinishApp = onFinishApp,
                    modifier = Modifier.padding(padding)
                )
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
        // A break means the session keeps going after this round, so the
        // summary is a round summary: "Done", not "Save and end".
        val continues = state.phase == com.frostlock.app.focus.FocusPhase.BREAK
        navController.navigate(Destinations.Focus.summary(id, continues = continues)) {
            launchSingleTop = true
        }
    }
}

/** The NavHost shared by the rail and bottom-bar layouts. */
@Composable
private fun SessionNavHost(
    navController: NavHostController,
    startDestination: String,
    onFinishApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
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
