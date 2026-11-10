package com.focusflow.app.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.focusflow.app.R
import com.focusflow.app.data.local.prefs.BlockingPreferences
import com.focusflow.app.ui.MainActivity
import com.focusflow.app.ui.util.formatClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps a focus session alive while the app is in the background.
 *
 * The service refreshes the blocking window every second, so app blocking keeps
 * working when the user leaves FocusFlow. Being a foreground service also stops
 * Android from freezing the process, which would silently kill the block.
 */
class FocusSessionService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private lateinit var prefs: BlockingPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = BlockingPreferences(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val openEnded = intent.getBooleanExtra(EXTRA_OPEN_ENDED, false)
                val until = intent.getLongExtra(EXTRA_UNTIL, 0L)
                startSession(openEnded, until)
            }
            ACTION_STOP -> stopSession()
            else -> stopSession()
        }
        return START_NOT_STICKY
    }

    private fun startSession(openEnded: Boolean, until: Long) {
        createChannel()
        prefs.setSessionEndsAt(if (openEnded) 0L else until)
        startForeground(NOTIFICATION_ID, buildNotification(openEnded, 0L))

        job?.cancel()
        job = scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                if (!openEnded && until > 0L && now >= until) break

                prefs.setFocusWindow(now + BlockingPreferences.HEARTBEAT_WINDOW_MS)
                val remaining = if (openEnded) 0L else (until - now).coerceAtLeast(0L)
                notifyProgress(openEnded, remaining)
                delay(1_000L)
            }
            prefs.clearSession()
            stopSelf()
        }
    }

    private fun stopSession() {
        job?.cancel()
        job = null
        prefs.clearSession()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        job?.cancel()
        prefs.clearSession()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Focus sessions",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows the remaining time of your focus session"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(openEnded: Boolean, remainingMs: Long): Notification {
        val text = if (openEnded) {
            "Focus session in progress"
        } else {
            "${formatClock((remainingMs / 1000L).toInt())} remaining"
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle("FocusFlow")
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .build()
    }

    private fun notifyProgress(openEnded: Boolean, remainingMs: Long) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(openEnded, remainingMs))
    }

    companion object {
        private const val CHANNEL_ID = "focus_session"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.focusflow.app.action.START_FOCUS"
        const val ACTION_STOP = "com.focusflow.app.action.STOP_FOCUS"
        const val EXTRA_UNTIL = "extra_until"
        const val EXTRA_OPEN_ENDED = "extra_open_ended"

        fun start(context: Context, openEnded: Boolean, until: Long) {
            val intent = Intent(context, FocusSessionService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_OPEN_ENDED, openEnded)
                .putExtra(EXTRA_UNTIL, until)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, FocusSessionService::class.java)
                .setAction(ACTION_STOP)
            runCatching { context.startService(intent) }
        }
    }
}
