package com.frostlock.app.ui.screens.block

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.components.PrimaryButton
import com.frostlock.app.ui.components.SecondaryButton
import com.frostlock.app.ui.theme.AccentFrost
import com.frostlock.app.ui.theme.BackgroundDark
import com.frostlock.app.ui.theme.DangerRed
import com.frostlock.app.ui.theme.PrimaryIce
import com.frostlock.app.ui.theme.TextPrimary
import com.frostlock.app.ui.theme.TextSecondary
import com.frostlock.app.ui.util.formatClock

@Composable
fun BlockedScreen(
    appLabel: String?,
    remainingSeconds: Int,
    onGoHome: () -> Unit,
    onOpenFrostLock: () -> Unit,
    isLimitReached: Boolean = false,
    isScheduled: Boolean = false,
    isWebsite: Boolean = false,
    isWebsiteStudy: Boolean = false,
    isYouTubeHome: Boolean = false,
    isPlayStore: Boolean = false,
    isYouTubeChannel: Boolean = false,
    domain: String? = null,
    limitMinutes: Int = 0,
    usedMinutes: Int = 0,
    ruleLabel: String? = null
) {
    val accent = if (isLimitReached) DangerRed else AccentFrost

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isLimitReached -> Icons.Default.TimerOff
                        isWebsite -> Icons.Default.Language
                        isYouTubeHome -> Icons.Default.Home
                        isPlayStore -> Icons.Default.ShoppingCart
                        isYouTubeChannel -> Icons.Default.VideoLibrary
                        isScheduled -> Icons.Default.CalendarMonth
                        else -> Icons.Default.Lock
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = when {
                    isLimitReached -> "Daily limit reached"
                    isWebsite -> "Site held"
                    isYouTubeHome -> "Home feed held"
                    isPlayStore -> "Search held"
                    isYouTubeChannel -> "Channel held"
                    isScheduled -> "Scheduled focus"
                    else -> "Stay in the zone"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when {
                    isLimitReached && appLabel != null ->
                        "You've used your $limitMinutes-minute budget for $appLabel today."
                    isLimitReached -> "You've used this app's daily budget."
                    isWebsiteStudy && domain != null ->
                        "$domain is not on your study list while focus is active."
                    isWebsiteStudy ->
                        "This site is not on your study list while focus is active."
                    isWebsite && domain != null ->
                        "$domain is on your block list while focus is active."
                    isWebsite -> "This site is on your block list while focus is active."
                    isYouTubeHome ->
                        "The YouTube home feed is paused while focus is active. " +
                            "Search and subscriptions still work."
                    isPlayStore && domain != null ->
                        "Search results for \u201C$domain\u201D are held while focus is active."
                    isPlayStore -> "This search is held while focus is active."
                    isYouTubeChannel && domain != null ->
                        "\u201C$domain\u201D is held while focus is active. Study channels and everything else still play."
                    isYouTubeChannel -> "This channel is held while focus is active."
                    isScheduled && ruleLabel != null && appLabel != null ->
                        "$appLabel is blocked during \u201C$ruleLabel\u201D."
                    isScheduled && ruleLabel != null ->
                        "This app is blocked during \u201C$ruleLabel\u201D."
                    isScheduled && appLabel != null ->
                        "$appLabel is blocked during your scheduled focus."
                    isScheduled -> "This app is blocked during your scheduled focus."
                    appLabel != null -> "$appLabel is blocked while you focus."
                    else -> "This app is blocked while you focus."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLimitReached) {
                Text(
                    text = "${usedMinutes}m",
                    style = MaterialTheme.typography.displayLarge,
                    color = TextPrimary
                )
                Text(
                    text = "used today · resets at midnight",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DangerRed
                )
            } else if (remainingSeconds >= 0) {
                Text(
                    text = formatClock(remainingSeconds),
                    style = MaterialTheme.typography.displayLarge,
                    color = TextPrimary
                )
                Text(
                    text = when {
                        isWebsite -> "left in this block"
                        isPlayStore -> "left in this block"
                        isYouTubeChannel -> "left in this block"
                        isScheduled -> "left in this schedule"
                        else -> "left in this session"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentFrost
                )
            } else {
                Text(
                    text = "In progress",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = if (isWebsite) {
                        "End the session in FrostLock to unlock sites."
                    } else if (isYouTubeHome) {
                        "End the session in FrostLock to unlock the home feed."
                    } else if (isPlayStore) {
                        "End the session in FrostLock to unlock search."
                    } else if (isYouTubeChannel) {
                        "End the session in FrostLock to unlock channels."
                    } else {
                        "End the session in FrostLock to unlock apps."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentFrost,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            PrimaryButton(
                text = "Back to work",
                onClick = onGoHome,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            SecondaryButton(
                text = "Open FrostLock",
                onClick = onOpenFrostLock,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
