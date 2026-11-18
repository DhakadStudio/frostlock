package com.frostlock.app.insights

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Injectable time source so insight tests can freeze "today" and the zone. */
interface Clock {
    fun now(): Instant
    fun zone(): ZoneId
    fun today(): LocalDate = now().atZone(zone()).toLocalDate()
}

@Singleton
class SystemClock @Inject constructor() : Clock {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
}

/**
 * Emits the current local date. Call [refresh] on resume and from the
 * date/time-change receiver so an app left open overnight stops showing
 * yesterday as "Today".
 */
@Singleton
class TodayProvider @Inject constructor(private val clock: Clock) {

    private val _today = MutableStateFlow(clock.today())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    fun refresh() {
        _today.value = clock.today()
    }
}
