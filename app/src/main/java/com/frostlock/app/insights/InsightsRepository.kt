package com.frostlock.app.insights

import com.frostlock.app.data.local.db.SessionDao
import com.frostlock.app.data.local.db.SessionEntity
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.data.local.prefs.FocusSubject
import com.frostlock.app.data.local.prefs.SubjectStore
import com.frostlock.app.data.local.prefs.subjectKey
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

/**
 * The single source of insight numbers.
 *
 * One read of the local session table feeds Insights, Trends, Weekly review,
 * History, badges, levels and widgets. Screens derive their own view state but
 * never re-aggregate raw sessions with a different day convention.
 */
@Singleton
class InsightsRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val focusSettings: FocusSettings,
    private val subjectStore: SubjectStore,
    private val clock: Clock,
    private val todayProvider: TodayProvider
) {

    val snapshot: Flow<InsightsSnapshot> = combine(
        sessionDao.recentSessions(),
        focusSettings.dailyGoalMinutes,
        subjectStore.subjectsFlow,
        todayProvider.today
    ) { sessions, dailyTargetMinutes, subjects, today ->
        buildInsightsSnapshot(
            sessions = sessions,
            dailyTargetMinutes = dailyTargetMinutes,
            subjects = subjects,
            today = today,
            zone = clock.zone(),
            now = clock.now()
        )
    }.flowOn(Dispatchers.Default)
}

internal const val UNTAGGED_SUBJECT = "Untagged"
internal const val OTHER_SUBJECT = "Other"
internal const val DEFAULT_SUBJECT_COLOR = 0xFF7DD3FC.toInt()

internal fun buildInsightsSnapshot(
    sessions: List<SessionEntity>,
    dailyTargetMinutes: Int,
    subjects: List<FocusSubject>,
    today: LocalDate,
    zone: ZoneId,
    now: Instant
): InsightsSnapshot {
    val saved = sessions.filter { it.durationSeconds > 0 }
    val buckets = FocusDayBuckets.build(saved, zone)
    val goalSeconds = dailyTargetMinutes.coerceAtLeast(1) * 60L

    val weekStart = FocusDayBuckets.startOfWeek(today)
    val weekEnd = weekStart.plusDays(6)
    val week = FocusDayBuckets.period(buckets, weekStart, weekEnd, goalSeconds)
    val last7 = FocusDayBuckets.period(buckets, today.minusDays(6), today, goalSeconds)

    val currentMonth = YearMonth.from(today)
    val months = (5 downTo 0).map { offset ->
        FocusDayBuckets.month(buckets, currentMonth.minusMonths(offset.toLong()))
    }

    val colorsByName = subjects.associate { subjectKey(it.name) to it.colorArgb }
    val weekSubjects = subjectBreakdown(
        sessions = saved,
        start = weekStart,
        end = weekEnd,
        zone = zone,
        colorFor = { name -> colorsByName[subjectKey(name)] }
    )

    val recent = saved.sortedByDescending { it.endedAt }.take(10)
    val weekReflections = saved.count { session ->
        session.note.isNotBlank() &&
            FocusDayBuckets.countDate(session, zone) in weekStart..weekEnd
    }

    return InsightsSnapshot(
        generatedAt = now,
        today = buckets[today] ?: DayFocus(today),
        week = week,
        last7 = last7,
        months = months,
        subjects = weekSubjects,
        days = buckets,
        recent = recent,
        currentStreak = FocusDayBuckets.currentStreak(buckets, today),
        bestStreak = FocusDayBuckets.bestStreak(buckets),
        totalSeconds = saved.sumOf { it.durationSeconds.toLong() },
        totalSessions = saved.size,
        reflections = saved.count { it.note.isNotBlank() },
        weekReflections = weekReflections
    )
}

/**
 * Splits each session at midnight and counts only the part inside [start]..[end].
 * Top five subjects keep their colour; the rest collapse into "Other".
 */
internal fun subjectBreakdown(
    sessions: List<SessionEntity>,
    start: LocalDate,
    end: LocalDate,
    zone: ZoneId,
    colorFor: (String) -> Int?
): List<SubjectFocus> {
    val secondsByTag = mutableMapOf<String, Long>()
    val sessionsByTag = mutableMapOf<String, Int>()

    sessions.forEach { session ->
        val seconds = FocusDayBuckets.split(session, zone)
            .filter { (date, _) -> date in start..end }
            .sumOf { it.second }
        if (seconds <= 0L) return@forEach

        val tag = session.tag.trim().ifBlank { UNTAGGED_SUBJECT }
        secondsByTag[tag] = (secondsByTag[tag] ?: 0L) + seconds
        if (FocusDayBuckets.countDate(session, zone) in start..end) {
            sessionsByTag[tag] = (sessionsByTag[tag] ?: 0) + 1
        }
    }

    val total = secondsByTag.values.sum().coerceAtLeast(1L)
    val ranked = secondsByTag.entries.sortedByDescending { it.value }
    val top = ranked.take(5).map { (tag, seconds) ->
        SubjectFocus(
            tag = tag,
            seconds = seconds,
            sessions = sessionsByTag[tag] ?: 0,
            share = seconds.toFloat() / total,
            colorArgb = colorFor(tag) ?: DEFAULT_SUBJECT_COLOR
        )
    }

    val rest = ranked.drop(5)
    if (rest.isEmpty()) return top

    val otherSeconds = rest.sumOf { it.value }
    val otherSessions = rest.sumOf { sessionsByTag[it.key] ?: 0 }
    return top + SubjectFocus(
        tag = OTHER_SUBJECT,
        seconds = otherSeconds,
        sessions = otherSessions,
        share = otherSeconds.toFloat() / total,
        colorArgb = DEFAULT_SUBJECT_COLOR
    )
}
