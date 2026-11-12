package com.frostlock.app.ui.screens.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.local.db.SessionDao
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.ui.util.DiaryDay
import com.frostlock.app.ui.util.DiarySummary
import com.frostlock.app.ui.util.buildDiaryDays
import com.frostlock.app.ui.util.diarySummary
import com.frostlock.app.ui.util.matchesDiaryQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiaryUiState(
    val days: List<DiaryDay> = emptyList(),
    val summary: DiarySummary = DiarySummary(0, 0, 0L),
    val query: String = "",
    val onlyWithNotes: Boolean = true,
    val hasAnySessions: Boolean = false
) {
    val isEmpty: Boolean get() = days.isEmpty()
}

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val sessionDao: SessionDao
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val onlyWithNotes = MutableStateFlow(true)

    val uiState: StateFlow<DiaryUiState> = combine(
        sessionDao.recentSessions(1_000),
        query,
        onlyWithNotes
    ) { sessions, search, notesOnly ->
        val matching = sessions
            .filter { !notesOnly || it.note.isNotBlank() }
            .filter { it.matchesDiaryQuery(search) }

        DiaryUiState(
            days = buildDiaryDays(matching),
            summary = diarySummary(sessions),
            query = search,
            onlyWithNotes = notesOnly,
            hasAnySessions = sessions.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiaryUiState()
    )

    fun setQuery(value: String) {
        query.value = value
    }

    fun setOnlyWithNotes(value: Boolean) {
        onlyWithNotes.value = value
    }

    /** Rewrites a reflection from the diary without touching the session itself. */
    fun updateNote(id: Long, note: String) {
        val cleaned = note.trim().take(SessionEntity.MAX_NOTE_LENGTH)
        viewModelScope.launch(Dispatchers.IO) {
            sessionDao.updateNote(id, cleaned)
        }
    }
}
