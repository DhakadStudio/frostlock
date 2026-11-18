package com.frostlock.app.ui.screens.stats

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.PrimaryButton
import com.frostlock.app.ui.components.SectionHeader
import com.frostlock.app.ui.theme.AccentFrost
import com.frostlock.app.ui.theme.BackgroundDark
import com.frostlock.app.ui.theme.GlacierBlue
import com.frostlock.app.ui.theme.GradientHero
import com.frostlock.app.ui.theme.PrimaryIce
import com.frostlock.app.ui.theme.StrokeSoft
import com.frostlock.app.ui.theme.SurfaceHigh
import com.frostlock.app.ui.theme.TextPrimary
import com.frostlock.app.ui.theme.TextSecondary
import com.frostlock.app.ui.util.FocusLevel
import com.frostlock.app.ui.util.FocusLevelStep
import com.frostlock.app.ui.util.focusLevelLadder
import com.frostlock.app.ui.util.focusLevelShareText

/**
 * The full freeze ladder. Every rung is derived from locally saved sessions, so
 * the page stays in sync with the stats card and needs no account or network.
 */
@Composable
fun LevelsScreen(
    onBack: () -> Unit,
    viewModel: com.frostlock.app.ui.screens.insights.LevelsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val level = state.level
    val ladder = focusLevelLadder(state.totalSeconds)
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Levels",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Titles earned from lifetime focus.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        LevelHero(level = level)

        PrimaryButton(
            text = "Share my level",
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, focusLevelShareText(level))
                }
                context.startActivity(Intent.createChooser(intent, "Share level"))
            }
        )

        SectionHeader(title = "The ladder")

        Text(
            text = "Each title needs more focus time than the last. Progress is " +
                "measured from every saved session on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ladder.forEach { step ->
                LevelRow(
                    step = step,
                    currentLevel = level.level,
                    minutesToReach = (step.minutesRequired - level.totalMinutes)
                        .coerceAtLeast(0L)
                )
            }
        }

        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AcUnit,
                        contentDescription = null,
                        tint = AccentFrost,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Levels are calculated from your saved sessions — no " +
                        "currency, purchases or store, and nothing is uploaded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun LevelHero(level: FocusLevel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(GradientHero))
            .border(1.dp, StrokeSoft, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = level.level.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = AccentFrost
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CURRENT TITLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = level.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }
            Text(
                text = minutesLabel(level.totalMinutes),
                style = MaterialTheme.typography.titleMedium,
                color = AccentFrost
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(SurfaceHigh)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(level.progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(5.dp))
                    .background(Brush.horizontalGradient(listOf(PrimaryIce, GlacierBlue)))
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${minutesLabel(level.minutesIntoLevel)} / " +
                    minutesLabel(level.minutesForNextLevel),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${minutesLabel(level.minutesToNextLevel)} to level ${level.level + 1}",
                style = MaterialTheme.typography.bodySmall,
                color = AccentFrost
            )
        }
    }
}

@Composable
private fun LevelRow(
    step: FocusLevelStep,
    currentLevel: Int,
    minutesToReach: Long
) {
    val isCurrent = step.level == currentLevel
    val shape = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceHigh.copy(alpha = if (isCurrent) 0.9f else 0.45f))
            .border(
                width = 1.dp,
                color = if (isCurrent) AccentFrost else StrokeSoft,
                shape = shape
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (step.reached) {
                        Brush.linearGradient(listOf(PrimaryIce, GlacierBlue))
                    } else {
                        Brush.linearGradient(listOf(SurfaceHigh, SurfaceHigh))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (step.reached) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = BackgroundDark,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = step.level.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (step.reached) TextPrimary else TextSecondary
            )
            Text(
                text = if (step.minutesRequired == 0L) {
                    "Starting title"
                } else {
                    "Starts at ${minutesLabel(step.minutesRequired)} focused"
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Text(
            text = when {
                isCurrent -> "Current"
                step.reached -> "Unlocked"
                else -> "${minutesLabel(minutesToReach)} to go"
            },
            style = MaterialTheme.typography.labelMedium,
            color = when {
                isCurrent -> AccentFrost
                step.reached -> TextSecondary
                else -> TextSecondary
            }
        )
    }
}

/** "45m", "3h" or "2h 30m" for a minute count. */
private fun minutesLabel(minutes: Long): String {
    val safe = minutes.coerceAtLeast(0L)
    return when {
        safe < 60L -> "${safe}m"
        safe % 60L == 0L -> "${safe / 60L}h"
        else -> "${safe / 60L}h ${safe % 60L}m"
    }
}
