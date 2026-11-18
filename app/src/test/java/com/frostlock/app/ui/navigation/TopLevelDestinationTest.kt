package com.frostlock.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TopLevelDestinationTest {

    @Test
    fun `four tabs appear in product order`() {
        assertEquals(
            listOf("Focus", "Shield", "Insights", "You"),
            TopLevelDestination.entries.map { it.label }
        )
    }

    @Test
    fun `each tab root maps to its own tab`() {
        assertEquals(TopLevelDestination.FOCUS, TopLevelDestination.fromRoute(Destinations.Focus.HOME))
        assertEquals(TopLevelDestination.SHIELD, TopLevelDestination.fromRoute(Destinations.Shield.HOME))
        assertEquals(TopLevelDestination.INSIGHTS, TopLevelDestination.fromRoute(Destinations.Insights.HOME))
        assertEquals(TopLevelDestination.YOU, TopLevelDestination.fromRoute(Destinations.You.HOME))
    }

    @Test
    fun `detail routes map back to the owning tab`() {
        assertEquals(
            TopLevelDestination.SHIELD,
            TopLevelDestination.fromRoute(Destinations.Shield.NOTIFICATIONS)
        )
        assertEquals(
            TopLevelDestination.INSIGHTS,
            TopLevelDestination.fromRoute(Destinations.Insights.REFLECTIONS)
        )
        assertEquals(
            TopLevelDestination.YOU,
            TopLevelDestination.fromRoute(Destinations.You.GOAL_EDITOR)
        )
    }

    @Test
    fun `graph route itself maps to its tab`() {
        TopLevelDestination.entries.forEach { tab ->
            assertEquals(tab, TopLevelDestination.fromRoute(tab.graphRoute))
        }
    }

    @Test
    fun `onboarding main and unknown routes have no tab`() {
        assertNull(TopLevelDestination.fromRoute(Destinations.ONBOARDING))
        assertNull(TopLevelDestination.fromRoute(Destinations.MAIN))
        assertNull(TopLevelDestination.fromRoute("elsewhere"))
        assertNull(TopLevelDestination.fromRoute(null))
    }

    @Test
    fun `a lookalike prefix does not match a tab`() {
        assertNull(TopLevelDestination.fromRoute("focusmode/home"))
        assertNull(TopLevelDestination.fromRoute("shielded"))
    }

    @Test
    fun `tab roots and graphs are all distinct`() {
        val roots = TopLevelDestination.entries.map { it.route }
        val graphs = TopLevelDestination.entries.map { it.graphRoute }

        assertEquals(roots.size, roots.toSet().size)
        assertEquals(graphs.size, graphs.toSet().size)
        assertTrue(roots.none { it in graphs })
    }

    @Test
    fun `navigation items point at the tab root`() {
        TopLevelDestination.entries.forEach { tab ->
            assertEquals(tab.route, tab.navItem.route)
            assertEquals(tab.label, tab.navItem.label)
        }
    }

    @Test
    fun `a tab root lives under its own graph`() {
        TopLevelDestination.entries.forEach { tab ->
            assertTrue(
                "${tab.label} root ${tab.route} is not under ${tab.graphRoute}",
                tab.route.startsWith("${tab.graphRoute}/")
            )
        }
    }
}
