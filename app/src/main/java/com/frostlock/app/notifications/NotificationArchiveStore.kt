package com.frostlock.app.notifications

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local archive of notifications that were held while an app was blocked.
 *
 * Everything stays in SharedPreferences on the device: no listener data is ever
 * uploaded, and clearing the inbox removes it permanently. The list is capped so
 * a long-running install cannot grow without limit.
 *
 * Entries are deduplicated by platform notification key: when an app updates the
 * same notification, the existing row is refreshed and moved to the top instead
 * of being added twice. [capped] tells the inbox whether older entries were
 * dropped so it can say "Showing latest 200" instead of implying a full history.
 */
@Singleton
class NotificationArchiveStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val initial = load()
    private val _items = MutableStateFlow(initial)
    val items: StateFlow<List<BlockedNotification>> = _items.asStateFlow()

    private val _capped = MutableStateFlow(initial.size >= MAX_ENTRIES)
    val capped: StateFlow<Boolean> = _capped.asStateFlow()

    /** Adds an entry at the top, replacing an existing entry with the same [key]. */
    fun add(
        packageName: String,
        appLabel: String,
        title: String,
        text: String,
        postedAt: Long,
        reason: BlockReason,
        ruleLabel: String? = null,
        key: String? = null
    ): BlockedNotification {
        val cleanKey = key?.take(MAX_KEY)
        val existing = cleanKey?.let { k -> _items.value.firstOrNull { it.key == k } }
        val item = BlockedNotification(
            id = existing?.id ?: nextId(),
            packageName = packageName,
            appLabel = appLabel,
            title = title.take(MAX_TITLE),
            text = text.take(MAX_TEXT),
            postedAt = postedAt,
            reason = reason,
            ruleLabel = ruleLabel?.take(MAX_RULE_LABEL),
            key = cleanKey
        )
        val updated = mergeBlockedNotification(_items.value, item, MAX_ENTRIES)
        persist(updated)
        _items.value = updated
        _capped.value = updated.size >= MAX_ENTRIES
        return item
    }

    fun remove(id: Long) {
        val updated = _items.value.filterNot { it.id == id }
        if (updated.size == _items.value.size) return
        persist(updated)
        _items.value = updated
        _capped.value = updated.size >= MAX_ENTRIES
    }

    fun clear() {
        prefs.edit().remove(KEY_ITEMS).apply()
        _items.value = emptyList()
        _capped.value = false
    }

    private fun nextId(): Long {
        val next = prefs.getLong(KEY_NEXT_ID, 1L).coerceAtLeast(1L)
        prefs.edit().putLong(KEY_NEXT_ID, next + 1).apply()
        return next
    }

    private fun load(): List<BlockedNotification> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.toItem()
            }
        }.getOrDefault(emptyList())
    }

    private fun persist(items: List<BlockedNotification>) {
        val array = JSONArray()
        items.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    private fun JSONObject.toItem(): BlockedNotification? {
        val reason = optString(KEY_REASON)
            .let { name -> BlockReason.entries.firstOrNull { it.name == name } }
            ?: return null
        val packageName = optString(KEY_PACKAGE)
        if (packageName.isBlank()) return null
        return BlockedNotification(
            id = optLong(KEY_ID),
            packageName = packageName,
            appLabel = optString(KEY_APP, packageName),
            title = optString(KEY_TITLE),
            text = optString(KEY_TEXT),
            postedAt = optLong(KEY_AT),
            reason = reason,
            ruleLabel = optString(KEY_RULE).takeIf { it.isNotBlank() },
            key = optString(KEY_KEY).takeIf { it.isNotBlank() }
        )
    }

    private fun BlockedNotification.toJson(): JSONObject = JSONObject().apply {
        put(KEY_ID, id)
        put(KEY_PACKAGE, packageName)
        put(KEY_APP, appLabel)
        put(KEY_TITLE, title)
        put(KEY_TEXT, text)
        put(KEY_AT, postedAt)
        put(KEY_REASON, reason.name)
        ruleLabel?.let { put(KEY_RULE, it) }
        key?.let { put(KEY_KEY, it) }
    }

    companion object {
        const val PREFS_NAME = "frostlock_notifications"
        const val MAX_ENTRIES = 200
        private const val MAX_TITLE = 160
        private const val MAX_TEXT = 400
        private const val MAX_RULE_LABEL = 60
        private const val MAX_KEY = 200
        private const val KEY_ITEMS = "blocked_notifications"
        private const val KEY_NEXT_ID = "next_id"
        private const val KEY_ID = "id"
        private const val KEY_PACKAGE = "package"
        private const val KEY_APP = "app"
        private const val KEY_TITLE = "title"
        private const val KEY_TEXT = "text"
        private const val KEY_AT = "at"
        private const val KEY_REASON = "reason"
        private const val KEY_RULE = "rule"
        private const val KEY_KEY = "key"
    }
}

/**
 * Puts [incoming] at the top of [current], replacing any entry with the same
 * notification key, then trims to [maxEntries].
 *
 * Kept free of Android and SharedPreferences so the dedupe and cap rules can be
 * unit tested directly.
 */
internal fun mergeBlockedNotification(
    current: List<BlockedNotification>,
    incoming: BlockedNotification,
    maxEntries: Int = NotificationArchiveStore.MAX_ENTRIES
): List<BlockedNotification> {
    if (maxEntries <= 0) return emptyList()
    val withoutDuplicate = incoming.key?.let { key ->
        current.filterNot { it.key == key }
    } ?: current
    return (listOf(incoming) + withoutDuplicate).take(maxEntries)
}
