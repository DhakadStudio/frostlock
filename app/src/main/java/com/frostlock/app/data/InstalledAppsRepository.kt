package com.frostlock.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the list of launchable apps once and shares it between the blocker and the
 * schedule editor, so both screens always offer the same apps without listing the
 * package manager twice.
 */
@Singleton
class InstalledAppsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    @Volatile
    private var cache: List<InstalledApp>? = null

    suspend fun apps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        cache ?: load().also { cache = it }
    }

    /** Icon for a package that may not be launchable (e.g. an archived notification). */
    fun iconFor(packageName: String): ImageBitmap? = runCatching {
        context.packageManager.getApplicationIcon(packageName)
            .toBitmap(96, 96)
            .asImageBitmap()
    }.getOrNull()

    private fun load(): List<InstalledApp> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
        }

        return resolved
            .mapNotNull { it.activityInfo?.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName }
            .map { info ->
                InstalledApp(
                    packageName = info.packageName,
                    label = runCatching {
                        pm.getApplicationLabel(info).toString()
                    }.getOrDefault(info.packageName),
                    icon = runCatching {
                        pm.getApplicationIcon(info).toBitmap(96, 96).asImageBitmap()
                    }.getOrNull()
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
