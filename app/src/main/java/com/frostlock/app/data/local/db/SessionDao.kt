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

    /** Sessions whose wall-clock span overlaps a local date range. */
    @Query("SELECT * FROM focus_sessions WHERE startedAt <= :end AND endedAt >= :start ORDER BY endedAt DESC")
    fun sessionsOverlapping(start: Long, end: Long): Flow<List<SessionEntity>>

    /** Sessions that ended inside a local date range. */
    @Query("SELECT * FROM focus_sessions WHERE endedAt >= :start AND endedAt <= :end ORDER BY endedAt DESC")
    fun sessionsEndingBetween(start: Long, end: Long): Flow<List<SessionEntity>>

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

    @Query("UPDATE focus_sessions SET photoPath = :photoPath WHERE id = :id")
    fun updatePhoto(id: Long, photoPath: String)

    @Query("SELECT * FROM focus_sessions WHERE id = :id LIMIT 1")
    fun byId(id: Long): SessionEntity?

    @Query("DELETE FROM focus_sessions WHERE id = :id")
    fun deleteById(id: Long)
}
