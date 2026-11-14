package com.frostlock.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.frostlock.app.reminder.ScheduleReminderScheduler
import com.frostlock.app.ui.navigation.AppNavGraph
import com.frostlock.app.ui.navigation.Screen
import com.frostlock.app.ui.theme.FrostLockTheme
import com.frostlock.app.ui.theme.IcePalettes
import com.frostlock.app.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    @Inject lateinit var scheduleReminders: ScheduleReminderScheduler

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WidgetUpdater.refreshAsync(this)
        scheduleReminders.rescheduleAll()
        requestNotificationPermissionIfNeeded()
        setContent {
            val themeKey by mainViewModel.themeKey.collectAsState()
            FrostLockTheme(palette = IcePalettes.fromKey(themeKey)) {
                val onboardingDone by mainViewModel.onboardingDone.collectAsState()
                if (onboardingDone != null) {
                    AppNavGraph(
                        startDestination = if (onboardingDone == true) Screen.Home.route else Screen.Onboarding.route,
                        onFinishApp = { finish() }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
