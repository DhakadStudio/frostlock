package com.focusflow.app.ui.util

import com.focusflow.app.data.local.db.SessionEntity

/** One row of the "by subject" breakdown on the stats screen. */
data class TagTotal(
    val tag: String,
    val seconds: Long
)

/**
 * Groups sessions by their optional subject tag, newest sessions first, and
 * returns the biggest totals. Sessions without a tag are grouped under
 * "Untagged" so no focus time disappears from the breakdown.
 */
fun tagBreakdown(sessions: List<SessionEntity>, limit: Int = 5): List<TagTotal> =
    sessions
        .groupBy { it.tag.trim().ifBlank { "Untagged" } }
        .map { (tag, group) -> TagTotal(tag, group.sumOf { it.durationSeconds.toLong() }) }
        .filter { it.seconds > 0L }
        .sortedByDescending { it.seconds }
        .take(limit)
