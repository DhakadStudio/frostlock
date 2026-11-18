package com.frostlock.app.ui.screens.insights

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.SessionPhotoStore
import com.frostlock.app.data.local.db.SessionDao
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.ui.util.DiaryDay
import com.frostlock.app.ui.util.DiarySummary
import com.frostlock.app.ui.util.buildDiaryDays
import com.frostlock.app.ui.util.diarySummary
import com.frostlock.app.ui.util.matchesDiaryQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Notes only: the qualitative archive, never the analytics dashboard. */
data class ReflectionsUiState(
    val days: List<DiaryDay> = emptyList(),
    val summary: DiarySummary = DiarySummary(0, 0, 0L),
    val query: String = "",
    val hasAnySessions: Boolean = false
) {
    val isEmpty: Boolean get() = days.isEmpty()
}

@HiltViewModel
class ReflectionsViewModel @Inject constructor(
    private val sessionDao: SessionDao,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<ReflectionsUiState> = combine(
        sessionDao.recentSessions(1_000),
        query
    ) { sessions, search ->
        val reflections = sessions
            .filter { it.note.isNotBlank() || it.photoPath.isNotBlank() }
            .filter { it.matchesDiaryQuery(search) }

        ReflectionsUiState(
            days = buildDiaryDays(reflections),
            summary = diarySummary(sessions),
            query = search,
            hasAnySessions = sessions.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReflectionsUiState()
    )

    fun setQuery(value: String) {
        query.value = value
    }

    /** Rewrites a reflection without touching the session itself. */
    fun updateNote(id: Long, note: String) {
        val cleaned = note.trim().take(SessionEntity.MAX_NOTE_LENGTH)
        viewModelScope.launch(Dispatchers.IO) {
            sessionDao.updateNote(id, cleaned)
        }
    }

    /** Attaches or replaces the local photo for a past session. */
    fun attachPhoto(id: Long, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionDao.byId(id) ?: return@launch
            val name = SessionPhotoStore.save(appContext, id, uri) ?: return@launch
            sessionDao.updatePhoto(id, name)
        }
    }

    /** Removes and deletes a past session's photo. */
    fun removePhoto(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionDao.byId(id) ?: return@launch
            sessionDao.updatePhoto(id, "")
            SessionPhotoStore.delete(appContext, session.photoPath)
        }
    }
}
