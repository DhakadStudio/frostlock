package com.frostlock.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AchievementsTest {

    private fun millisAt(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `empty history yields no streak and only locked badges`() {
        val achievements = buildAchievements(
            totalSeconds = 0L,
            totalSessions = 0,
            startTimes = emptyList()
        )

        assertEquals(16, achievements.size)
        assertEquals(0, longestStreak(emptyList()))
        assertTrue(achievements.none { it.unlocked })
    }

    @Test
    fun `focus badges unlock from total focused time`() {
        val achievements = buildAchievements(
            totalSeconds = 4L * 3600L + 30L * 60L,
            totalSessions = 3,
            startTimes = emptyList()
        )

        val focus = achievements.filter { it.category == AchievementCategory.FOCUS }
        assertEquals(listOf(1L, 3L), focus.filter { it.unlocked }.map { it.target / 3600L })
        assertFalse(focus.first { it.target == 5L * 3600L }.unlocked)
    }

    @Test
    fun `session badges track completed sessions`() {
        val achievements = buildAchievements(
            totalSeconds = 0L,
            totalSessions = 10,
            startTimes = emptyList()
        )

        val sessions = achievements.filter { it.category == AchievementCategory.SESSIONS }
        assertEquals(2, sessions.count { it.unlocked })
        assertTrue(sessions.first { it.target == 50L }.progressLabel.contains("10 / 50"))
    }

    @Test
    fun `longest streak counts consecutive days only`() {
        val start = LocalDate.of(2026, 1, 1)
        val days = listOf(0L, 1L, 2L, 4L, 5L)

        assertEquals(3, longestStreak(days.map { millisAt(start.plusDays(it)) }))
    }

    @Test
    fun `one session per day still counts as a streak day`() {
        val start = LocalDate.of(2026, 2, 10)
        val times = (0L..4L).map { millisAt(start.plusDays(it)) }

        assertEquals(5, longestStreak(times))
    }

    @Test
    fun `duplicate days do not inflate the streak`() {
        val start = LocalDate.of(2026, 3, 1)
        val times = listOf(
            millisAt(start),
            millisAt(start),
            millisAt(start.plusDays(1)),
            millisAt(start.plusDays(3))
        )

        assertEquals(2, longestStreak(times))
    }

    @Test
    fun `share text names the badge and keeps the offline pitch`() {
        val achievement = Achievement(
            id = "streak_7d",
            badge = "7D",
            title = "Streak 7d",
            description = "Focus on 7 days in a row",
            current = 7L,
            target = 7L,
            category = AchievementCategory.STREAK
        )

        val text = achievementShareText(achievement)

        assertTrue(text.contains("Streak 7d"))
        assertTrue(text.contains("Focus on 7 days in a row"))
        assertTrue(text.contains("7 / 7 days"))
        assertTrue(text.contains("offline focus timer"))
    }

    @Test
    fun `badge progress is clamped and labelled per category`() {
        val achievement = Achievement(
            id = "focus_10h",
            badge = "10H",
            title = "Focus 10h",
            description = "Focus for 10h in total",
            current = 20L * 3600L,
            target = 10L * 3600L,
            category = AchievementCategory.FOCUS
        )

        assertEquals(1f, achievement.progress)
        assertTrue(achievement.unlocked)
        assertEquals("10h / 10h", achievement.progressLabel)
    }
}
