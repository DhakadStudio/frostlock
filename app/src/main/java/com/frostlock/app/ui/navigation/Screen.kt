package com.frostlock.app.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Onboarding : Screen("onboarding", "Onboarding")
    object Home : Screen("home", "Home")
    object Block : Screen("block", "Block")
    object Focus : Screen("focus", "Focus")
    object Stats : Screen("stats", "Stats")
    object Achievements : Screen("achievements", "Achievements")
    object Diary : Screen("diary", "Diary")
    object Profile : Screen("profile", "Profile")
    object Schedules : Screen("schedules", "Schedules")
    object DesignPreview : Screen("design_preview", "Design Preview")
}
