package com.frostlock.app.ui.screens.focus

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.SessionPhotoStore
import com.frostlock.app.data.local.db.SessionDao
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the session summary screen shows. */
data class SessionSummaryUiState(
    val loading: Boolean = true,
    val session: SessionEntity? = null,
    val streakDays: Int = 0,
    val note: String = "",
    val noteDirty: Boolean = false,
    val busy: Boolean = false,
    val finished: Boolean = false
) {
    /** True when the route points at a session that no longer exists. */
    val missing: Boolean get() = !loading && session == null
}

/**
 * Loads one saved session and owns the closing actions: note, photo, save and
 * discard.
 *
 * The session is already in the database when this screen opens, so a process
 * death never loses the work. Discarding deletes it; saving only updates it.
 */
@HiltViewModel
class SessionSummaryViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sessionDao: SessionDao,
    private val focusSettings: FocusSettings,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = savedStateHandle[SESSION_ID_ARG] ?: -1L

    private val _uiState = MutableStateFlow(SessionSummaryUiState())
    val uiState: StateFlow<SessionSummaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = withContext(Dispatchers.IO) { sessionDao.byId(sessionId) }
            val streak = if (session != null) {
                withContext(Dispatchers.IO) {
                    computeStreakForThisDevice()
                }
            } else {
                0
            }
            _uiState.value = SessionSummaryUiState(
                loading = false,
                session = session,
                streakDays = streak,
                note = session?.note.orEmpty()
            )
        }
    }

    fun onNoteChange(note: String) {
        val cleaned = note.take(SessionEntity.MAX_NOTE_LENGTH)
        _uiState.update { it.copy(note = cleaned, noteDirty = true) }
    }

    /** Saves the note and closes; the intention is cleared because the work is done. */
    fun saveAndEnd() = close(clearIntent = true)

    /** Closes without clearing the intention; the note is still saved. */
    fun done() = close(clearIntent = false)

    /** Deletes the session, its photo and keeps the intention for the next attempt. */
    fun discard() {
        val session = _uiState.value.session
        if (session == null) {
            _uiState.update { it.copy(finished = true) }
            return
        }
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                sessionDao.deleteById(session.id)
                SessionPhotoStore.delete(appContext, session.photoPath)
            }
            WidgetUpdater.refreshAsync(appContext)
            _uiState.update { it.copy(busy = false, finished = true) }
        }
    }

    fun attachPhoto(uri: Uri) {
        val session = _uiState.value.session ?: return
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) {
                SessionPhotoStore.save(appContext, session.id, uri)
            }
            if (name != null) {
                withContext(Dispatchers.IO) { sessionDao.updatePhoto(session.id, name) }
                _uiState.update {
                    it.copy(
                        busy = false,
                        session = it.session?.copy(photoPath = name)
                    )
                }
            } else {
                _uiState.update { it.copy(busy = false) }
            }
        }
    }

    fun removePhoto() {
        val session = _uiState.value.session ?: return
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                sessionDao.updatePhoto(session.id, "")
                SessionPhotoStore.delete(appContext, session.photoPath)
            }
            _uiState.update {
                it.copy(
                    busy = false,
                    session = it.session?.copy(photoPath = "")
                )
            }
        }
    }

    private fun close(clearIntent: Boolean) {
        val state = _uiState.value
        if (state.busy) return
        val session = state.session
        if (session == null) {
            _uiState.update { it.copy(finished = true) }
            return
        }
        _uiState.update { it.copy(busy = true) }
        viewModelScope.launch {
            val note = state.note.trim().take(SessionEntity.MAX_NOTE_LENGTH)
            if (state.noteDirty || note != session.note) {
                withContext(Dispatchers.IO) { sessionDao.updateNote(session.id, note) }
            }
            if (clearIntent) focusSettings.setSessionIntent("")
            WidgetUpdater.refreshAsync(appContext)
            _uiState.update { it.copy(busy = false, finished = true) }
        }
    }

    private suspend fun computeStreakForThisDevice(): Int {
        val starts = sessionDao.recentSessions(500).first().map { it.startedAt }
        return com.frostlock.app.ui.util.computeStreak(starts)
    }

    companion object {
        const val SESSION_ID_ARG = "sessionId"
    }
}
