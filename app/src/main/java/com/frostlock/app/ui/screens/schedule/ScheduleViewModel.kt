package com.frostlock.app.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostlock.app.data.InstalledApp
import com.frostlock.app.data.InstalledAppsRepository
import com.frostlock.app.data.local.db.SessionDao
import com.frostlock.app.data.local.prefs.FocusSchedule
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.data.local.prefs.FocusSubject
import com.frostlock.app.data.local.prefs.ScheduleStore
import com.frostlock.app.data.local.prefs.SubjectStore
import com.frostlock.app.reminder.ScheduleReminderScheduler
import com.frostlock.app.ui.util.computeScheduleStreak
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val store: ScheduleStore,
    private val subjectStore: SubjectStore,
    private val installedApps: InstalledAppsRepository,
    private val focusSettings: FocusSettings,
    private val reminders: ScheduleReminderScheduler,
    private val sessionDao: SessionDao
) : ViewModel() {

    val remindersEnabled: StateFlow<Boolean> = focusSettings.scheduleRemindersEnabled

    private val _schedules = MutableStateFlow(store.schedules())
    val schedules: StateFlow<List<FocusSchedule>> = _schedules.asStateFlow()

    private val _subjects = MutableStateFlow(subjectStore.subjects())
    val subjects: StateFlow<List<FocusSubject>> = _subjects.asStateFlow()

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps.asStateFlow()

    private val _activeIds = MutableStateFlow<Set<Long>>(emptySet())
    val activeIds: StateFlow<Set<Long>> = _activeIds.asStateFlow()

    private val _scheduleStreak = MutableStateFlow(0)
    val scheduleStreak: StateFlow<Int> = _scheduleStreak.asStateFlow()

    init {
        viewModelScope.launch { _apps.value = installedApps.apps() }
        refreshActive()
        refreshStreak()
        reminders.rescheduleAll()
    }

    fun save(schedule: FocusSchedule) {
        store.upsert(schedule)
        refresh()
        reminders.rescheduleAll()
    }

    fun delete(id: Long) {
        store.delete(id)
        reminders.cancel(id)
        refresh()
        reminders.rescheduleAll()
    }

    fun setEnabled(id: Long, enabled: Boolean) {
        val schedule = store.schedules().firstOrNull { it.id == id } ?: return
        store.upsert(schedule.copy(enabled = enabled))
        refresh()
        reminders.rescheduleAll()
    }

    fun setRemindersEnabled(enabled: Boolean) {
        focusSettings.setScheduleRemindersEnabled(enabled)
        reminders.rescheduleAll()
    }

    /** Recomputes which rule is running now; call when the screen resumes. */
    fun refreshActive() {
        _activeIds.value = store.activeSchedules().map { it.schedule.id }.toSet()
    }

    /** Recounts the schedule streak from saved sessions; call on resume. */
    fun refreshStreak() {
        viewModelScope.launch {
            val starts = sessionDao.recentSessions(500).first().map { it.startedAt }
            _scheduleStreak.value = computeScheduleStreak(_schedules.value, starts)
        }
    }

    /** Picks up subject app lists edited from the Focus screen. */
    fun refreshSubjects() {
        _subjects.value = subjectStore.subjects()
    }

    private fun refresh() {
        _schedules.value = store.schedules()
        refreshSubjects()
        refreshActive()
        refreshStreak()
    }
}
