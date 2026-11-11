package com.focusflow.app.ui.screens.block

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusflow.app.blocking.FocusBlockAccessibilityService
import com.focusflow.app.data.InstalledApp
import com.focusflow.app.data.InstalledAppsRepository
import com.focusflow.app.data.local.prefs.AppLimitStore
import com.focusflow.app.data.local.prefs.BlockingPreferences
import com.focusflow.app.data.local.prefs.FocusSchedule
import com.focusflow.app.data.local.prefs.ScheduleStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockUiState(
    val apps: List<InstalledApp> = emptyList(),
    val blockedPackages: Set<String> = emptySet(),
    val limits: Map<String, Int> = emptyMap(),
    val usageMillis: Map<String, Long> = emptyMap(),
    val blockingEnabled: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val schedules: List<FocusSchedule> = emptyList(),
    val activeRuleLabels: List<String> = emptyList(),
    val loading: Boolean = true
) {
    val blockedCount: Int get() = blockedPackages.size
    val limitedCount: Int get() = limits.count { it.value > 0 }
}

@HiltViewModel
class BlockViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blockingPreferences: BlockingPreferences,
    private val limitStore: AppLimitStore,
    private val scheduleStore: ScheduleStore,
    private val installedApps: InstalledAppsRepository
) : ViewModel() {

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val _loading = MutableStateFlow(true)
    private val _accessibilityEnabled = MutableStateFlow(false)
    private val _limits = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _usage = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val _schedules = MutableStateFlow<List<FocusSchedule>>(emptyList())
    private val _activeRuleLabels = MutableStateFlow<List<String>>(emptyList())

    private val appState = combine(
        _apps,
        blockingPreferences.blockedPackages,
        blockingPreferences.blockingEnabled,
        _accessibilityEnabled,
        _loading
    ) { apps, blocked, enabled, accessibility, loading ->
        BlockUiState(
            apps = apps,
            blockedPackages = blocked,
            blockingEnabled = enabled,
            accessibilityEnabled = accessibility,
            loading = loading
        )
    }

    private val ruleState = combine(_schedules, _activeRuleLabels) { schedules, active ->
        schedules to active
    }

    val uiState: StateFlow<BlockUiState> = combine(
        appState,
        _limits,
        _usage,
        ruleState
    ) { base, limits, usage, (schedules, active) ->
        base.copy(
            limits = limits,
            usageMillis = usage,
            schedules = schedules,
            activeRuleLabels = active
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BlockUiState()
    )

    init {
        loadApps()
        refreshAccessibilityStatus()
        refreshLimits()
        refreshSchedules()
    }

    fun refreshAccessibilityStatus() {
        _accessibilityEnabled.value = FocusBlockAccessibilityService.isEnabled(context)
    }

    /** Re-reads usage, limits and rules; call when the screen resumes. */
    fun refreshLimits() {
        _limits.value = limitStore.limits()
        _usage.value = limitStore.usageToday()
    }

    fun refreshSchedules() {
        _schedules.value = scheduleStore.schedules()
        _activeRuleLabels.value = scheduleStore.activeSchedules().map { it.schedule.label }
    }

    fun setBlockingEnabled(enabled: Boolean) = blockingPreferences.setBlockingEnabled(enabled)

    fun togglePackage(packageName: String) = blockingPreferences.togglePackage(packageName)

    /** Zero removes the limit. */
    fun setLimit(packageName: String, minutes: Int) {
        limitStore.setLimit(packageName, minutes)
        refreshLimits()
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun loadApps() {
        viewModelScope.launch {
            _apps.value = installedApps.apps()
            _loading.value = false
        }
    }
}
