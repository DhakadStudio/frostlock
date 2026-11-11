package com.focusflow.app.ui

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
import com.focusflow.app.ui.navigation.AppNavGraph
import com.focusflow.app.ui.navigation.Screen
import com.focusflow.app.ui.theme.FocusFlowTheme
import com.focusflow.app.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WidgetUpdater.refreshAsync(this)
        requestNotificationPermissionIfNeeded()
        setContent {
            FocusFlowTheme {
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
