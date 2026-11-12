package com.frostlock.app.data

import androidx.compose.ui.graphics.ImageBitmap

/** A launchable app shown in the blocker and schedule pickers. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap? = null
)
