package com.frostlock.app.ui.screens.shield

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.capability.Capability
import com.frostlock.app.capability.CapabilityRepository
import com.frostlock.app.capability.CapabilityState
import com.frostlock.app.data.local.prefs.AppLimitStore
import com.frostlock.app.data.local.prefs.BlockingPreferences
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.data.local.prefs.ScheduleStore
import com.frostlock.app.data.local.prefs.WebsiteBlockingStore
import com.frostlock.app.data.local.prefs.YouTubeChannelStore
import com.frostlock.app.notifications.NotificationArchiveStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** What Shield shows at a glance. */
data class ShieldState(
    val appsEnabled: Boolean = false,
    val appsCount: Int = 0,
    val limitsCount: Int = 0,
    val schedulesCount: Int = 0,
    val activeSchedules: Int = 0,
    val websitesEnabled: Boolean = false,
    val websitesCount: Int = 0,
    val channelsEnabled: Boolean = false,
    val channelsCount: Int = 0,
    val notificationsEnabled: Boolean = false,
    val notificationsHeld: Int = 0,
    val blockingAccess: CapabilityState = CapabilityState.DENIED,
    val notificationAccess: CapabilityState = CapabilityState.DENIED
) {
    val blockingReady: Boolean get() = blockingAccess == CapabilityState.GRANTED
    val notificationsReady: Boolean get() = notificationAccess == CapabilityState.GRANTED
}

/**
 * Temporary Shield state for the Phase 2 navigation shell.
 *
 * It reads the same stores the old Block screen reads, so the hub cannot claim
 * protection that the accessibility service is not actually providing.
 */
@HiltViewModel
class ShieldViewModel @Inject constructor(
    blockingPreferences: BlockingPreferences,
    private val limitStore: AppLimitStore,
    private val scheduleStore: ScheduleStore,
    websiteStore: WebsiteBlockingStore,
    channelStore: YouTubeChannelStore,
    archive: NotificationArchiveStore,
    capabilities: CapabilityRepository,
    focusSettings: FocusSettings
) : ViewModel() {

    private data class Protection(
        val appsEnabled: Boolean,
        val appsCount: Int,
        val websitesEnabled: Boolean,
        val websitesCount: Int,
        val channelsEnabled: Boolean
    )

    private data class Activity(
        val channelsCount: Int,
        val notificationsHeld: Int,
        val notificationsEnabled: Boolean,
        val capabilities: Map<Capability, CapabilityState>
    )

    private data class Counts(
        val limits: Int,
        val schedules: Int,
        val activeSchedules: Int
    )

    // Limits and schedules are read from SharedPreferences rather than exposed
    // as flows, so the hub re-reads them whenever it resumes instead of showing
    // a count from before the user edited a detail screen.
    private val limitsCount = MutableStateFlow(limitStore.limits().size)
    private val schedulesCount = MutableStateFlow(scheduleStore.schedules().size)
    private val activeSchedules = MutableStateFlow(scheduleStore.activeSchedules().size)

    private val protection = combine(
        blockingPreferences.blockingEnabled,
        blockingPreferences.blockedPackages,
        websiteStore.enabled,
        websiteStore.domains,
        channelStore.enabled
    ) { appsEnabled, apps, websitesEnabled, websites, channelsEnabled ->
        Protection(
            appsEnabled = appsEnabled,
            appsCount = apps.size,
            websitesEnabled = websitesEnabled,
            websitesCount = websites.size,
            channelsEnabled = channelsEnabled
        )
    }

    private val activity = combine(
        channelStore.blocked,
        archive.items,
        focusSettings.notificationBlockingEnabled,
        capabilities.states
    ) { channels, items, notificationsEnabled, caps ->
        Activity(
            channelsCount = channels.size,
            notificationsHeld = items.size,
            notificationsEnabled = notificationsEnabled,
            capabilities = caps
        )
    }

    val state: StateFlow<ShieldState> = combine(
        protection,
        activity,
        combine(limitsCount, schedulesCount, activeSchedules) { limits, schedules, active ->
            Counts(limits = limits, schedules = schedules, activeSchedules = active)
        }
    ) { p, a, counts ->
        ShieldState(
            appsEnabled = p.appsEnabled,
            appsCount = p.appsCount,
            limitsCount = counts.limits,
            schedulesCount = counts.schedules,
            activeSchedules = counts.activeSchedules,
            websitesEnabled = p.websitesEnabled,
            websitesCount = p.websitesCount,
            channelsEnabled = p.channelsEnabled,
            channelsCount = a.channelsCount,
            notificationsEnabled = a.notificationsEnabled,
            notificationsHeld = a.notificationsHeld,
            blockingAccess = a.capabilities[Capability.APP_BLOCKING] ?: CapabilityState.DENIED,
            notificationAccess = a.capabilities[Capability.NOTIFICATION_ACCESS]
                ?: CapabilityState.DENIED
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ShieldState()
    )

    /** Re-reads the SharedPreferences-backed counts when the hub resumes. */
    fun refresh() {
        limitsCount.value = limitStore.limits().size
        schedulesCount.value = scheduleStore.schedules().size
        activeSchedules.value = scheduleStore.activeSchedules().size
    }
}
