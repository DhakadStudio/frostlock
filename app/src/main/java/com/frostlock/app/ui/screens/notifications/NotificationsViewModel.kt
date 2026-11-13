package com.frostlock.app.ui.screens.notifications

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.InstalledAppsRepository
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.notifications.BlockedNotification
import com.frostlock.app.notifications.NotificationArchiveStore
import com.frostlock.app.ui.util.NotificationDay
import com.frostlock.app.ui.util.buildNotificationDays
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class NotificationInboxUiState(
    val enabled: Boolean = false,
    val accessGranted: Boolean = false,
    val days: List<NotificationDay> = emptyList(),
    val total: Int = 0,
    val icons: Map<String, ImageBitmap?> = emptyMap()
) {
    val isEmpty: Boolean get() = days.isEmpty()
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val archive: NotificationArchiveStore,
    private val settings: FocusSettings,
    private val installedApps: InstalledAppsRepository
) : ViewModel() {

    private val accessGranted = MutableStateFlow(isAccessGranted())
    private val icons = MutableStateFlow<Map<String, ImageBitmap?>>(emptyMap())

    val uiState: StateFlow<NotificationInboxUiState> = combine(
        settings.notificationBlockingEnabled,
        accessGranted,
        archive.items,
        icons
    ) { enabled, access, items, iconMap ->
        NotificationInboxUiState(
            enabled = enabled,
            accessGranted = access,
            days = buildNotificationDays(items),
            total = items.size,
            icons = iconMap
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationInboxUiState()
    )

    init {
        viewModelScope.launch {
            archive.items.collect { items ->
                val missing = items
                    .map { it.packageName }
                    .distinct()
                    .filterNot { icons.value.containsKey(it) }
                if (missing.isEmpty()) return@collect
                val loaded = withContext(Dispatchers.IO) {
                    missing.associateWith { installedApps.iconFor(it) }
                }
                icons.value = icons.value + loaded
            }
        }
    }

    fun refreshAccess() {
        accessGranted.value = isAccessGranted()
    }

    fun setEnabled(enabled: Boolean) {
        settings.setNotificationBlockingEnabled(enabled)
        if (enabled && !accessGranted.value) openAccessSettings()
    }

    fun openAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun remove(item: BlockedNotification) = archive.remove(item.id)

    fun clearAll() = archive.clear()

    private fun isAccessGranted(): Boolean = NotificationManagerCompat
        .getEnabledListenerPackages(context)
        .contains(context.packageName)
}
