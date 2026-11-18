package com.frostlock.app.ui.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.frostlock.app.focus.FocusUiState

/**
 * Shell-level host for the active session.
 *
 * It sits above the navigation host, so switching tabs never tears the timer
 * down. Minimising hides the host and leaves the running-session bar instead.
 */
@Composable
fun ImmersiveSessionHost(
    state: FocusUiState,
    onMinimize: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onDiscard: () -> Unit,
    onEndStrict: () -> Unit,
    onSkipBreak: () -> Unit
) {
    if (!state.immersiveVisible || !state.hasStarted) return
    val dismissible = !state.strictActive

    Dialog(
        onDismissRequest = { if (dismissible) onMinimize() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = dismissible,
            decorFitsSystemWindows = false
        )
    ) {
        HideSystemBars()
        FocusingScreen(
            state = state,
            onMinimize = onMinimize,
            onPause = onPause,
            onResume = onResume,
            onStop = onStop,
            onDiscard = onDiscard,
            onEndStrict = onEndStrict,
            onSkipBreak = onSkipBreak
        )
    }
}

@Composable
private fun HideSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.parent as? DialogWindowProvider)?.window
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, view).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            if (window != null) {
                WindowInsetsControllerCompat(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}
