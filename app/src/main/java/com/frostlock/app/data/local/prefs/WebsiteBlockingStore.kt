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

    private val _allowedDomains = MutableStateFlow(readAllowedDomains())
    val allowedDomains: StateFlow<Set<String>> = _allowedDomains.asStateFlow()

    private val _allowOnly = MutableStateFlow(prefs.getBoolean(KEY_ALLOW_ONLY, false))
    val allowOnly: StateFlow<Boolean> = _allowOnly.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _enabled.value = enabled
    }

    /**
     * Study mode: when true, only domains on the allowed list may open during
     * focus and the block list is ignored. False is the classic block-list
     * behaviour, where the allowed list only pokes holes in the block list.
     */
    fun setAllowOnly(allowOnly: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_ONLY, allowOnly).apply()
        _allowOnly.value = allowOnly
    }

    /**
     * Live read for the accessibility service. The service constructs its own
     * store instance, whose flows would go stale after the UI changes a value:
     * SharedPreferences does not notify other instances in the same process.
     */
    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    /** Live read for the accessibility service. */
    fun blockedDomainList(): Set<String> = readDomains()

    /** Live read for the accessibility service. */
    fun allowedDomainList(): Set<String> = readAllowedDomains()

    /** Live study-mode read for the accessibility service. */
    fun isAllowOnly(): Boolean = prefs.getBoolean(KEY_ALLOW_ONLY, false)

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

    /** Adds a typed domain to the allowlist. Returns the normalized host or null. */
    fun addAllowedDomain(raw: String): String? {
        val normalized = normalizeDomain(raw) ?: return null
        if (normalized !in _allowedDomains.value) {
            writeAllowed(_allowedDomains.value + normalized)
        }
        return normalized
    }

    fun removeAllowedDomain(domain: String) {
        val normalized = normalizeDomain(domain) ?: return
        writeAllowed(_allowedDomains.value - normalized)
    }

    private fun write(domains: Set<String>) {
        prefs.edit().putStringSet(KEY_DOMAINS, domains).apply()
        _domains.value = domains
    }

    private fun writeAllowed(domains: Set<String>) {
        prefs.edit().putStringSet(KEY_ALLOWED_DOMAINS, domains).apply()
        _allowedDomains.value = domains
    }

    private fun readDomains(): Set<String> {
        val stored = prefs.getStringSet(KEY_DOMAINS, null)
        return stored?.mapNotNull(::normalizeDomain)?.toSet() ?: DefaultBlockedDomains
    }

    private fun readAllowedDomains(): Set<String> {
        val stored = prefs.getStringSet(KEY_ALLOWED_DOMAINS, null)
        return stored?.mapNotNull(::normalizeDomain)?.toSet() ?: emptySet()
    }

    companion object {
        const val PREFS_NAME = "frostlock_website_blocking"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_DOMAINS = "domains"
        private const val KEY_ALLOWED_DOMAINS = "allowed_domains"
        private const val KEY_ALLOW_ONLY = "allow_only_listed"
    }
}
