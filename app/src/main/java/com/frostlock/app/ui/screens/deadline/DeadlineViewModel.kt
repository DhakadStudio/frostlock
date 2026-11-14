package com.frostlock.app.ui.screens.deadline

import androidx.lifecycle.ViewModel
import com.frostlock.app.data.local.prefs.DeadlineStore
import com.frostlock.app.data.local.prefs.FocusDeadline
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DeadlineViewModel @Inject constructor(
    private val store: DeadlineStore
) : ViewModel() {

    val deadline: StateFlow<FocusDeadline?> = store.deadline

    fun save(title: String, isoDate: String) = store.set(title, isoDate)

    fun clear() = store.clear()
}
