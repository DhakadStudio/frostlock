package com.frostlock.app.ui.session

import androidx.lifecycle.ViewModel
import com.frostlock.app.focus.FocusSessionController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * The shell's window onto the process-wide session controller.
 *
 * It lives as long as the activity, so the immersive dialog and the
 * running-session bar keep working across tab switches and screen recreation.
 */
@HiltViewModel
class FocusSessionHostViewModel @Inject constructor(
    val controller: FocusSessionController
) : ViewModel() {

    val uiState = controller.uiState
    val pendingSummaryId = controller.pendingSummaryId

    fun showImmersive() = controller.showImmersive()

    fun minimize() = controller.minimize()

    fun pause() = controller.pause()

    fun startOrResume() = controller.startOrResume()

    fun stop() = controller.stop()

    fun discardSession() = controller.discardSession()

    fun endStrictSession() = controller.endStrictSession()

    fun skipBreak() = controller.skipBreak()

    fun toggleAmbience() = controller.toggleAmbience()

    fun consumeSummary() = controller.consumeSummary()
}
