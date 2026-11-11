package com.focusflow.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [StreakEntity::class, ProfileEntity::class, SessionEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun streakDao(): StreakDao
    abstract fun profileDao(): ProfileDao
    abstract fun sessionDao(): SessionDao

    companion object {
        /** Adds the optional subject tag that powers per-tag insights. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE focus_sessions ADD COLUMN tag TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}
