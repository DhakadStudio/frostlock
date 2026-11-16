package com.frostlock.app.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Onboarding : Screen("onboarding", "Onboarding")
    object Home : Screen("home", "Home")
    object Block : Screen("block", "Block")
    object Focus : Screen("focus", "Focus")
    object Stats : Screen("stats", "Stats")
    object Achievements : Screen("achievements", "Achievements")
    object Levels : Screen("levels", "Freeze levels")
    object Diary : Screen("diary", "Diary")
    object Calendar : Screen("calendar", "Focus calendar")
    object Deadline : Screen("deadline", "Focus deadline")
    object Goals : Screen("goals", "Focus goals")
    object GoalEditor : Screen("goal_editor/{goalId}", "Focus goal") {
        fun route(goalId: Long = -1L) = "goal_editor/$goalId"
    }
    object WeeklyReport : Screen("weekly_report", "Weekly report")
    object Notifications : Screen("notifications", "Notifications")
    object Websites : Screen("websites", "Website blocking")
    object Channels : Screen("channels", "YouTube channels")
    object Profile : Screen("profile", "Profile")
    object Schedules : Screen("schedules", "Schedules")
    object DesignPreview : Screen("design_preview", "Design Preview")
}
