package com.frostlock.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A local target date — an exam, a launch, a deadline — shown as a countdown.
 *
 * Deliberately just a label and an ISO date: no calendar sync, no account and
 * no network. The date is stored as `yyyy-MM-dd` so it survives timezone changes.
 */
data class FocusDeadline(
    val title: String,
    val date: String
) {
    companion object {
        const val MAX_TITLE_LENGTH = 40
    }
}

@Singleton
class DeadlineStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _deadline = MutableStateFlow(read())
    val deadline: StateFlow<FocusDeadline?> = _deadline.asStateFlow()

    fun set(title: String, isoDate: String) {
        val cleaned = title.trim().take(FocusDeadline.MAX_TITLE_LENGTH)
        if (cleaned.isBlank()) return
        if (runCatching { LocalDate.parse(isoDate) }.isFailure) return
        prefs.edit()
            .putString(KEY_TITLE, cleaned)
            .putString(KEY_DATE, isoDate)
            .apply()
        _deadline.value = FocusDeadline(cleaned, isoDate)
    }

    fun clear() {
        prefs.edit().remove(KEY_TITLE).remove(KEY_DATE).apply()
        _deadline.value = null
    }

    private fun read(): FocusDeadline? {
        val title = prefs.getString(KEY_TITLE, null)?.trim().orEmpty()
        val date = prefs.getString(KEY_DATE, null) ?: return null
        if (title.isBlank()) return null
        if (runCatching { LocalDate.parse(date) }.isFailure) return null
        return FocusDeadline(title, date)
    }

    companion object {
        const val PREFS_NAME = "frostlock_deadline"
        private const val KEY_TITLE = "deadline_title"
        private const val KEY_DATE = "deadline_date"
    }
}
