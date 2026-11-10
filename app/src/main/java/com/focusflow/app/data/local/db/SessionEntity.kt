package com.focusflow.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single focus session record. [mode] stores the enum name of the timer mode
 * (TIMER, POMODORO, STOPWATCH) as a plain string so the data layer stays independent
 * of UI code.
 */
@Entity(tableName = "focus_sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val mode: String,
    val plannedSeconds: Int,
    val durationSeconds: Int,
    val startedAt: Long,
    val endedAt: Long,
    val completed: Boolean
)
