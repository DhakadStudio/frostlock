package com.frostlock.app.capability

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.frostlock.app.blocking.FocusBlockAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A system capability FrostLock needs for a protection feature to actually work. */
enum class Capability {
    APP_BLOCKING,
    NOTIFICATION_ACCESS,
    OVERLAY,
    EXACT_ALARMS,
    POST_NOTIFICATIONS
}

/**
 * The truth about one capability or feature.
 *
 * [NOT_CONFIGURED] belongs to features (no apps selected, no schedules yet),
 * not to permissions. A capability repository only returns the other three.
 */
enum class CapabilityState {
    GRANTED,
    DENIED,
    NOT_CONFIGURED,
    UNAVAILABLE
}

/** Pure mapping so the permission state table is unit-testable. */
fun resolveCapability(supported: Boolean, granted: Boolean): CapabilityState = when {
    !supported -> CapabilityState.UNAVAILABLE
    granted -> CapabilityState.GRANTED
    else -> CapabilityState.DENIED
}

/**
 * Reads every capability FrostLock depends on and re-checks on demand.
 *
 * Call [refresh] on `ON_RESUME`, after returning from system settings, and from
 * the date/time-change receiver. UI must never claim protection while a required
 * capability is [CapabilityState.DENIED].
 */
@Singleton
class CapabilityRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _states = MutableStateFlow(read())
    val states: StateFlow<Map<Capability, CapabilityState>> = _states.asStateFlow()

    fun refresh() {
        _states.value = read()
    }

    fun state(capability: Capability): CapabilityState =
        _states.value[capability] ?: CapabilityState.UNAVAILABLE

    private fun read(): Map<Capability, CapabilityState> = mapOf(
        Capability.APP_BLOCKING to resolveCapability(
            supported = true,
            granted = FocusBlockAccessibilityService.isEnabled(context)
        ),
        Capability.NOTIFICATION_ACCESS to resolveCapability(
            supported = true,
            granted = notificationAccessGranted()
        ),
        Capability.OVERLAY to resolveCapability(
            supported = true,
            granted = Settings.canDrawOverlays(context)
        ),
        Capability.EXACT_ALARMS to exactAlarmState(),
        Capability.POST_NOTIFICATIONS to postNotificationState()
    )

    private fun notificationAccessGranted(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

    private fun exactAlarmState(): CapabilityState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return CapabilityState.GRANTED
        }
        val manager = context.getSystemService(AlarmManager::class.java)
        return resolveCapability(
            supported = true,
            granted = manager?.canScheduleExactAlarms() == true
        )
    }

    private fun postNotificationState(): CapabilityState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return CapabilityState.GRANTED
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        return resolveCapability(supported = true, granted = granted)
    }
}
