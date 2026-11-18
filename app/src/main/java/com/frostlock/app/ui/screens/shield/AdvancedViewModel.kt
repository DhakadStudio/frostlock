package com.frostlock.app.ui.screens.shield

import androidx.lifecycle.ViewModel
import com.frostlock.app.data.local.prefs.BlockingPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Protection surfaces that are not a plain app list. */
data class AdvancedUiState(
    val homeScreen: Boolean = false,
    val youtubeHome: Boolean = false,
    val playStore: Boolean = false
)

/**
 * Home screen, YouTube home and Play Store controls.
 *
 * These are protection settings, so they live under Shield → Apps → Advanced
 * rather than in You. All three only take effect while a focus session or an
 * active schedule is running.
 */
@HiltViewModel
class AdvancedViewModel @Inject constructor(
    private val prefs: BlockingPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(read())
    val state: StateFlow<AdvancedUiState> = _state.asStateFlow()

    fun setHomeScreen(enabled: Boolean) {
        prefs.setHomeBlockingEnabled(enabled)
        _state.update { it.copy(homeScreen = enabled) }
    }

    fun setYouTubeHome(enabled: Boolean) {
        prefs.setYouTubeHomeBlockingEnabled(enabled)
        _state.update { it.copy(youtubeHome = enabled) }
    }

    fun setPlayStore(enabled: Boolean) {
        prefs.setPlayStoreBlockingEnabled(enabled)
        _state.update { it.copy(playStore = enabled) }
    }

    /** Picks up changes made elsewhere, such as a restored backup. */
    fun refresh() {
        _state.value = read()
    }

    private fun read() = AdvancedUiState(
        homeScreen = prefs.isHomeBlockingEnabled(),
        youtubeHome = prefs.isYouTubeHomeBlockingEnabled(),
        playStore = prefs.isPlayStoreBlockingEnabled()
    )
}
