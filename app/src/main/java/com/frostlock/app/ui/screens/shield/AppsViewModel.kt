package com.frostlock.app.ui.screens.shield

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.capability.Capability
import com.frostlock.app.capability.CapabilityRepository
import com.frostlock.app.capability.CapabilityState
import com.frostlock.app.data.InstalledApp
import com.frostlock.app.data.InstalledAppsRepository
import com.frostlock.app.data.local.prefs.BlockingPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Which apps are held during focus, and whether the access to hold them exists. */
data class AppsUiState(
    val apps: List<InstalledApp> = emptyList(),
    val blockedPackages: Set<String> = emptySet(),
    val blockingEnabled: Boolean = false,
    val blockingAccess: CapabilityState = CapabilityState.DENIED,
    val loading: Boolean = true
) {
    val blockedCount: Int get() = blockedPackages.size

    /** Access is on, so a selected app can actually be held. */
    val ready: Boolean get() = blockingAccess == CapabilityState.GRANTED
}

@HiltViewModel
class AppsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blockingPreferences: BlockingPreferences,
    private val installedApps: InstalledAppsRepository,
    capabilities: CapabilityRepository
) : ViewModel() {

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val _loading = MutableStateFlow(true)

    val state: StateFlow<AppsUiState> = combine(
        _apps,
        blockingPreferences.blockedPackages,
        blockingPreferences.blockingEnabled,
        capabilities.states,
        _loading
    ) { apps, blocked, enabled, caps, loading ->
        AppsUiState(
            apps = apps,
            blockedPackages = blocked,
            blockingEnabled = enabled,
            blockingAccess = caps[Capability.APP_BLOCKING] ?: CapabilityState.DENIED,
            loading = loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppsUiState()
    )

    init {
        viewModelScope.launch {
            _apps.value = installedApps.apps()
            _loading.value = false
        }
    }

    fun setBlockingEnabled(enabled: Boolean) =
        blockingPreferences.setBlockingEnabled(enabled)

    fun togglePackage(packageName: String) =
        blockingPreferences.togglePackage(packageName)

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
