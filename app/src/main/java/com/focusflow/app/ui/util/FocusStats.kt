package com.focusflow.app.ui.util

import com.focusflow.app.data.local.db.SessionEntity

/** One row of the "by subject" breakdown on the stats screen. */
data class TagTotal(
    val tag: String,
    val seconds: Long,
    /** Subject colour so the breakdown matches the chips in Focus and Schedule. */
    val colorArgb: Int = DEFAULT_SUBJECT_COLOR
)

private const val DEFAULT_SUBJECT_COLOR = 0xFFA78BFA.toInt()

/**
 * Groups sessions by their optional subject tag, newest sessions first, and
 * returns the biggest totals. Sessions without a tag are grouped under
 * "Untagged" so no focus time disappears from the breakdown.
 *
 * [colorFor] lets the caller resolve the saved subject colour; unknown or
 * untagged rows fall back to the app's violet accent.
 */
fun tagBreakdown(
    sessions: List<SessionEntity>,
    limit: Int = 5,
    colorFor: (String) -> Int? = { null }
): List<TagTotal> =
    sessions
        .groupBy { it.tag.trim().ifBlank { "Untagged" } }
        .map { (tag, group) ->
            TagTotal(
                tag = tag,
                seconds = group.sumOf { it.durationSeconds.toLong() },
                colorArgb = colorFor(tag) ?: DEFAULT_SUBJECT_COLOR
            )
        }
        .filter { it.seconds > 0L }
        .sortedByDescending { it.seconds }
        .take(limit)
