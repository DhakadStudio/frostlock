package com.frostlock.app.ui.screens.focus

import androidx.lifecycle.ViewModel
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.focus.FocusMode
import com.frostlock.app.focus.FocusSessionController
import com.frostlock.app.focus.FocusUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * The Focus tab's window onto the process-wide [FocusSessionController].
 *
 * All timer and session state lives in the controller, so this ViewModel can be
 * destroyed on a tab switch without interrupting a run.
 */
@HiltViewModel
class FocusViewModel @Inject constructor(
    private val controller: FocusSessionController
) : ViewModel() {

    val uiState: StateFlow<FocusUiState> = controller.uiState
    val lastSavedSession: StateFlow<SessionEntity?> = controller.lastSavedSession

    fun selectMode(mode: FocusMode) = controller.selectMode(mode)

    fun selectMinutes(minutes: Int) = controller.selectMinutes(minutes)

    fun selectTag(tag: String) = controller.selectTag(tag)

    fun setSubjectPackages(subjectName: String, packages: Set<String>) =
        controller.setSubjectPackages(subjectName, packages)

    fun setIntent(intent: String) = controller.setIntent(intent)

    fun setStrictMode(enabled: Boolean) = controller.setStrictMode(enabled)

    fun setMaxPauses(count: Int) = controller.setMaxPauses(count)

    fun setAmbience(key: String) = controller.setAmbience(key)

    fun toggleAmbience() = controller.toggleAmbience()

    fun refreshAccess() = controller.refreshAccess()

    fun startOrResume() = controller.startOrResume()

    fun pause() = controller.pause()

    fun stop() = controller.stop()

    fun discardSession() = controller.discardSession()

    fun endStrictSession() = controller.endStrictSession()

    fun skipBreak() = controller.skipBreak()

    fun showImmersive() = controller.showImmersive()

    fun minimize() = controller.minimize()
}
