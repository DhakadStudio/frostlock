package com.focusflow.app.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusflow.app.data.InstalledApp
import com.focusflow.app.data.InstalledAppsRepository
import com.focusflow.app.data.local.prefs.FocusSchedule
import com.focusflow.app.data.local.prefs.ScheduleStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val store: ScheduleStore,
    private val installedApps: InstalledAppsRepository
) : ViewModel() {

    private val _schedules = MutableStateFlow(store.schedules())
    val schedules: StateFlow<List<FocusSchedule>> = _schedules.asStateFlow()

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps.asStateFlow()

    private val _activeIds = MutableStateFlow<Set<Long>>(emptySet())
    val activeIds: StateFlow<Set<Long>> = _activeIds.asStateFlow()

    init {
        viewModelScope.launch { _apps.value = installedApps.apps() }
        refreshActive()
    }

    fun save(schedule: FocusSchedule) {
        store.upsert(schedule)
        refresh()
    }

    fun delete(id: Long) {
        store.delete(id)
        refresh()
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        val schedule = store.schedules().firstOrNull { it.id == id } ?: return
        store.upsert(schedule.copy(enabled = enabled))
        refresh()
    }

    /** Recomputes which rule is running now; call when the screen resumes. */
    fun refreshActive() {
        _activeIds.value = store.activeSchedules().map { it.schedule.id }.toSet()
    }

    private fun refresh() {
        _schedules.value = store.schedules()
        refreshActive()
    }
}
