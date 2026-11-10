package com.focusflow.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A weekly window during which the blocked apps stay locked.
 *
 * [days] uses [java.time.DayOfWeek] values (1 = Monday … 7 = Sunday).
 * Times are minutes from midnight. When [startMinutes] is greater than
 * [endMinutes] the window runs overnight into the next day.
 */
data class FocusSchedule(
    val id: Long,
    val label: String,
    val days: Set<Int>,
    val startMinutes: Int,
    val endMinutes: Int,
    val enabled: Boolean = true
)

/** A schedule that is running right now, plus the instant its window closes. */
data class ActiveSchedule(
    val schedule: FocusSchedule,
    val endsAtMillis: Long
)

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
                    val daysJson = obj.optJSONArray(KEY_DAYS) ?: JSONArray()
                    val days = buildSet {
                        for (d in 0 until daysJson.length()) add(daysJson.optInt(d))
                    }.filter { it in 1..7 }.toSet()
                    add(
                        FocusSchedule(
                            id = obj.optLong(KEY_ID),
                            label = obj.optString(KEY_LABEL, "Focus"),
                            days = days,
                            startMinutes = obj.optInt(KEY_START),
                            endMinutes = obj.optInt(KEY_END),
                            enabled = obj.optBoolean(KEY_ENABLED, true)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(schedules: List<FocusSchedule>) {
        val array = JSONArray()
        schedules.forEach { schedule ->
            array.put(
                JSONObject().apply {
                    put(KEY_ID, schedule.id)
                    put(KEY_LABEL, schedule.label)
                    put(KEY_DAYS, JSONArray(schedule.days.sorted()))
                    put(KEY_START, schedule.startMinutes)
                    put(KEY_END, schedule.endMinutes)
                    put(KEY_ENABLED, schedule.enabled)
                }
            )
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

    /** The first enabled schedule whose window contains [now], if any. */
    fun activeSchedule(now: ZonedDateTime = ZonedDateTime.now()): ActiveSchedule? {
        val minuteOfDay = now.hour * 60 + now.minute
        val today = now.dayOfWeek.value
        val yesterday = now.minusDays(1).dayOfWeek.value

        for (schedule in schedules()) {
            if (!schedule.enabled || schedule.days.isEmpty()) continue
            if (schedule.startMinutes == schedule.endMinutes) continue

            val active: Boolean
            val endsAt: ZonedDateTime
            if (schedule.startMinutes < schedule.endMinutes) {
                active = today in schedule.days &&
                    minuteOfDay >= schedule.startMinutes &&
                    minuteOfDay < schedule.endMinutes
                endsAt = atTime(now, schedule.endMinutes)
            } else {
                // Overnight window, e.g. 22:00 – 02:00.
                active = (today in schedule.days && minuteOfDay >= schedule.startMinutes) ||
                    (yesterday in schedule.days && minuteOfDay < schedule.endMinutes)
                endsAt = if (minuteOfDay < schedule.endMinutes) {
                    atTime(now, schedule.endMinutes)
                } else {
                    atTime(now.plusDays(1), schedule.endMinutes)
                }
            }

            if (active) {
                return ActiveSchedule(schedule, endsAt.toInstant().toEpochMilli())
            }
        }
        return null
    }

    private fun atTime(day: ZonedDateTime, minutes: Int): ZonedDateTime =
        day.toLocalDate()
            .atTime(minutes / 60, minutes % 60)
            .atZone(day.zone)

    companion object {
        const val PREFS_NAME = "focusflow_schedules"
        private const val KEY_SCHEDULES = "schedules"
        private const val KEY_ID = "id"
        private const val KEY_LABEL = "label"
        private const val KEY_DAYS = "days"
        private const val KEY_START = "start"
        private const val KEY_END = "end"
        private const val KEY_ENABLED = "enabled"
    }
}
