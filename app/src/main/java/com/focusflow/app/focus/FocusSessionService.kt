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
 *
 * The same service also runs Pomodoro breaks. A break deliberately does *not*
 * refresh the blocking window: the user is allowed to use their phone until the
 * break ends, and then gets an alert to return.
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
                val focusGuard = intent.getBooleanExtra(EXTRA_FOCUS_GUARD, false)
                startSession(openEnded, until, focusGuard)
            }
            ACTION_START_BREAK -> {
                val until = intent.getLongExtra(EXTRA_UNTIL, 0L)
                startBreak(until)
            }
            ACTION_STOP -> stopSession()
            else -> stopSession()
        }
        return START_NOT_STICKY
    }

    private fun startSession(openEnded: Boolean, until: Long, focusGuard: Boolean) {
        createChannels()
        prefs.setSessionEndsAt(if (openEnded) 0L else until)
        startForeground(NOTIFICATION_ID, buildNotification(openEnded, 0L))

        job?.cancel()
        job = scope.launch {
            var lastGuardAt = System.currentTimeMillis()
            while (isActive) {
                val now = System.currentTimeMillis()
                if (!openEnded && until > 0L && now >= until) break

                prefs.setFocusWindow(now + BlockingPreferences.HEARTBEAT_WINDOW_MS)
                val remaining = if (openEnded) 0L else (until - now).coerceAtLeast(0L)
                notifyProgress(openEnded, remaining)

                // Focus Guard: an opt-in nudge every ten minutes.
                if (focusGuard && now - lastGuardAt >= FOCUS_GUARD_INTERVAL_MS) {
                    lastGuardAt = now
                    postFocusGuard()
                }
                delay(1_000L)
            }
            prefs.clearSession()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startBreak(until: Long) {
        createChannels()
        job?.cancel()
        prefs.clearSession()
        startForeground(NOTIFICATION_ID, buildBreakNotification(0L))

        job = scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val remaining = (until - now).coerceAtLeast(0L)
                if (remaining <= 0L) break
                notifyBreakProgress(remaining)
                delay(1_000L)
            }
            prefs.clearSession()
            stopForeground(STOP_FOREGROUND_REMOVE)
            postBreakOver()
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
        stopForeground(STOP_FOREGROUND_REMOVE)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Focus sessions",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the remaining time of your focus session"
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS_ID,
                "Focus alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts you when a Pomodoro break ends"
                setShowBadge(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GUARD_ID,
                "Focus Guard",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle reminders during long focus sessions"
            }
        )
    }

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun buildNotification(openEnded: Boolean, remainingMs: Long): Notification {
        val text = if (openEnded) {
            "Focus session in progress"
        } else {
            "${formatClock((remainingMs / 1000L).toInt())} remaining"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle("FocusFlow")
            .setContentText(text)
            .setContentIntent(contentIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .build()
    }

    private fun buildBreakNotification(remainingMs: Long): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle("Break time")
            .setContentText("${formatClock((remainingMs / 1000L).toInt())} until your next round")
            .setContentIntent(contentIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

    private fun notifyProgress(openEnded: Boolean, remainingMs: Long) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(openEnded, remainingMs))
    }

    private fun notifyBreakProgress(remainingMs: Long) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildBreakNotification(remainingMs))
    }

    private fun postBreakOver() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle("Break's over")
            .setContentText("Time for your next focus round.")
            .setContentIntent(contentIntent())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        getSystemService(NotificationManager::class.java).notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun postFocusGuard() {
        val notification = NotificationCompat.Builder(this, CHANNEL_GUARD_ID)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle("Focus Guard")
            .setContentText("Still with us? Put the phone down and keep going.")
            .setContentIntent(contentIntent())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        getSystemService(NotificationManager::class.java).notify(GUARD_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "focus_session"
        private const val CHANNEL_ALERTS_ID = "focus_alerts"
        private const val CHANNEL_GUARD_ID = "focus_guard"
        private const val NOTIFICATION_ID = 1001
        private const val ALERT_NOTIFICATION_ID = 1002
        private const val GUARD_NOTIFICATION_ID = 1003
        private const val FOCUS_GUARD_INTERVAL_MS = 10 * 60 * 1000L

        const val ACTION_START = "com.focusflow.app.action.START_FOCUS"
        const val ACTION_START_BREAK = "com.focusflow.app.action.START_BREAK"
        const val ACTION_STOP = "com.focusflow.app.action.STOP_FOCUS"
        const val EXTRA_UNTIL = "extra_until"
        const val EXTRA_OPEN_ENDED = "extra_open_ended"
        const val EXTRA_FOCUS_GUARD = "extra_focus_guard"

        fun start(
            context: Context,
            openEnded: Boolean,
            until: Long,
            focusGuard: Boolean = false
        ) {
            val intent = Intent(context, FocusSessionService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_OPEN_ENDED, openEnded)
                .putExtra(EXTRA_UNTIL, until)
                .putExtra(EXTRA_FOCUS_GUARD, focusGuard)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun startBreak(context: Context, until: Long) {
            val intent = Intent(context, FocusSessionService::class.java)
                .setAction(ACTION_START_BREAK)
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
