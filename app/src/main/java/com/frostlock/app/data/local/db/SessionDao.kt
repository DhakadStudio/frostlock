package com.frostlock.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert
    fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    fun recentSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC LIMIT :limit")
    fun recentSessions(limit: Int): Flow<List<SessionEntity>>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM focus_sessions")
    fun totalSeconds(): Flow<Long>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM focus_sessions WHERE startedAt >= :since")
    fun totalSecondsSince(since: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM focus_sessions")
    fun totalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE completed = 1")
    fun completedCount(): Flow<Int>

    @Query("UPDATE focus_sessions SET note = :note WHERE id = :id")
    fun updateNote(id: Long, note: String)

    @Query("DELETE FROM focus_sessions WHERE id = :id")
    fun deleteById(id: Long)
}
