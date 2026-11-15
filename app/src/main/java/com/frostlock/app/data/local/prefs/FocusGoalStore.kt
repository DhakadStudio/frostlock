package com.frostlock.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A local focus goal — an exam, a project or a personal target. Goals store only
 * a name, an ISO target date and a focus-minute target; progress is derived from
 * the sessions already in the database, so there is nothing to sync or upload.
 */
data class FocusGoal(
    val id: Long,
    val name: String,
    /** ISO `yyyy-MM-dd`, so the target survives timezone changes. */
    val targetDate: String,
    val targetMinutes: Int,
    /** Epoch millis when the goal was created; sessions before it do not count. */
    val createdAt: Long
) {
    companion object {
        const val MAX_NAME_LENGTH = 40
        const val MIN_TARGET_MINUTES = 30
        const val MAX_TARGET_MINUTES = 60 * 500
    }
}

@Singleton
class FocusGoalStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _goals = MutableStateFlow(read())
    val goals: StateFlow<List<FocusGoal>> = _goals.asStateFlow()

    /** Adds a goal and returns it, or null when the input is invalid. */
    fun add(name: String, isoDate: String, targetMinutes: Int): FocusGoal? {
        val cleaned = cleanName(name) ?: return null
        if (runCatching { LocalDate.parse(isoDate) }.isFailure) return null
        val nextId = (_goals.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val goal = FocusGoal(
            id = nextId,
            name = cleaned,
            targetDate = isoDate,
            targetMinutes = clampTarget(targetMinutes),
            createdAt = System.currentTimeMillis()
        )
        write(_goals.value + goal)
        return goal
    }

    /** Updates an existing goal, preserving its id and creation time. */
    fun update(id: Long, name: String, isoDate: String, targetMinutes: Int) {
        val cleaned = cleanName(name) ?: return
        if (runCatching { LocalDate.parse(isoDate) }.isFailure) return
        val updated = _goals.value.map { goal ->
            if (goal.id == id) {
                goal.copy(
                    name = cleaned,
                    targetDate = isoDate,
                    targetMinutes = clampTarget(targetMinutes)
                )
            } else {
                goal
            }
        }
        write(updated)
    }

    fun remove(id: Long) {
        write(_goals.value.filterNot { it.id == id })
    }

    private fun cleanName(name: String): String? {
        val cleaned = name.trim().take(FocusGoal.MAX_NAME_LENGTH)
        return cleaned.takeIf { it.isNotBlank() }
    }

    private fun clampTarget(minutes: Int): Int =
        minutes.coerceIn(FocusGoal.MIN_TARGET_MINUTES, FocusGoal.MAX_TARGET_MINUTES)

    private fun write(goals: List<FocusGoal>) {
        val sorted = goals.sortedWith(compareBy({ it.targetDate }, { it.name }))
        val array = JSONArray()
        sorted.forEach { goal ->
            array.put(
                JSONObject().apply {
                    put(KEY_ID, goal.id)
                    put(KEY_NAME, goal.name)
                    put(KEY_DATE, goal.targetDate)
                    put(KEY_TARGET, goal.targetMinutes)
                    put(KEY_CREATED, goal.createdAt)
                }
            )
        }
        prefs.edit().putString(KEY_GOALS, array.toString()).apply()
        _goals.value = sorted
    }

    private fun read(): List<FocusGoal> {
        val raw = prefs.getString(KEY_GOALS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val obj = array.optJSONObject(index) ?: return@mapNotNull null
                val date = obj.optString(KEY_DATE)
                if (runCatching { LocalDate.parse(date) }.isFailure) return@mapNotNull null
                val name = obj.optString(KEY_NAME).trim()
                if (name.isBlank()) return@mapNotNull null
                FocusGoal(
                    id = obj.optLong(KEY_ID),
                    name = name.take(FocusGoal.MAX_NAME_LENGTH),
                    targetDate = date,
                    targetMinutes = clampTarget(obj.optInt(KEY_TARGET)),
                    createdAt = obj.optLong(KEY_CREATED)
                )
            }.sortedWith(compareBy({ it.targetDate }, { it.name }))
        }.getOrDefault(emptyList())
    }

    companion object {
        const val PREFS_NAME = "frostlock_focus_goals"
        private const val KEY_GOALS = "goals"
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_DATE = "date"
        private const val KEY_TARGET = "targetMinutes"
        private const val KEY_CREATED = "createdAt"
    }
}
