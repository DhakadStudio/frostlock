package com.frostlock.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import com.frostlock.app.ui.util.DefaultBlockedDomains
import com.frostlock.app.ui.util.normalizeDomain
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local website block list. The accessibility service reads it synchronously on
 * browser events, so SharedPreferences is the right fit; nothing is fetched or
 * uploaded.
 */
@Singleton
class WebsiteBlockingStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _domains = MutableStateFlow(readDomains())
    val domains: StateFlow<Set<String>> = _domains.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _enabled.value = enabled
    }

    fun isEnabled(): Boolean = _enabled.value

    /** Adds or removes one domain from the local list. */
    fun setDomainBlocked(domain: String, blocked: Boolean) {
        val normalized = normalizeDomain(domain) ?: return
        write(
            if (blocked) _domains.value + normalized else _domains.value - normalized
        )
    }

    /**
     * Adds a typed domain. Returns null when the input is not a valid domain;
     * otherwise returns the normalized host (already present is fine).
     */
    fun addDomain(raw: String): String? {
        val normalized = normalizeDomain(raw) ?: return null
        if (normalized !in _domains.value) {
            write(_domains.value + normalized)
        }
        return normalized
    }

    fun removeDomain(domain: String) {
        val normalized = normalizeDomain(domain) ?: return
        write(_domains.value - normalized)
    }

    private fun write(domains: Set<String>) {
        prefs.edit().putStringSet(KEY_DOMAINS, domains).apply()
        _domains.value = domains
    }

    private fun readDomains(): Set<String> {
        val stored = prefs.getStringSet(KEY_DOMAINS, null)
        return stored?.mapNotNull(::normalizeDomain)?.toSet() ?: DefaultBlockedDomains
    }

    companion object {
        const val PREFS_NAME = "frostlock_website_blocking"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_DOMAINS = "domains"
    }
}
