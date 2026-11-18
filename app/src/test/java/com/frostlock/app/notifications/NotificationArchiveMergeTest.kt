package com.frostlock.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationArchiveMergeTest {

    private fun item(
        id: Long,
        key: String?,
        title: String = "Title $id",
        postedAt: Long = id
    ) = BlockedNotification(
        id = id,
        packageName = "com.example.app",
        appLabel = "Example",
        title = title,
        text = "Body",
        postedAt = postedAt,
        reason = BlockReason.FOCUS,
        key = key
    )

    @Test
    fun `new notification is added at the top`() {
        val current = listOf(item(1, "a"), item(2, "b"))

        val merged = mergeBlockedNotification(current, item(3, "c"))

        assertEquals(listOf(3L, 1L, 2L), merged.map { it.id })
    }

    @Test
    fun `same key updates in place and moves to the top`() {
        val current = listOf(item(1, "a"), item(2, "b"), item(3, "c"))

        val merged = mergeBlockedNotification(
            current,
            item(2, "b", title = "Updated", postedAt = 99)
        )

        assertEquals(listOf(2L, 1L, 3L), merged.map { it.id })
        assertEquals(1, merged.count { it.key == "b" })
        assertEquals("Updated", merged.first().title)
        assertEquals(99L, merged.first().postedAt)
    }

    @Test
    fun `entries without a key are never merged`() {
        val current = listOf(item(1, null), item(2, null))

        val merged = mergeBlockedNotification(current, item(3, null))

        assertEquals(3, merged.size)
        assertEquals(listOf(3L, 1L, 2L), merged.map { it.id })
    }

    @Test
    fun `cap drops the oldest entries`() {
        val current = (1L..5L).map { item(it, "key$it") }

        val merged = mergeBlockedNotification(current, item(6, "key6"), maxEntries = 3)

        assertEquals(3, merged.size)
        assertEquals(listOf(6L, 1L, 2L), merged.map { it.id })
    }

    @Test
    fun `cap of zero yields an empty inbox`() {
        val merged = mergeBlockedNotification(
            current = listOf(item(1, "a")),
            incoming = item(2, "b"),
            maxEntries = 0
        )

        assertEquals(emptyList<BlockedNotification>(), merged)
    }

    @Test
    fun `legacy entry without a key survives a load-and-save round trip`() {
        val legacy = item(1, null)

        assertNull(legacy.key)
        assertEquals("Title 1", legacy.title)
    }
}
