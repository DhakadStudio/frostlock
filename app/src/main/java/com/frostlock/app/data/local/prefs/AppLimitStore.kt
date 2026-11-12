package com.frostlock.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-app daily time budgets plus the usage counter that backs them.
 *
 * Usage is stored per calendar day (key includes the date) so limits reset at
 * midnight without any background job. The accessibility service records usage
 * as the user switches between apps and reads this store on every window change,
 * so SharedPreferences is used instead of DataStore.
 */
@Singleton
class AppLimitStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Avoids scanning every pref key on each usage commit. */
    @Volatile
    private var lastPrunedDay: LocalDate? = null

    fun limitMinutes(packageName: String): Int = prefs.getInt(limitKey(packageName), 0)

    fun setLimit(packageName: String, minutes: Int) {
        if (minutes <= 0) {
            prefs.edit().remove(limitKey(packageName)).apply()
        } else {
            prefs.edit().putInt(limitKey(packageName), minutes).apply()
        }
    }

    fun limits(): Map<String, Int> = prefs.all
        .filterKeys { it.startsWith(KEY_LIMIT_PREFIX) }
        .mapNotNull { (key, value) ->
            (value as? Int)?.let { key.removePrefix(KEY_LIMIT_PREFIX) to it }
        }
        .toMap()

    fun usageMillisToday(packageName: String): Long =
        prefs.getLong(usageKey(packageName), 0L)

    fun usageMinutesToday(packageName: String): Int =
        (usageMillisToday(packageName) / 60_000L).toInt()

    fun usageToday(): Map<String, Long> {
        val prefix = usagePrefix()
        return prefs.all
            .filterKeys { it.startsWith(prefix) }
            .mapNotNull { (key, value) ->
                (value as? Long)?.let { key.removePrefix(prefix) to it }
            }
            .toMap()
    }

    fun addUsage(packageName: String, millis: Long) {
        if (millis <= 0L) return
        val key = usageKey(packageName)
        prefs.edit().putLong(key, prefs.getLong(key, 0L) + millis).apply()
        pruneOldUsageIfNeeded()
    }

    fun isLimitReached(packageName: String): Boolean {
        val limit = limitMinutes(packageName)
        if (limit <= 0) return false
        return usageMillisToday(packageName) >= limit * 60_000L
    }

    /**
     * True once an app has used [WARN_FRACTION] of its budget but has not hit the
     * cap yet. The service sends one heads-up notification per app per day.
     */
    fun shouldWarn(packageName: String): Boolean {
        val limit = limitMinutes(packageName)
        if (limit <= 0) return false
        val used = usageMillisToday(packageName)
        return shouldWarn(used, limit) && !warningSent(packageName)
    }

    fun markWarningSent(packageName: String) {
        prefs.edit().putBoolean(warnedKey(packageName), true).apply()
    }

    fun warningSent(packageName: String): Boolean =
        prefs.getBoolean(warnedKey(packageName), false)

    fun remainingMillis(packageName: String): Long {
        val limit = limitMinutes(packageName)
        if (limit <= 0) return Long.MAX_VALUE
        return (limit * 60_000L - usageMillisToday(packageName)).coerceAtLeast(0L)
    }

    // --- Foreground tracking ---------------------------------------------------
    // The service stores which limited app is in the foreground so elapsed time
    // can be credited when the user switches away.

    fun activePackage(): String? = prefs.getString(KEY_ACTIVE_PACKAGE, null)

    fun activeSince(): Long = prefs.getLong(KEY_ACTIVE_SINCE, 0L)

    fun setActive(packageName: String?, since: Long) {
        prefs.edit()
            .apply {
                if (packageName == null) {
                    remove(KEY_ACTIVE_PACKAGE)
                    remove(KEY_ACTIVE_SINCE)
                } else {
                    putString(KEY_ACTIVE_PACKAGE, packageName)
                    putLong(KEY_ACTIVE_SINCE, since)
                }
            }
            .apply()
    }

    /**
     * Credits the time spent in the currently tracked app and stops tracking.
     *
     * Each credit is capped so a sleeping device (where the service stops getting
     * events) cannot inflate usage by hours when it wakes up.
     */
    fun commitActive(now: Long = System.currentTimeMillis()) {
        val pkg = prefs.getString(KEY_ACTIVE_PACKAGE, null) ?: return
        val since = prefs.getLong(KEY_ACTIVE_SINCE, 0L)
        val elapsed = (now - since).coerceIn(0L, MAX_SINGLE_CREDIT_MS)
        val key = usageKey(pkg)
        prefs.edit()
            .remove(KEY_ACTIVE_PACKAGE)
            .remove(KEY_ACTIVE_SINCE)
            .apply {
                if (elapsed > 0L) putLong(key, prefs.getLong(key, 0L) + elapsed)
            }
            .apply()
        pruneOldUsageIfNeeded()
    }

    private fun limitKey(packageName: String) = "$KEY_LIMIT_PREFIX$packageName"

    private fun usagePrefix() = "$USAGE_PREFIX${LocalDate.now()}_"

    private fun usageKey(packageName: String) = "${usagePrefix()}$packageName"

    private fun warnedKey(packageName: String) = "$WARNED_PREFIX${LocalDate.now()}_$packageName"

    /** Drops usage counters from previous days so the prefs file stays small. */
    private fun pruneOldUsageIfNeeded() {
        val today = LocalDate.now()
        if (lastPrunedDay == today) return
        lastPrunedDay = today
        val keepUsage = usagePrefix()
        val keepWarned = "$WARNED_PREFIX${today}_"
        val stale = prefs.all.keys.filter {
            (it.startsWith(USAGE_PREFIX) && !it.startsWith(keepUsage)) ||
                (it.startsWith(WARNED_PREFIX) && !it.startsWith(keepWarned))
        }
        if (stale.isEmpty()) return
        prefs.edit().apply {
            stale.forEach { remove(it) }
        }.apply()
    }

    companion object {
        const val PREFS_NAME = "frostlock_limits"
        private const val KEY_LIMIT_PREFIX = "limit_"
        private const val USAGE_PREFIX = "usage_"
        private const val WARNED_PREFIX = "warned_"
        private const val KEY_ACTIVE_PACKAGE = "active_package"
        private const val KEY_ACTIVE_SINCE = "active_since"

        /** Heads-up is sent once this share of the daily budget has been used. */
        const val WARN_FRACTION = 0.8f

        /** Longest single block of time credited to an app (covers service hiccups). */
        private const val MAX_SINGLE_CREDIT_MS = 60_000L

        /** Pure helper so the warning threshold is unit-testable. */
        fun shouldWarn(usedMillis: Long, limitMinutes: Int): Boolean {
            if (limitMinutes <= 0) return false
            val budget = limitMinutes * 60_000L
            return usedMillis >= budget * WARN_FRACTION && usedMillis < budget
        }
    }
}
