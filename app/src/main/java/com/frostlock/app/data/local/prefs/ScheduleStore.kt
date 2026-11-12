package com.frostlock.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A blocking rule during which its own set of apps stays locked.
 *
 * Two ways to scope a rule:
 * - [days] repeats weekly (uses [java.time.DayOfWeek] values, 1 = Monday … 7 = Sunday).
 * - [dates] runs once on each specific calendar date (ISO-8601, e.g. `2026-11-20`).
 *   When [dates] is non-empty it wins over [days], which is what "one-off" means.
 *
 * [startMinutes] / [endMinutes] are minutes from midnight. When start > end the
 * window runs overnight into the next day.
 *
 * [packages] is the app set this rule locks. Rules are independent of each other,
 * so two rules can block different apps at different times. An empty set means
 * "fall back to the global focus-session app list" (kept for older saved rules).
 */
data class FocusSchedule(
    val id: Long,
    val label: String,
    val days: Set<Int> = emptySet(),
    val dates: Set<String> = emptySet(),
    val startMinutes: Int,
    val endMinutes: Int,
    val packages: Set<String> = emptySet(),
    val enabled: Boolean = true
)

/** A schedule that is running right now, plus the instant its window closes. */
data class ActiveSchedule(
    val schedule: FocusSchedule,
    val endsAtMillis: Long
)

/** True when this rule is scoped to [date], by explicit date or by weekday. */
fun FocusSchedule.appliesOn(date: LocalDate): Boolean =
    if (dates.isNotEmpty()) date.toString() in dates else date.dayOfWeek.value in days

/**
 * True when this rule locks [packageName]. An empty app set falls back to
 * [fallbackPackages] (the focus-session list) so rules saved before per-rule
 * apps existed keep working.
 */
fun FocusSchedule.blocks(packageName: String, fallbackPackages: Set<String>): Boolean =
    packageName in packages.ifEmpty { fallbackPackages }

/** True while [now] falls inside this rule's window. */
fun FocusSchedule.isActiveAt(now: ZonedDateTime): Boolean {
    if (!enabled || startMinutes == endMinutes) return false
    val minute = now.hour * 60 + now.minute
    val overnight = startMinutes > endMinutes
    return if (!overnight) {
        appliesOn(now.toLocalDate()) && minute >= startMinutes && minute < endMinutes
    } else {
        (appliesOn(now.toLocalDate()) && minute >= startMinutes) ||
            (appliesOn(now.minusDays(1).toLocalDate()) && minute < endMinutes)
    }
}

/** Wall-clock instant this rule's current (or next) window closes. */
fun FocusSchedule.endsAt(now: ZonedDateTime): ZonedDateTime {
    val minute = now.hour * 60 + now.minute
    val overnight = startMinutes > endMinutes
    val anchor = if (overnight && minute >= startMinutes) now.plusDays(1) else now
    return anchor.toLocalDate()
        .atTime(endMinutes / 60, endMinutes % 60)
        .atZone(now.zone)
}

/**
 * Weekly schedules are small and read by the accessibility service on every
 * window change, so they live in SharedPreferences as a JSON array.
 */
@Singleton
class ScheduleStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun schedules(): List<FocusSchedule> {
        val raw = prefs.getString(KEY_SCHEDULES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(obj.toSchedule())
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(schedules: List<FocusSchedule>) {
        val array = JSONArray()
        schedules.forEach { schedule ->
            array.put(schedule.toJson())
        }
        prefs.edit().putString(KEY_SCHEDULES, array.toString()).apply()
    }

    fun upsert(schedule: FocusSchedule) {
        val current = schedules().toMutableList()
        val index = current.indexOfFirst { it.id == schedule.id }
        if (index >= 0) current[index] = schedule else current.add(schedule)
        save(current)
    }

    fun delete(id: Long) {
        save(schedules().filterNot { it.id == id })
    }

    /**
     * Every rule running at [now]. Several rules can run at once (for example
     * "Study" and "Work" with different app sets), so callers must check all of
     * them instead of only the first.
     */
    fun activeSchedules(now: ZonedDateTime = ZonedDateTime.now()): List<ActiveSchedule> =
        schedules()
            .filter { it.isActiveAt(now) }
            .map { ActiveSchedule(it, it.endsAt(now).toInstant().toEpochMilli()) }

    private fun JSONObject.toSchedule(): FocusSchedule {
        val daysJson = optJSONArray(KEY_DAYS) ?: JSONArray()
        val days = buildSet {
            for (d in 0 until daysJson.length()) add(daysJson.optInt(d))
        }.filter { it in 1..7 }.toSet()

        val datesJson = optJSONArray(KEY_DATES) ?: JSONArray()
        val dates = buildSet {
            for (d in 0 until datesJson.length()) {
                datesJson.optString(d).takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }

        val packagesJson = optJSONArray(KEY_PACKAGES) ?: JSONArray()
        val packages = buildSet {
            for (p in 0 until packagesJson.length()) {
                packagesJson.optString(p).takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }

        return FocusSchedule(
            id = optLong(KEY_ID),
            label = optString(KEY_LABEL, "Focus"),
            days = days,
            dates = dates,
            startMinutes = optInt(KEY_START),
            endMinutes = optInt(KEY_END),
            packages = packages,
            enabled = optBoolean(KEY_ENABLED, true)
        )
    }

    private fun FocusSchedule.toJson(): JSONObject = JSONObject().apply {
        put(KEY_ID, id)
        put(KEY_LABEL, label)
        put(KEY_DAYS, JSONArray(days.sorted()))
        put(KEY_DATES, JSONArray(dates.sorted()))
        put(KEY_START, startMinutes)
        put(KEY_END, endMinutes)
        put(KEY_PACKAGES, JSONArray(packages.sorted()))
        put(KEY_ENABLED, enabled)
    }

    companion object {
        const val PREFS_NAME = "frostlock_schedules"
        private const val KEY_SCHEDULES = "schedules"
        private const val KEY_ID = "id"
        private const val KEY_LABEL = "label"
        private const val KEY_DAYS = "days"
        private const val KEY_DATES = "dates"
        private const val KEY_PACKAGES = "packages"
        private const val KEY_START = "start"
        private const val KEY_END = "end"
        private const val KEY_ENABLED = "enabled"
    }
}
