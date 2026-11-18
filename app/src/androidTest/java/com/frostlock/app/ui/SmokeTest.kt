package com.frostlock.app.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Launch smoke tests for the four-tab shell.
 *
 * These run against real app data (the device under test is a daily-driver
 * phone), so assertions are deliberately data-independent: they check the
 * shell, the tab bar and each tab's structural markers — never empty states,
 * specific counts, or anything the user's history could change.
 *
 * Tabs are selected with `hasText(...) and hasClickAction()` because every
 * tab title text also appears as the screen title — the navigation item is
 * the only clickable node carrying that text.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SmokeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun tabMatcher(label: String) = hasText(label) and hasClickAction()

    private fun tapTab(label: String) {
        composeTestRule.waitUntilAtLeastOneExists(tabMatcher(label), 10_000)
        composeTestRule.onNode(tabMatcher(label)).performClick()
    }

    private fun waitFor(text: String, timeoutMillis: Long = 10_000) {
        composeTestRule.waitUntilAtLeastOneExists(hasText(text), timeoutMillis)
    }

    @Test
    fun coldLaunchShowsFocusTabAndAllFourTabs() {
        waitFor("Intention")
        onTab("Focus")
        onTab("Shield")
        onTab("Insights")
        onTab("You")
    }

    @Test
    fun shieldTabShowsProtectionSection() {
        tapTab("Shield")
        waitFor("Protection")
    }

    @Test
    fun insightsTabShowsPerspectiveSubtitle() {
        tapTab("Insights")
        waitFor("Your focus, in perspective.")
    }

    @Test
    fun youTabShowsSettingsSubtitle() {
        tapTab("You")
        waitFor("Your settings, your data.")
    }

    @Test
    fun tabRoundTripReturnsToFocus() {
        tapTab("You")
        waitFor("Your settings, your data.")
        tapTab("Insights")
        waitFor("Your focus, in perspective.")
        tapTab("Shield")
        waitFor("Protection")
        tapTab("Focus")
        waitFor("Intention")
    }

    private fun onTab(label: String) {
        // Structural presence check: the nav item exists and is clickable.
        composeTestRule.waitUntilAtLeastOneExists(tabMatcher(label), 10_000)
        composeTestRule.onNode(tabMatcher(label)).assertExists()
    }
}
