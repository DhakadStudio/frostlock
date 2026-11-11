package com.focusflow.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val startTime: Long,
    val endTime: Long?
)
