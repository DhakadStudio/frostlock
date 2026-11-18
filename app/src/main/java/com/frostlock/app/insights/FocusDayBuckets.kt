package com.frostlock.app.insights

import com.frostlock.app.data.local.db.SessionEntity
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * The one canonical way FrostLock turns saved sessions into local days.
 *
 * Rules:
 * - focus duration is split across local midnights;
 * - a session counts once, on the day it ended;
 * - streak and active-day logic use days with duration > 0;
 * - all maths uses [ZoneId] and [Instant], never fixed 86,400-second days.
 *
 * Every screen, chart and widget must use this helper. Grouping raw `startedAt`
 * or `endedAt` anywhere else is a bug.
 */
object FocusDayBuckets {

    /** Buckets every saved session into local days, oldest first. */
    fun build(
        sessions: List<SessionEntity>,
        zone: ZoneId = ZoneId.systemDefault()
    ): Map<LocalDate, DayFocus> {
        val secondsByDate = mutableMapOf<LocalDate, Long>()
        val sessionsByDate = mutableMapOf<LocalDate, Int>()

        sessions.forEach { session ->
            if (session.durationSeconds <= 0) return@forEach
            split(session, zone).forEach { (date, seconds) ->
                secondsByDate[date] = (secondsByDate[date] ?: 0L) + seconds
            }
            val endDate = countDate(session, zone)
            sessionsByDate[endDate] = (sessionsByDate[endDate] ?: 0) + 1
        }

        return (secondsByDate.keys + sessionsByDate.keys)
            .associateWith { date ->
                DayFocus(
                    date = date,
                    seconds = secondsByDate[date] ?: 0L,
                    sessions = sessionsByDate[date] ?: 0
                )
            }
            .toSortedMap()
    }

    /**
     * Splits one session's recorded duration across the local days it touches.
     *
     * The wall-clock span decides the proportions, then the recorded duration is
     * distributed over those proportions so pauses do not inflate the total. The
     * final slice absorbs rounding remainder, which keeps the sum exact.
     */
    fun split(session: SessionEntity, zone: ZoneId): List<Pair<LocalDate, Long>> {
        val duration = session.durationSeconds.toLong()
        if (duration <= 0L) return emptyList()

        val start = Instant.ofEpochMilli(session.startedAt)
        val end = Instant.ofEpochMilli(session.endedAt)
        val endDate = countDate(session, zone)
        if (!end.isAfter(start)) return listOf(endDate to duration)

        val slices = mutableListOf<Pair<LocalDate, Long>>()
        var cursor = start
        while (cursor.isBefore(end)) {
            val date = cursor.atZone(zone).toLocalDate()
            val nextMidnight = date.plusDays(1).atStartOfDay(zone).toInstant()
            val sliceEnd = if (nextMidnight.isBefore(end)) nextMidnight else end
            val millis = Duration.between(cursor, sliceEnd).toMillis()
            if (millis > 0L) slices += date to millis
            cursor = sliceEnd
        }
        if (slices.isEmpty()) return listOf(endDate to duration)

        val wallMillis = slices.sumOf { it.second }
        if (wallMillis <= 0L) return listOf(endDate to duration)

        var allocated = 0L
        return slices.mapIndexed { index, (date, millis) ->
            val part = if (index == slices.lastIndex) {
                duration - allocated
            } else {
                ((millis.toDouble() / wallMillis) * duration).toLong().also {
                    allocated += it
                }
            }
            date to part
        }.filter { it.second > 0L }
    }

    /** Aggregates a closed local date range, including zero-value future days. */
    fun period(
        buckets: Map<LocalDate, DayFocus>,
        start: LocalDate,
        end: LocalDate,
        goalSeconds: Long
    ): PeriodFocus {
        val days = generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .map { date -> buckets[date] ?: DayFocus(date) }
            .toList()

        return PeriodFocus(
            start = start,
            end = end,
            seconds = days.sumOf { it.seconds },
            sessions = days.sumOf { it.sessions },
            activeDays = days.count { it.hasFocus },
            goalDays = if (goalSeconds <= 0L) 0 else days.count { it.seconds >= goalSeconds },
            days = days
        )
    }

    /** Consecutive focus days ending today, or yesterday when today is still empty. */
    fun currentStreak(buckets: Map<LocalDate, DayFocus>, today: LocalDate): Int {
        var cursor = when {
            buckets[today]?.hasFocus == true -> today
            buckets[today.minusDays(1)]?.hasFocus == true -> today.minusDays(1)
            else -> return 0
        }
        var streak = 0
        while (buckets[cursor]?.hasFocus == true) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /** Longest run of consecutive focus days in the whole history. */
    fun bestStreak(buckets: Map<LocalDate, DayFocus>): Int {
        val dates = buckets.values.filter { it.hasFocus }.map { it.date }.sorted()
        if (dates.isEmpty()) return 0

        var best = 1
        var run = 1
        for (index in 1 until dates.size) {
            if (dates[index] == dates[index - 1].plusDays(1)) {
                run++
                best = maxOf(best, run)
            } else {
                run = 1
            }
        }
        return best
    }

    /** Month rollup used by the rhythm chart and History. */
    fun month(
        buckets: Map<LocalDate, DayFocus>,
        month: YearMonth
    ): MonthFocus {
        val days = (1..month.lengthOfMonth()).map { day ->
            buckets[month.atDay(day)] ?: DayFocus(month.atDay(day))
        }
        return MonthFocus(
            month = month,
            seconds = days.sumOf { it.seconds },
            sessions = days.sumOf { it.sessions },
            activeDays = days.count { it.hasFocus },
            bestDaySeconds = days.maxOfOrNull { it.seconds } ?: 0L
        )
    }

    /** Monday 00:00 of the week containing [date]. */
    fun startOfWeek(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)

    /**
     * The day a session counts towards: the day it ended. A session that ends
     * exactly at midnight belongs to the day that just finished, not the new one.
     */
    fun countDate(session: SessionEntity, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
        val start = Instant.ofEpochMilli(session.startedAt)
        val end = Instant.ofEpochMilli(session.endedAt)
        return if (end.isAfter(start)) {
            localDateOf(end.minusNanos(1).toEpochMilli(), zone)
        } else {
            localDateOf(session.endedAt, zone)
        }
    }
}

/** Local date for an epoch millisecond value in [zone]. */
fun localDateOf(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
