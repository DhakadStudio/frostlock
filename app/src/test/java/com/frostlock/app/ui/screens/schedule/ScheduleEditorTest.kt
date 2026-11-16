package com.frostlock.app.ui.screens.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure helpers behind the dedicated blocking-rule editor page. */
class ScheduleEditorTest {

    @Test
    fun weekdayPresetReadsAsWeekdays() {
        assertEquals("Weekdays", repeatSummary(setOf(1, 2, 3, 4, 5), emptySet()))
    }

    @Test
    fun allSevenDaysReadAsEveryDay() {
        assertEquals("Every day", repeatSummary((1..7).toSet(), emptySet()))
    }

    @Test
    fun partialSelectionListsDayNames() {
        assertEquals("Mon, Wed", repeatSummary(setOf(1, 3), emptySet()))
    }

    @Test
    fun dateRulesSummariseTheirCount() {
        assertEquals("Once, on 1 date", repeatSummary(emptySet(), setOf("2026-11-20")))
        assertEquals(
            "Once, on 2 dates",
            repeatSummary(emptySet(), setOf("2026-11-20", "2026-11-21"))
        )
    }

    @Test
    fun emptyRepeatIsCalledOut() {
        assertEquals("No days selected", repeatSummary(emptySet(), emptySet()))
    }

    @Test
    fun daytimeWindowReportsHours() {
        assertEquals("8h", windowDuration(9 * 60, 17 * 60))
    }

    @Test
    fun windowReportsHoursAndMinutes() {
        assertEquals("8h 30m", windowDuration(9 * 60, 17 * 60 + 30))
    }

    @Test
    fun overnightWindowWrapsPastMidnight() {
        assertEquals("4h", windowDuration(22 * 60, 2 * 60))
        assertEquals("45m", windowDuration(23 * 60 + 30, 15))
    }

    @Test
    fun blankNameIsRejected() {
        assertEquals(
            "Give this rule a name.",
            validateRule("   ", setOf(1), emptySet(), 9 * 60, 17 * 60)
        )
    }

    @Test
    fun ruleNeedsAtLeastOneDayOrDate() {
        assertEquals(
            "Pick at least one day or date.",
            validateRule("Focus", emptySet(), emptySet(), 9 * 60, 17 * 60)
        )
    }

    @Test
    fun identicalStartAndEndIsRejected() {
        assertEquals(
            "Start and end can't be the same.",
            validateRule("Focus", setOf(1), emptySet(), 9 * 60, 9 * 60)
        )
    }

    @Test
    fun dateOnlyRuleIsValid() {
        assertNull(validateRule("Exam", emptySet(), setOf("2026-11-20"), 9 * 60, 17 * 60))
    }

    @Test
    fun dateRangeIsValidWhenOrdered() {
        assertNull(
            validateRule(
                "Exam",
                setOf(1, 2, 3, 4, 5),
                emptySet(),
                9 * 60,
                17 * 60,
                fromDate = "2026-11-01",
                untilDate = "2026-11-30"
            )
        )
    }

    @Test
    fun reversedDateRangeIsRejected() {
        assertEquals(
            "The end date can't be before the start date.",
            validateRule(
                "Exam",
                setOf(1, 2, 3, 4, 5),
                emptySet(),
                9 * 60,
                17 * 60,
                fromDate = "2026-11-30",
                untilDate = "2026-11-01"
            )
        )
    }

    @Test
    fun completeRuleIsValid() {
        assertNull(validateRule("Focus", setOf(1, 2, 3, 4, 5), emptySet(), 9 * 60, 17 * 60))
    }

    @Test
    fun dateRangeSummaryNamesBothEnds() {
        assertEquals("18 Nov – 20 Nov", dateRangeSummary("2026-11-18", "2026-11-20"))
        assertEquals("From 18 Nov", dateRangeSummary("2026-11-18", null))
        assertEquals("Until 20 Nov", dateRangeSummary(null, "2026-11-20"))
        assertNull(dateRangeSummary(null, null))
    }

    @Test
    fun timePresetsAreDistinctValidWindows() {
        assertTrue(TIME_PRESETS.isNotEmpty())
        TIME_PRESETS.forEach { preset ->
            assertTrue(preset.startMinutes in 0 until 24 * 60)
            assertTrue(preset.endMinutes in 0 until 24 * 60)
            assertNotEquals(preset.startMinutes, preset.endMinutes)
        }
    }

    @Test
    fun nightPresetCrossesMidnight() {
        val night = matchingTimePreset(22 * 60, 2 * 60)

        assertEquals("Night", night?.label)
        assertEquals("4h", windowDuration(22 * 60, 2 * 60))
    }

    @Test
    fun unknownWindowHasNoPreset() {
        assertNull(matchingTimePreset(8 * 60, 10 * 60))
    }
}
