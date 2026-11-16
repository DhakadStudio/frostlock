package com.frostlock.app.blocking

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.frostlock.app.data.local.prefs.AppLimitStore
import com.frostlock.app.data.local.prefs.BlockingPreferences
import com.frostlock.app.ui.MainActivity
import com.frostlock.app.ui.screens.block.BlockedScreen
import com.frostlock.app.ui.theme.FrostLockTheme
import kotlinx.coroutines.delay

/**
 * Full-screen wall shown when the user opens an app that is blocked during a focus
 * session, or one that has burned through its daily limit.
 *
 * There is no bypass here on purpose: focus blocking ends in FrostLock, and daily
 * limits reset on their own at midnight.
 */
class BlockActivity : ComponentActivity() {

    private var blockedPackage by mutableStateOf<String?>(null)
    private var blockedDomain by mutableStateOf<String?>(null)
    private var reason by mutableStateOf(REASON_FOCUS)
    private var untilMillis by mutableStateOf(0L)
    private var ruleLabel by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        readIntent(intent)
        val prefs = getSharedPreferences(BlockingPreferences.PREFS_NAME, MODE_PRIVATE)
        val limitStore = AppLimitStore(this)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = goHome()
            }
        )

        setContent {
            FrostLockTheme {
                BlockedContent(
                    appLabel = blockedPackage?.let { labelFor(it) },
                    blockedPackage = blockedPackage,
                    blockedDomain = blockedDomain,
                    reason = reason,
                    untilMillis = untilMillis,
                    ruleLabel = ruleLabel,
                    prefs = prefs,
                    limitStore = limitStore,
                    onGoHome = ::goHome,
                    onOpenFrostLock = ::openFrostLock,
                    onFinished = ::finish
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: Intent) {
        blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE)
        blockedDomain = intent.getStringExtra(EXTRA_DOMAIN)
        reason = intent.getStringExtra(EXTRA_REASON) ?: REASON_FOCUS
        untilMillis = intent.getLongExtra(EXTRA_UNTIL, 0L)
        ruleLabel = intent.getStringExtra(EXTRA_RULE_LABEL)
    }

    private fun labelFor(packageName: String): String? = runCatching {
        val info = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(info).toString()
    }.getOrNull()

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(home) }
        finishAndRemoveTask()
    }

    private fun openFrostLock() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
        const val EXTRA_DOMAIN = "extra_domain"
        const val EXTRA_REASON = "extra_reason"
        const val EXTRA_UNTIL = "extra_until"
        const val EXTRA_RULE_LABEL = "extra_rule_label"
        const val REASON_FOCUS = "focus"
        const val REASON_LIMIT = "limit"
        const val REASON_SCHEDULE = "schedule"
        const val REASON_WEBSITE = "website"
        const val REASON_YOUTUBE_HOME = "youtube_home"
    }
}

@Composable
private fun BlockedContent(
    appLabel: String?,
    blockedPackage: String?,
    blockedDomain: String?,
    reason: String,
    untilMillis: Long,
    ruleLabel: String?,
    prefs: SharedPreferences,
    limitStore: AppLimitStore,
    onGoHome: () -> Unit,
    onOpenFrostLock: () -> Unit,
    onFinished: () -> Unit
) {
    val isLimit = reason == BlockActivity.REASON_LIMIT
    val isScheduled = reason == BlockActivity.REASON_SCHEDULE
    val isWebsite = reason == BlockActivity.REASON_WEBSITE
    val isYouTubeHome = reason == BlockActivity.REASON_YOUTUBE_HOME

    var remainingSeconds by remember { mutableIntStateOf(-1) }
    var limitMinutes by remember(blockedPackage) {
        mutableIntStateOf(blockedPackage?.let { limitStore.limitMinutes(it) } ?: 0)
    }
    var usedMinutes by remember(blockedPackage) {
        mutableIntStateOf(blockedPackage?.let { limitStore.usageMinutesToday(it) } ?: 0)
    }

    LaunchedEffect(blockedPackage, reason, untilMillis) {
        val pkg = blockedPackage
        if (isLimit) {
            if (pkg == null) {
                onFinished()
                return@LaunchedEffect
            }
            var liftedTicks = 0
            while (true) {
                usedMinutes = limitStore.usageMinutesToday(pkg)
                limitMinutes = limitStore.limitMinutes(pkg)
                if (limitStore.isLimitReached(pkg)) {
                    liftedTicks = 0
                } else {
                    // Midnight (or a raised limit) makes the wall obsolete.
                    liftedTicks++
                    if (liftedTicks >= 2) {
                        onFinished()
                        break
                    }
                }
                delay(1_000L)
            }
        } else if (isWebsite) {
            // Rule-based website blocks end with the rule; focus-session blocks
            // follow the same heartbeat as app blocking.
            var expiredTicks = 0
            while (true) {
                val now = System.currentTimeMillis()
                if (untilMillis > 0L) {
                    remainingSeconds =
                        ((untilMillis - now) / 1000L).coerceAtLeast(0L).toInt()
                    expiredTicks = if (untilMillis <= now) expiredTicks + 1 else 0
                } else {
                    val heartbeat = prefs.getLong(BlockingPreferences.KEY_FOCUS_UNTIL, 0L)
                    val endsAt = prefs.getLong(BlockingPreferences.KEY_SESSION_ENDS_AT, 0L)
                    remainingSeconds = if (endsAt <= 0L) {
                        -1
                    } else {
                        ((endsAt - now) / 1000L).coerceAtLeast(0L).toInt()
                    }
                    expiredTicks = if (heartbeat <= now) expiredTicks + 1 else 0
                }
                if (expiredTicks >= 3) {
                    onGoHome()
                    break
                }
                delay(1_000L)
            }
        } else if (isScheduled) {
            // The wall lifts by itself when the scheduled window closes.
            var expiredTicks = 0
            while (true) {
                val now = System.currentTimeMillis()
                remainingSeconds = ((untilMillis - now) / 1000L).coerceAtLeast(0L).toInt()
                expiredTicks = if (untilMillis <= now) expiredTicks + 1 else 0
                if (expiredTicks >= 3) {
                    onGoHome()
                    break
                }
                delay(1_000L)
            }
        } else {
            // Poll the shared focus window: the session service refreshes it every second.
            var expiredTicks = 0
            while (true) {
                val now = System.currentTimeMillis()
                val heartbeat = prefs.getLong(BlockingPreferences.KEY_FOCUS_UNTIL, 0L)
                val endsAt = prefs.getLong(BlockingPreferences.KEY_SESSION_ENDS_AT, 0L)
                remainingSeconds = if (endsAt <= 0L) {
                    -1
                } else {
                    ((endsAt - now) / 1000L).coerceAtLeast(0L).toInt()
                }
                expiredTicks = if (heartbeat <= now) expiredTicks + 1 else 0
                if (expiredTicks >= 3) {
                    onGoHome()
                    break
                }
                delay(1_000L)
            }
        }
    }

    BlockedScreen(
        appLabel = appLabel,
        remainingSeconds = remainingSeconds,
        isLimitReached = isLimit,
        isScheduled = isScheduled,
        isWebsite = isWebsite,
        isYouTubeHome = isYouTubeHome,
        domain = blockedDomain,
        limitMinutes = limitMinutes,
        usedMinutes = usedMinutes,
        ruleLabel = ruleLabel,
        onGoHome = onGoHome,
        onOpenFrostLock = onOpenFrostLock
    )
}
