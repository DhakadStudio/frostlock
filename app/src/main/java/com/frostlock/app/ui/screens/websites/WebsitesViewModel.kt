package com.frostlock.app.ui.screens.websites

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.blocking.FocusBlockAccessibilityService
import com.frostlock.app.data.local.prefs.WebsiteBlockingStore
import com.frostlock.app.ui.util.DefaultBlockedDomains
import com.frostlock.app.ui.util.WebsiteCategory
import com.frostlock.app.ui.util.WebsiteCategories
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class WebsitesUiState(
    val enabled: Boolean = false,
    val domains: Set<String> = emptySet(),
    val allowedDomains: Set<String> = emptySet(),
    val accessibilityEnabled: Boolean = false,
    val categories: List<WebsiteCategory> = WebsiteCategories,
    val customDomains: List<String> = emptyList()
)

@HiltViewModel
class WebsitesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: WebsiteBlockingStore
) : ViewModel() {

    private val _accessibilityEnabled = MutableStateFlow(false)

    val uiState: StateFlow<WebsitesUiState> = combine(
        store.enabled,
        store.domains,
        store.allowedDomains,
        _accessibilityEnabled
    ) { enabled, domains, allowed, accessibility ->
        WebsitesUiState(
            enabled = enabled,
            domains = domains,
            allowedDomains = allowed,
            accessibilityEnabled = accessibility,
            categories = WebsiteCategories,
            customDomains = domains
                .filterNot { it in DefaultBlockedDomains }
                .sorted()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WebsitesUiState()
    )

    init {
        refreshAccessibilityStatus()
    }

    fun refreshAccessibilityStatus() {
        _accessibilityEnabled.value = FocusBlockAccessibilityService.isEnabled(context)
    }

    fun setEnabled(enabled: Boolean) = store.setEnabled(enabled)

    fun setDomainBlocked(domain: String, blocked: Boolean) =
        store.setDomainBlocked(domain, blocked)

    /** Returns false when the typed text is not a valid domain. */
    fun addDomain(raw: String): Boolean = store.addDomain(raw) != null

    fun removeDomain(domain: String) = store.removeDomain(domain)

    /** Returns false when the typed text is not a valid domain. */
    fun addAllowedDomain(raw: String): Boolean = store.addAllowedDomain(raw) != null

    fun removeAllowedDomain(domain: String) = store.removeAllowedDomain(domain)

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
