package com.frostlock.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DestinationsTest {

    private val allRoutes = listOf(
        Destinations.ONBOARDING,
        Destinations.MAIN,
        Destinations.Focus.GRAPH,
        Destinations.Focus.HOME,
        Destinations.Focus.SESSION,
        Destinations.Focus.SUMMARY,
        Destinations.Shield.GRAPH,
        Destinations.Shield.HOME,
        Destinations.Shield.APPS,
        Destinations.Shield.ADVANCED,
        Destinations.Shield.LIMITS,
        Destinations.Shield.SCHEDULES,
        Destinations.Shield.WEBSITES,
        Destinations.Shield.CHANNELS,
        Destinations.Shield.NOTIFICATIONS,
        Destinations.Insights.GRAPH,
        Destinations.Insights.HOME,
        Destinations.Insights.TRENDS,
        Destinations.Insights.HISTORY,
        Destinations.Insights.WEEKLY,
        Destinations.Insights.REFLECTIONS,
        Destinations.Insights.BADGES,
        Destinations.Insights.LEVELS,
        Destinations.You.GRAPH,
        Destinations.You.HOME,
        Destinations.You.GOALS,
        Destinations.You.GOAL_EDITOR,
        Destinations.You.COUNTDOWN,
        Destinations.You.WIDGETS,
        Destinations.You.PRIVACY,
        Destinations.You.ABOUT,
        Destinations.You.DESIGN_PREVIEW
    )

    @Test
    fun `every route is unique`() {
        assertEquals(allRoutes.size, allRoutes.toSet().size)
    }

    @Test
    fun `goal editor carries the goal id`() {
        assertEquals("you/goals/editor?goalId=42", Destinations.You.goalEditor(42L))
        assertEquals("you/goals/editor?goalId=-1", Destinations.You.goalEditor())
    }

    @Test
    fun `immersive routes never include a tab root`() {
        TopLevelDestination.entries.forEach { tab ->
            assertFalse(
                "${tab.label} root must keep the navigation bar",
                tab.route in Destinations.immersiveRoutes
            )
        }
    }

    @Test
    fun `immersive routes hide onboarding editors and session flows`() {
        assertTrue(Destinations.ONBOARDING in Destinations.immersiveRoutes)
        assertTrue(Destinations.You.GOAL_EDITOR in Destinations.immersiveRoutes)
        assertTrue(Destinations.Focus.SESSION in Destinations.immersiveRoutes)
        assertTrue(Destinations.Focus.SUMMARY in Destinations.immersiveRoutes)
    }

    @Test
    fun `standard detail screens keep the navigation bar`() {
        val details = listOf(
            Destinations.Shield.APPS,
            Destinations.Shield.ADVANCED,
            Destinations.Shield.LIMITS,
            Destinations.Shield.SCHEDULES,
            Destinations.Shield.WEBSITES,
            Destinations.Shield.CHANNELS,
            Destinations.Shield.NOTIFICATIONS,
            Destinations.Insights.HISTORY,
            Destinations.Insights.WEEKLY,
            Destinations.Insights.REFLECTIONS,
            Destinations.Insights.BADGES,
            Destinations.Insights.LEVELS,
            Destinations.You.GOALS,
            Destinations.You.COUNTDOWN,
            Destinations.You.WIDGETS,
            Destinations.You.PRIVACY,
            Destinations.You.ABOUT,
            Destinations.You.DESIGN_PREVIEW
        )

        details.forEach { route ->
            assertFalse("$route should show the navigation bar", route in Destinations.immersiveRoutes)
        }
    }
}
