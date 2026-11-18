package com.frostlock.app.ui.screens.shield

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.InstalledApp
import com.frostlock.app.data.InstalledAppsRepository
import com.frostlock.app.data.local.prefs.AppLimitStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Per-app daily budgets and how much of each is already used today. */
data class LimitsUiState(
    val apps: List<InstalledApp> = emptyList(),
    val limits: Map<String, Int> = emptyMap(),
    val usageMillis: Map<String, Long> = emptyMap(),
    val loading: Boolean = true
) {
    val limitedCount: Int get() = limits.count { it.value > 0 }

    fun usedMinutes(packageName: String): Int =
        ((usageMillis[packageName] ?: 0L) / 60_000L).toInt()
}

@HiltViewModel
class LimitsViewModel @Inject constructor(
    private val limitStore: AppLimitStore,
    private val installedApps: InstalledAppsRepository
) : ViewModel() {

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val _loading = MutableStateFlow(true)
    private val _limits = MutableStateFlow(limitStore.limits())
    private val _usage = MutableStateFlow(limitStore.usageToday())

    val state: StateFlow<LimitsUiState> = combine(
        _apps,
        _limits,
        _usage,
        _loading
    ) { apps, limits, usage, loading ->
        LimitsUiState(
            apps = apps,
            limits = limits,
            usageMillis = usage,
            loading = loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LimitsUiState()
    )

    init {
        viewModelScope.launch {
            _apps.value = installedApps.apps()
            _loading.value = false
        }
        refresh()
    }

    /** Re-reads usage and budgets; call when the screen resumes. */
    fun refresh() {
        _limits.value = limitStore.limits()
        _usage.value = limitStore.usageToday()
    }

    /** Zero removes the limit. */
    fun setLimit(packageName: String, minutes: Int) {
        limitStore.setLimit(packageName, minutes)
        refresh()
    }
}
