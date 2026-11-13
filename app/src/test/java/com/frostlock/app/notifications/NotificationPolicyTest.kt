package com.frostlock.app.notifications

import android.app.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationPolicyTest {

    private val focusContext = BlockContext(
        enabled = true,
        ownPackage = "com.frostlock.app",
        focusActive = true,
        focusPackages = setOf("com.instagram.android", "com.google.android.youtube")
    )

    private fun snapshot(
        packageName: String = "com.instagram.android",
        category: String? = null,
        isOngoing: Boolean = false,
        isForegroundService: Boolean = false,
        isGroupSummary: Boolean = false,
        isClearable: Boolean = true
    ) = NotificationSnapshot(
        packageName = packageName,
        category = category,
        isOngoing = isOngoing,
        isForegroundService = isForegroundService,
        isGroupSummary = isGroupSummary,
        isClearable = isClearable
    )

    @Test
    fun `blocks a blocked app during a focus session`() {
        val decision = decideNotificationBlock(snapshot(), focusContext)
        assertEquals(BlockReason.FOCUS, decision?.reason)
    }

    @Test
    fun `allows apps outside the blocked set`() {
        assertNull(
            decideNotificationBlock(
                snapshot(packageName = "com.android.settings"),
                focusContext
            )
        )
    }

    @Test
    fun `does nothing while the shield is off`() {
        assertNull(
            decideNotificationBlock(snapshot(), focusContext.copy(enabled = false))
        )
    }

    @Test
    fun `never blocks our own notifications`() {
        assertNull(
            decideNotificationBlock(
                snapshot(packageName = "com.frostlock.app"),
                focusContext.copy(focusPackages = setOf("com.frostlock.app"))
            )
        )
    }

    @Test
    fun `lets calls alarms and ongoing notifications through`() {
        assertNull(
            decideNotificationBlock(
                snapshot(category = Notification.CATEGORY_CALL),
                focusContext
            )
        )
        assertNull(
            decideNotificationBlock(
                snapshot(category = Notification.CATEGORY_ALARM),
                focusContext
            )
        )
        assertNull(
            decideNotificationBlock(snapshot(isOngoing = true), focusContext)
        )
        assertNull(
            decideNotificationBlock(snapshot(isForegroundService = true), focusContext)
        )
    }

    @Test
    fun `skips group summaries and unpinned notifications`() {
        assertNull(
            decideNotificationBlock(snapshot(isGroupSummary = true), focusContext)
        )
        assertNull(
            decideNotificationBlock(snapshot(isClearable = false), focusContext)
        )
    }

    @Test
    fun `active rule wins over an idle focus session`() {
        val context = focusContext.copy(
            focusActive = false,
            schedules = listOf(
                ScheduleScope("Study", setOf("com.google.android.youtube"))
            )
        )
        val decision = decideNotificationBlock(
            snapshot(packageName = "com.google.android.youtube"),
            context
        )
        assertNotNull(decision)
        assertEquals(BlockReason.SCHEDULE, decision?.reason)
        assertEquals("Study", decision?.ruleLabel)
    }

    @Test
    fun `reached daily limit is reported as the reason`() {
        val context = focusContext.copy(
            focusActive = false,
            limitedPackages = setOf("com.instagram.android")
        )
        val decision = decideNotificationBlock(snapshot(), context)
        assertEquals(BlockReason.LIMIT, decision?.reason)
    }
}
