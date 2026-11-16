package com.frostlock.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import com.frostlock.app.ui.util.YouTubeChannelDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration for YouTube channel filtering: either a block list ("never let
 * these channels play") or a study allowlist ("only let these play") while
 * focus or a rule is active.
 *
 * SharedPreferences instead of DataStore because the accessibility service
 * reads this on every YouTube content change, where an asynchronous read is
 * not practical.
 */
@Singleton
class YouTubeChannelStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _allowOnly = MutableStateFlow(prefs.getBoolean(KEY_ALLOW_ONLY, false))
    val allowOnly: StateFlow<Boolean> = _allowOnly.asStateFlow()

    private val _blocked = MutableStateFlow(readSet(KEY_BLOCKED))
    val blocked: StateFlow<Set<String>> = _blocked.asStateFlow()

    private val _allowed = MutableStateFlow(readSet(KEY_ALLOWED))
    val allowed: StateFlow<Set<String>> = _allowed.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _enabled.value = enabled
    }

    fun setAllowOnly(allowOnly: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_ONLY, allowOnly).apply()
        _allowOnly.value = allowOnly
    }

    /**
     * Live reads for the accessibility service. The service constructs its own
     * store instance, whose flows would go stale after the UI changes a value:
     * SharedPreferences does not notify other instances in the same process.
     */
    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    /** Live read for the accessibility service. */
    fun allowOnlyMode(): Boolean = prefs.getBoolean(KEY_ALLOW_ONLY, false)

    /** Live read for the accessibility service. */
    fun blockedChannels(): Set<String> = readSet(KEY_BLOCKED)

    /** Live read for the accessibility service. */
    fun allowedChannels(): Set<String> = readSet(KEY_ALLOWED)

    /** Adds a normalized channel to the block list; false when nothing to add. */
    fun addBlocked(raw: String): Boolean = addTo(KEY_BLOCKED, raw, _blocked)

    fun removeBlocked(name: String) = removeFrom(KEY_BLOCKED, name, _blocked)

    /** Adds a normalized channel to the study allowlist; false when nothing to add. */
    fun addAllowed(raw: String): Boolean = addTo(KEY_ALLOWED, raw, _allowed)

    fun removeAllowed(name: String) = removeFrom(KEY_ALLOWED, name, _allowed)

    private fun readSet(key: String): Set<String> =
        prefs.getStringSet(key, emptySet())?.toSet().orEmpty()

    private fun addTo(
        key: String,
        raw: String,
        flow: MutableStateFlow<Set<String>>
    ): Boolean {
        val name = YouTubeChannelDetector.normalize(raw)
        if (name.isEmpty()) return false
        val updated = flow.value + name
        prefs.edit().putStringSet(key, updated).apply()
        flow.value = updated
        return true
    }

    private fun removeFrom(
        key: String,
        raw: String,
        flow: MutableStateFlow<Set<String>>
    ) {
        val name = YouTubeChannelDetector.normalize(raw)
        val updated = flow.value - name
        prefs.edit().putStringSet(key, updated).apply()
        flow.value = updated
    }

    companion object {
        const val PREFS_NAME = "frostlock_youtube_channels"
        const val KEY_ENABLED = "channel_blocking_enabled"
        const val KEY_ALLOW_ONLY = "allow_only_listed"
        const val KEY_BLOCKED = "blocked_channels"
        const val KEY_ALLOWED = "allowed_channels"
    }
}
