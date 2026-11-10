package com.focusflow.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [StreakEntity::class, ProfileEntity::class, SessionEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun streakDao(): StreakDao
    abstract fun profileDao(): ProfileDao
    abstract fun sessionDao(): SessionDao
}
