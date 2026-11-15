package com.frostlock.app.data.local.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Counts device unlocks locally. The accessibility service listens for
 * ACTION_USER_PRESENT while it is running, so no usage-access permission is
 * needed. The count rolls over at midnight.
 */
@Singleton
class UnlockStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _todayCount = MutableStateFlow(readTodayCount())
    val todayCount: StateFlow<Int> = _todayCount.asStateFlow()

    fun recordUnlock(today: LocalDate = LocalDate.now()) {
        val key = today.toString()
        val storedDate = prefs.getString(KEY_DATE, null)
        val storedCount = prefs.getInt(KEY_COUNT, 0)
        val next = nextCount(storedDate, storedCount, key)
        prefs.edit()
            .putString(KEY_DATE, key)
            .putInt(KEY_COUNT, next)
            .apply()
        _todayCount.value = next
    }

    fun readTodayCount(today: LocalDate = LocalDate.now()): Int {
        val key = today.toString()
        return if (prefs.getString(KEY_DATE, null) == key) prefs.getInt(KEY_COUNT, 0) else 0
    }

    companion object {
        const val PREFS_NAME = "frostlock_unlocks"
        const val KEY_DATE = "unlock_date"
        const val KEY_COUNT = "unlock_count"

        fun nextCount(storedDate: String?, storedCount: Int, today: String): Int =
            if (storedDate == today) storedCount + 1 else 1
    }
}
