package com.frostlock.app.ui.screens.block

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.blocking.FocusBlockAccessibilityService
import com.frostlock.app.data.InstalledApp
import com.frostlock.app.data.InstalledAppsRepository
import com.frostlock.app.data.local.prefs.AppLimitStore
import com.frostlock.app.data.local.prefs.BlockingPreferences
import com.frostlock.app.data.local.prefs.FocusSchedule
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.data.local.prefs.ScheduleStore
import com.frostlock.app.notifications.NotificationArchiveStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class NotificationUiState(
    val enabled: Boolean = false,
    val accessGranted: Boolean = false,
    val total: Int = 0,
    val heldToday: Int = 0
)

data class BlockUiState(
    val apps: List<InstalledApp> = emptyList(),
    val blockedPackages: Set<String> = emptySet(),
    val limits: Map<String, Int> = emptyMap(),
    val usageMillis: Map<String, Long> = emptyMap(),
    val blockingEnabled: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val schedules: List<FocusSchedule> = emptyList(),
    val activeRuleLabels: List<String> = emptyList(),
    val notifications: NotificationUiState = NotificationUiState(),
    val loading: Boolean = true
) {
    val blockedCount: Int get() = blockedPackages.size
    val limitedCount: Int get() = limits.count { it.value > 0 }
    val notificationsActive: Boolean
        get() = notifications.enabled && notifications.accessGranted
}

@HiltViewModel
class BlockViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blockingPreferences: BlockingPreferences,
    private val limitStore: AppLimitStore,
    private val scheduleStore: ScheduleStore,
    private val installedApps: InstalledAppsRepository,
    private val focusSettings: FocusSettings,
    private val archive: NotificationArchiveStore
) : ViewModel() {

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val _loading = MutableStateFlow(true)
    private val _accessibilityEnabled = MutableStateFlow(false)
    private val _limits = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _usage = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val _schedules = MutableStateFlow<List<FocusSchedule>>(emptyList())
    private val _activeRuleLabels = MutableStateFlow<List<String>>(emptyList())
    private val _listenerAccess = MutableStateFlow(false)
    private val _notifications = combine(
        focusSettings.notificationBlockingEnabled,
        _listenerAccess,
        archive.items
    ) { enabled, access, items ->
        val todayStart = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        NotificationUiState(
            enabled = enabled,
            accessGranted = access,
            total = items.size,
            heldToday = items.count { it.postedAt >= todayStart }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationUiState()
    )

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
        ruleState,
        _notifications
    ) { base, limits, usage, (schedules, active), notifications ->
        base.copy(
            limits = limits,
            usageMillis = usage,
            schedules = schedules,
            activeRuleLabels = active,
            notifications = notifications
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BlockUiState()
    )

    init {
        loadApps()
        refreshAccessibilityStatus()
        refreshNotificationAccess()
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

    fun refreshNotificationAccess() {
        _listenerAccess.value = NotificationManagerCompat
            .getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    fun setBlockingEnabled(enabled: Boolean) = blockingPreferences.setBlockingEnabled(enabled)

    fun setNotificationBlocking(enabled: Boolean) {
        focusSettings.setNotificationBlockingEnabled(enabled)
        if (enabled && !_listenerAccess.value) openNotificationAccessSettings()
    }

    fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

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
