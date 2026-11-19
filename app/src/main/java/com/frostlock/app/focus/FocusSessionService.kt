package com.frostlock.app.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.frostlock.app.R
import com.frostlock.app.data.local.prefs.BlockingPreferences
import com.frostlock.app.data.local.prefs.FocusSettings
import com.frostlock.app.ui.MainActivity
import com.frostlock.app.ui.util.AmbienceSound
import com.frostlock.app.ui.util.ambienceFromKey
import com.frostlock.app.ui.util.focusBubbleLabel
import com.frostlock.app.ui.util.formatClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Keeps a focus session alive while the app is in the background.
 *
 * The service refreshes the blocking window every second, so app blocking keeps
 * working when the user leaves FrostLock. Being a foreground service also stops
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

    private val mainHandler = Handler(Looper.getMainLooper())
    private var bubble: TextView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private val ambiencePlayer = AmbiencePlayer()

    /** The key currently handed to [ambiencePlayer], so ticks can detect edits. */
    private var activeAmbienceKey: String? = null

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
        showBubble()
        syncAmbience()

        job?.cancel()
        job = scope.launch {
            var lastGuardAt = System.currentTimeMillis()
            while (isActive) {
                val now = System.currentTimeMillis()
                if (!openEnded && until > 0L && now >= until) break

                prefs.setFocusWindow(now + BlockingPreferences.HEARTBEAT_WINDOW_MS)
                val remaining = if (openEnded) 0L else (until - now).coerceAtLeast(0L)
                notifyProgress(openEnded, remaining)
                updateBubble(openEnded, remaining)
                syncAmbience()

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
        hideBubble()
        stopAmbience()
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
        hideBubble()
        stopAmbience()
        prefs.clearSession()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        job?.cancel()
        hideBubble()
        stopAmbience()
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

    private fun bubbleEnabled(): Boolean =
        getSharedPreferences(FocusSettings.PREFS_NAME, MODE_PRIVATE)
            .getBoolean(FocusSettings.KEY_FOCUS_BUBBLE, false) &&
            Settings.canDrawOverlays(this)

    /**
     * Reads the ambience choice synchronously, exactly like the bubble setting,
     * because the service starts before any ViewModel exists.
     *
     * The same call runs once per tick, so changing or muting the sound from the
     * timer takes effect within a second instead of at the next session.
     */
    private fun syncAmbience() {
        val key = getSharedPreferences(FocusSettings.PREFS_NAME, MODE_PRIVATE)
            .getString(FocusSettings.KEY_AMBIENCE, AmbienceSound.Off.key)
        if (key == activeAmbienceKey) return
        activeAmbienceKey = key
        ambiencePlayer.play(ambienceFromKey(key))
    }

    private fun stopAmbience() {
        activeAmbienceKey = null
        ambiencePlayer.stop()
    }

    /**
     * A small draggable pill above other apps. It is purely informational:
     * tapping it returns to FrostLock, and it disappears when the session ends
     * or a break starts.
     */
    private fun showBubble() {
        if (!bubbleEnabled()) return
        mainHandler.post {
            if (bubble != null) return@post
            val manager = getSystemService(WindowManager::class.java) ?: return@post
            val view = TextView(this).apply {
                setTextColor(Color.WHITE)
                textSize = 17f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                text = focusBubbleLabel(openEnded = true, remainingMs = 0L)
                setPadding(dp(16), dp(8), dp(16), dp(8))
                background = GradientDrawable().apply {
                    cornerRadius = dp(24).toFloat()
                    setColor(Color.parseColor("#E60E1B28"))
                    setStroke(dp(1), Color.parseColor("#6638BDF8"))
                }
                elevation = dp(6).toFloat()
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = dp(16)
                y = dp(72)
            }
            attachDrag(view, params, manager)
            runCatching { manager.addView(view, params) }.onSuccess {
                bubble = view
                bubbleParams = params
            }
        }
    }

    private fun attachDrag(
        view: TextView,
        params: WindowManager.LayoutParams,
        manager: WindowManager
    ) {
        val slop = dp(8).toFloat()
        var startX = 0
        var startY = 0
        var downX = 0f
        var downY = 0f
        var moved = false

        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    downX = event.rawX
                    downY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > slop || abs(dy) > slop) moved = true
                    params.x = (startX - dx).toInt().coerceAtLeast(0)
                    params.y = (startY + dy).toInt().coerceAtLeast(0)
                    runCatching { manager.updateViewLayout(view, params) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) openApp()
                    true
                }
                else -> false
            }
        }
    }

    private fun updateBubble(openEnded: Boolean, remainingMs: Long) {
        val text = focusBubbleLabel(openEnded, remainingMs)
        mainHandler.post { bubble?.text = text }
    }

    private fun hideBubble() {
        mainHandler.post {
            bubble?.let { view ->
                runCatching {
                    getSystemService(WindowManager::class.java)?.removeView(view)
                }
            }
            bubble = null
            bubbleParams = null
        }
    }

    private fun openApp() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

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
            .setContentTitle("FrostLock")
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

        const val ACTION_START = "com.frostlock.app.action.START_FOCUS"
        const val ACTION_START_BREAK = "com.frostlock.app.action.START_BREAK"
        const val ACTION_STOP = "com.frostlock.app.action.STOP_FOCUS"
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
