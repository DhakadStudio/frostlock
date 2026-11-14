package com.frostlock.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.local.datastore.SettingsDataStore
import com.frostlock.app.data.local.prefs.ThemeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsDataStore: SettingsDataStore,
    themeStore: ThemeStore
) : ViewModel() {
    val onboardingDone: StateFlow<Boolean?> = settingsDataStore.onboardingDone
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val themeKey: StateFlow<String> = themeStore.paletteKey
}
