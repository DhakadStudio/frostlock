package com.frostlock.app.ui.screens.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.local.prefs.YouTubeChannelStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ChannelsUiState(
    val enabled: Boolean = false,
    val allowOnly: Boolean = false,
    val blocked: List<String> = emptyList(),
    val allowed: List<String> = emptyList()
)

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val store: YouTubeChannelStore
) : ViewModel() {

    val uiState: StateFlow<ChannelsUiState> = combine(
        store.enabled,
        store.allowOnly,
        store.blocked,
        store.allowed
    ) { enabled, allowOnly, blocked, allowed ->
        ChannelsUiState(
            enabled = enabled,
            allowOnly = allowOnly,
            blocked = blocked.sorted(),
            allowed = allowed.sorted()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ChannelsUiState()
    )

    fun setEnabled(enabled: Boolean) = store.setEnabled(enabled)

    fun setAllowOnly(allowOnly: Boolean) = store.setAllowOnly(allowOnly)

    /** Returns false when the typed text is not a usable channel name. */
    fun addBlocked(raw: String): Boolean = store.addBlocked(raw)

    fun removeBlocked(name: String) = store.removeBlocked(name)

    /** Returns false when the typed text is not a usable channel name. */
    fun addAllowed(raw: String): Boolean = store.addAllowed(raw)

    fun removeAllowed(name: String) = store.removeAllowed(name)
}
