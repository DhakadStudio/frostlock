package com.frostlock.app.ui.util

import com.frostlock.app.data.local.db.SessionEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** How many days make up one comparison window. */
private const val WINDOW_DAYS = 7

/** One side of the improvement comparison: the first week or the latest week. */
data class FocusPeriod(
    val start: LocalDate,
    val end: LocalDate,
    val seconds: Long,
    val sessions: Int,
    val activeDays: Int,
    val goalDays: Int,
    val bestStreak: Int
)

enum class ImprovementTrend { UP, DOWN, FLAT }

/** One row in the improvement card. */
data class ImprovementMetric(
    val label: String,
    val baselineText: String,
    val currentText: String,
    val deltaText: String,
    val trend: ImprovementTrend
)

/**
 * A local-only comparison between the user's first week of focus history and
 * their latest week. No account, upload or usage-access permission is involved.
 */
data class ImprovementReport(
    val hasHistory: Boolean = false,
    val hasEnoughHistory: Boolean = false,
    val baselineDaysCollected: Int = 0,
    val baselineTargetDays: Int = WINDOW_DAYS,
    /** Days left before a full, non-overlapping comparison is possible. */
    val daysUntilComparison: Int = WINDOW_DAYS * 2 - 1,
    val baseline: FocusPeriod? = null,
    val current: FocusPeriod? = null,
    val metrics: List<ImprovementMetric> = emptyList()
) {
    val isEmpty: Boolean get() = !hasHistory
    val isBuildingBaseline: Boolean get() = hasHistory && !hasEnoughHistory
}

/**
 * Builds the improvement report from local focus sessions.
 *
 * The baseline is the first seven days of history. The current window is the
 * latest seven days ending [today]. A full comparison only appears once those
 * two windows no longer overlap, so the same sessions never inflate a delta.
 */
fun buildImprovementReport(
    sessions: List<SessionEntity>,
    dailyGoalMinutes: Int,
    today: LocalDate = LocalDate.now()
): ImprovementReport {
    val goalSeconds = dailyGoalMinutes.coerceAtLeast(1) * 60L
    val dated = sessions
        .filter { it.durationSeconds > 0 }
        .groupBy { localDateOf(it.endedAt) }
    val firstDate = dated.keys.minOrNull()

    if (firstDate == null) {
        return ImprovementReport(
            hasHistory = false,
            hasEnoughHistory = false,
            baselineDaysCollected = 0,
            baselineTargetDays = WINDOW_DAYS,
            daysUntilComparison = WINDOW_DAYS * 2 - 1
        )
    }

    val baselineStart = firstDate
    val baselineEnd = firstDate.plusDays(WINDOW_DAYS - 1L)
    val currentStart = today.minusDays(WINDOW_DAYS - 1L)
    val currentEnd = today

    val baselineDaysCollected = (ChronoUnit.DAYS.between(baselineStart, today) + 1)
        .coerceIn(0L, WINDOW_DAYS.toLong())
        .toInt()

    // The first full post-baseline week ends here.
    val comparisonReadyDate = baselineEnd.plusDays(WINDOW_DAYS.toLong())
    val hasEnoughHistory = !today.isBefore(comparisonReadyDate)
    val daysUntilComparison = if (hasEnoughHistory) {
        0
    } else {
        ChronoUnit.DAYS.between(today, comparisonReadyDate).toInt().coerceAtLeast(1)
    }

    val baseline = periodSummary(dated, baselineStart, baselineEnd, goalSeconds)
    val current = periodSummary(dated, currentStart, currentEnd, goalSeconds)

    return ImprovementReport(
        hasHistory = true,
        hasEnoughHistory = hasEnoughHistory,
        baselineDaysCollected = baselineDaysCollected,
        baselineTargetDays = WINDOW_DAYS,
        daysUntilComparison = daysUntilComparison,
        baseline = baseline,
        current = current,
        metrics = if (hasEnoughHistory) buildMetrics(baseline, current) else emptyList()
    )
}

private fun periodSummary(
    dated: Map<LocalDate, List<SessionEntity>>,
    start: LocalDate,
    end: LocalDate,
    goalSeconds: Long
): FocusPeriod {
    val days = (0L..ChronoUnit.DAYS.between(start, end)).map { start.plusDays(it) }
    val secondsByDate = days.associateWith { date ->
        dated[date].orEmpty().sumOf { it.durationSeconds.toLong() }
    }
    val activeDates = secondsByDate.filterValues { it > 0L }.keys

    return FocusPeriod(
        start = start,
        end = end,
        seconds = secondsByDate.values.sum(),
        sessions = days.sumOf { dated[it].orEmpty().size },
        activeDays = activeDates.size,
        goalDays = secondsByDate.count { (_, seconds) -> seconds >= goalSeconds },
        bestStreak = longestStreak(activeDates)
    )
}

/** Longest run of consecutive active days inside one window. */
private fun longestStreak(dates: Set<LocalDate>): Int {
    if (dates.isEmpty()) return 0
    val sorted = dates.sorted()
    var best = 1
    var run = 1
    for (index in 1 until sorted.size) {
        if (sorted[index] == sorted[index - 1].plusDays(1)) {
            run++
            best = maxOf(best, run)
        } else {
            run = 1
        }
    }
    return best
}

private fun buildMetrics(baseline: FocusPeriod, current: FocusPeriod): List<ImprovementMetric> = listOf(
    ImprovementMetric(
        label = "Focus time",
        baselineText = formatDuration(baseline.seconds),
        currentText = formatDuration(current.seconds),
        deltaText = percentText(current.seconds, baseline.seconds),
        trend = trendOf(current.seconds, baseline.seconds)
    ),
    ImprovementMetric(
        label = "Sessions",
        baselineText = baseline.sessions.toString(),
        currentText = current.sessions.toString(),
        deltaText = signedText(current.sessions - baseline.sessions, "session"),
        trend = trendOf(current.sessions.toLong(), baseline.sessions.toLong())
    ),
    ImprovementMetric(
        label = "Active days",
        baselineText = "${baseline.activeDays} / $WINDOW_DAYS",
        currentText = "${current.activeDays} / $WINDOW_DAYS",
        deltaText = signedText(current.activeDays - baseline.activeDays, "day"),
        trend = trendOf(current.activeDays.toLong(), baseline.activeDays.toLong())
    ),
    ImprovementMetric(
        label = "Goal hit",
        baselineText = "${baseline.goalDays} / $WINDOW_DAYS",
        currentText = "${current.goalDays} / $WINDOW_DAYS",
        deltaText = signedText(current.goalDays - baseline.goalDays, "day"),
        trend = trendOf(current.goalDays.toLong(), baseline.goalDays.toLong())
    ),
    ImprovementMetric(
        label = "Best streak",
        baselineText = "${baseline.bestStreak}d",
        currentText = "${current.bestStreak}d",
        deltaText = signedText(current.bestStreak - baseline.bestStreak, "day"),
        trend = trendOf(current.bestStreak.toLong(), baseline.bestStreak.toLong())
    )
)

private fun percentText(current: Long, baseline: Long): String {
    if (baseline <= 0L) return if (current > 0L) "New" else "—"
    val percent = (((current - baseline).toDouble() / baseline) * 100).roundToInt()
    return when {
        percent > 0 -> "+$percent%"
        percent < 0 -> "$percent%"
        else -> "0%"
    }
}

private fun signedText(difference: Int, unit: String): String = when {
    difference > 0 -> "+$difference $unit${if (difference == 1) "" else "s"}"
    difference < 0 -> "$difference $unit${if (difference == -1) "" else "s"}"
    else -> "No change"
}

private fun trendOf(current: Long, baseline: Long): ImprovementTrend = when {
    current > baseline -> ImprovementTrend.UP
    current < baseline -> ImprovementTrend.DOWN
    else -> ImprovementTrend.FLAT
}
