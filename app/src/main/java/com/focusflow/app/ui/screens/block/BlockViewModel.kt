package com.focusflow.app.ui.screens.block

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusflow.app.blocking.FocusBlockAccessibilityService
import com.focusflow.app.data.local.prefs.AppLimitStore
import com.focusflow.app.data.local.prefs.BlockingPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.ImageBitmap? = null
)

data class BlockUiState(
    val apps: List<InstalledApp> = emptyList(),
    val blockedPackages: Set<String> = emptySet(),
    val limits: Map<String, Int> = emptyMap(),
    val usageMillis: Map<String, Long> = emptyMap(),
    val blockingEnabled: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val loading: Boolean = true
)

@HiltViewModel
class BlockViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blockingPreferences: BlockingPreferences,
    private val limitStore: AppLimitStore
) : ViewModel() {

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val _loading = MutableStateFlow(true)
    private val _accessibilityEnabled = MutableStateFlow(false)
    private val _limits = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _usage = MutableStateFlow<Map<String, Long>>(emptyMap())

    private val baseState = combine(
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

    val uiState: StateFlow<BlockUiState> = combine(
        baseState,
        _limits,
        _usage
    ) { base, limits, usage ->
        base.copy(limits = limits, usageMillis = usage)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BlockUiState()
    )

    init {
        loadApps()
        refreshAccessibilityStatus()
        refreshLimits()
    }

    fun refreshAccessibilityStatus() {
        _accessibilityEnabled.value = FocusBlockAccessibilityService.isEnabled(context)
    }

    /** Re-reads usage and limits; call when the screen resumes. */
    fun refreshLimits() {
        _limits.value = limitStore.limits()
        _usage.value = limitStore.usageToday()
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
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(launcherIntent, 0)
            }

            val apps = resolved
                .mapNotNull { it.activityInfo?.applicationInfo }
                .distinctBy { it.packageName }
                .filter { it.packageName != context.packageName }
                .map { info ->
                    InstalledApp(
                        packageName = info.packageName,
                        label = runCatching {
                            pm.getApplicationLabel(info).toString()
                        }.getOrDefault(info.packageName),
                        icon = runCatching {
                            pm.getApplicationIcon(info).toBitmap(96, 96).asImageBitmap()
                        }.getOrNull()
                    )
                }
                .sortedBy { it.label.lowercase() }

            _apps.value = apps
            _loading.value = false
        }
    }
}
