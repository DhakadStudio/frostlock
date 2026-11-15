package com.frostlock.app.ui.screens.profile

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.MoodChip
import com.frostlock.app.ui.components.SecondaryButton
import com.frostlock.app.ui.components.SectionHeader
import com.frostlock.app.widget.CalendarWidgetProvider
import com.frostlock.app.widget.DailyGoalWidgetProvider
import com.frostlock.app.widget.FocusWidgetProvider
import com.frostlock.app.widget.WeeklyWidgetProvider
import com.frostlock.app.widget.WidgetPinning
import com.frostlock.app.ui.theme.AccentFrost
import com.frostlock.app.ui.theme.GradientHero
import com.frostlock.app.ui.theme.BackgroundDark
import com.frostlock.app.ui.theme.GradientFrost
import com.frostlock.app.ui.theme.IcePalettes
import com.frostlock.app.ui.theme.PrimaryIce
import com.frostlock.app.ui.theme.StrokeSoft
import com.frostlock.app.ui.theme.SuccessGreen
import com.frostlock.app.ui.theme.SurfaceHigh
import com.frostlock.app.ui.theme.TextPrimary
import com.frostlock.app.ui.theme.TextSecondary
import com.frostlock.app.ui.util.AmbienceSound
import com.frostlock.app.ui.util.deadlineCountdown
import com.frostlock.app.ui.util.pauseOptionLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val GOAL_OPTIONS = listOf(30, 60, 90, 120, 180, 240)

@Composable
fun ProfileScreen(
    onNavigateToDesignPreview: () -> Unit = {},
    onOpenDiary: () -> Unit = {},
    onOpenWeeklyReport: () -> Unit = {},
    onOpenDeadline: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val canDrawOverlays = Settings.canDrawOverlays(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = "Tune FrostLock to your goals.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Free badge hero
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(
                    Brush.horizontalGradient(
                        GradientHero
                    )
                )
                .border(1.dp, StrokeSoft, MaterialTheme.shapes.medium)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(GradientFrost)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "F",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "FrostLock Pro",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Free forever · no ads · no subscriptions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SuccessGreen
                )
            }
        }

        SectionHeader(title = "Ice theme")
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Pick the palette that keeps you cool. It applies instantly across the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IcePalettes.all.forEach { palette ->
                        val selected = state.themeKey == palette.key
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .background(if (selected) SurfaceHigh else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (selected) palette.primary else StrokeSoft,
                                    MaterialTheme.shapes.medium
                                )
                                .clickable { viewModel.setTheme(palette.key) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(palette.gradient))
                                    .border(1.dp, palette.glow.copy(alpha = 0.5f), CircleShape)
                            )
                            Text(
                                text = palette.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) TextPrimary else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        SectionHeader(title = "Focus ambience")
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = AccentFrost,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sound while focusing",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Synthesised on-device, so it works offline. Media volume sets the level.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(AmbienceSound.Off, AmbienceSound.Deep, AmbienceSound.Rain).forEach { sound ->
                        MoodChip(
                            text = sound.label,
                            selected = state.ambienceKey == sound.key,
                            onClick = { viewModel.setAmbience(sound.key) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(AmbienceSound.Wind, AmbienceSound.Snow).forEach { sound ->
                        MoodChip(
                            text = sound.label,
                            selected = state.ambienceKey == sound.key,
                            onClick = { viewModel.setAmbience(sound.key) }
                        )
                    }
                }
            }
        }

        SectionHeader(title = "Daily goal")
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "How much focused time do you want each day?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GOAL_OPTIONS.take(3).forEach { minutes ->
                        MoodChip(
                            text = formatGoal(minutes),
                            selected = state.dailyGoalMinutes == minutes,
                            onClick = { viewModel.setDailyGoalMinutes(minutes) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GOAL_OPTIONS.drop(3).forEach { minutes ->
                        MoodChip(
                            text = formatGoal(minutes),
                            selected = state.dailyGoalMinutes == minutes,
                            onClick = { viewModel.setDailyGoalMinutes(minutes) }
                        )
                    }
                }
            }
        }

        SectionHeader(title = "Strict Mode")
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AccentFrost,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "No early exit",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Sessions can't be paused or stopped until the timer runs out.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = state.strictMode,
                    onCheckedChange = viewModel::setStrictMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryIce,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceHigh
                    )
                )
            }
        }

        SectionHeader(title = "Pause allowance")
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = null,
                            tint = AccentFrost,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pauses per session",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Cap how often a normal session can be paused. Strict Mode already blocks pausing entirely.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 1, 2).forEach { count ->
                        MoodChip(
                            text = pauseOptionLabel(count),
                            selected = state.maxPauses == count,
                            onClick = { viewModel.setMaxPauses(count) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, -1).forEach { count ->
                        MoodChip(
                            text = pauseOptionLabel(count),
                            selected = state.maxPauses == count,
                            onClick = { viewModel.setMaxPauses(count) }
                        )
                    }
                }
            }
        }

        SectionHeader(title = "Pomodoro breaks")
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = AccentFrost,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Break between rounds",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "After each Pomodoro round, blocking pauses and a break timer starts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = state.breaksEnabled,
                        onCheckedChange = viewModel::setBreaksEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryIce,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SurfaceHigh
                        )
                    )
                }
                if (state.breaksEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 5, 10).forEach { minutes ->
                            MoodChip(
                                text = "${minutes}m",
                                selected = state.breakMinutes == minutes,
                                onClick = { viewModel.setBreakMinutes(minutes) }
                            )
                        }
                    }
                }
            }
        }

        SectionHeader(title = "Focus Guard")
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = AccentFrost,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gentle nudges",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "A reminder every 10 minutes during a focus session, so a quick check doesn't become a scroll.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = state.focusGuardEnabled,
                    onCheckedChange = viewModel::setFocusGuardEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryIce,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceHigh
                    )
                )
            }
        }

        SectionHeader(title = "Focus bubble")
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = AccentFrost,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Floating timer",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (canDrawOverlays) {
                                "A small draggable timer stays visible over other apps during focus."
                            } else {
                                "Needs “display over other apps” permission to float above other apps."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = state.bubbleEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !Settings.canDrawOverlays(context)) {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                }
                            }
                            viewModel.setBubbleEnabled(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryIce,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = SurfaceHigh
                        )
                    )
                }

                if (!canDrawOverlays) {
                    TextButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            }
                        }
                    ) {
                        Text(text = "Allow display over other apps", color = AccentFrost)
                    }
                }
            }
        }

        SectionHeader(title = "Focus deadline")
        AppCard(modifier = Modifier.clickable(onClick = onOpenDeadline)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = AccentFrost,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.deadline?.title ?: "Set a target date",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = state.deadline?.let { deadline ->
                            "${formatDeadlineDate(deadline.date)} · " +
                                deadlineCountdown(deadline).orEmpty()
                        } ?: "Count down to an exam, launch or personal deadline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        SectionHeader(title = "Reflect & review")
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenDiary),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = AccentFrost,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Study diary",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "One-line reflections saved with every session.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(StrokeSoft)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenWeeklyReport),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = AccentFrost,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Weekly report",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Totals, rhythm and subjects for any week.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        SectionHeader(title = "Home screen widgets")
        AppCard {
            val context = LocalContext.current
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = AccentFrost,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Four widgets: today's focus, daily-goal progress, the week ahead, and a month heat-map. Everything stays on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                if (WidgetPinning.isSupported(context)) {
                    Text(
                        text = "Pin one straight to your home screen:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    SecondaryButton(
                        text = "Focus today",
                        onClick = { WidgetPinning.request(context, FocusWidgetProvider::class.java) }
                    )
                    SecondaryButton(
                        text = "Daily goal",
                        onClick = { WidgetPinning.request(context, DailyGoalWidgetProvider::class.java) }
                    )
                    SecondaryButton(
                        text = "This week",
                        onClick = { WidgetPinning.request(context, WeeklyWidgetProvider::class.java) }
                    )
                    SecondaryButton(
                        text = "Focus calendar",
                        onClick = { WidgetPinning.request(context, CalendarWidgetProvider::class.java) }
                    )
                } else {
                    Text(
                        text = "Long-press your home screen → Widgets → FrostLock to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        SectionHeader(title = "Coming soon")
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•  Website & Shorts blocker", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("•  Focus groups & leaderboards", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }

        SectionHeader(title = "About")
        AppCard {
            Text(
                text = "FrostLock is a free focus timer with a built-in app blocker. Track your sessions, build a daily streak, and keep distracting apps out of your way — plus scheduled auto-blocking and daily app limits.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun formatGoal(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "${rest}m"
        rest == 0 -> "${hours}h"
        else -> "${hours}h ${rest}m"
    }
}

private fun formatDeadlineDate(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US))
}.getOrDefault(iso)
