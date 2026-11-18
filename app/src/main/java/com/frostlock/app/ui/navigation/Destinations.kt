package com.frostlock.app.ui.navigation

/**
 * Every route in the redesign, grouped by tab.
 *
 * Only tab roots are top-level destinations. Everything else is nested inside a
 * tab graph so the back stack, saved state and predictive back stay coherent.
 */
object Destinations {

    const val ONBOARDING = "onboarding"
    const val MAIN = "main"

    object Focus {
        const val GRAPH = "focus"
        const val HOME = "focus/home"

        // Reserved for Phase 6.
        const val SESSION = "focus/session"
        const val SUMMARY = "focus/summary/{sessionId}"
    }

    object Shield {
        const val GRAPH = "shield"
        const val HOME = "shield/home"
        const val APPS = "shield/apps"
        const val LIMITS = "shield/limits"
        const val SCHEDULES = "shield/schedules"
        const val WEBSITES = "shield/websites"
        const val CHANNELS = "shield/channels"
        const val NOTIFICATIONS = "shield/notifications"
    }

    object Insights {
        const val GRAPH = "insights"
        const val HOME = "insights/home"
        const val TRENDS = "insights/trends"
        const val HISTORY = "insights/history"
        const val WEEKLY = "insights/weekly"
        const val REFLECTIONS = "insights/reflections"
        const val BADGES = "insights/badges"
        const val LEVELS = "insights/levels"
    }

    object You {
        const val GRAPH = "you"
        const val HOME = "you/home"
        const val GOALS = "you/goals"
        const val GOAL_EDITOR = "you/goals/editor?goalId={goalId}"
        const val COUNTDOWN = "you/countdown"
        const val WIDGETS = "you/widgets"
        const val DESIGN_PREVIEW = "you/design-preview"

        fun goalEditor(goalId: Long = -1L) = "you/goals/editor?goalId=$goalId"
    }

    /**
     * Flows that own the whole window. The bottom bar stays visible on standard
     * detail screens and is hidden only here, on onboarding and on the block
     * wall.
     */
    val immersiveRoutes: Set<String> = setOf(
        ONBOARDING,
        You.GOAL_EDITOR,
        Focus.SESSION,
        Focus.SUMMARY
    )
}
