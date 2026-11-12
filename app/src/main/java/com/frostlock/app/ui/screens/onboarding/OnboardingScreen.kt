package com.frostlock.app.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frostlock.app.R
import com.frostlock.app.ui.components.AchievementBadge
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.MoodChip
import com.frostlock.app.ui.components.PrimaryButton
import com.frostlock.app.ui.components.SecondaryButton
import com.frostlock.app.ui.theme.AccentFrost
import com.frostlock.app.ui.theme.BackgroundDark
import com.frostlock.app.ui.theme.FrostLockTheme
import com.frostlock.app.ui.theme.GlacierBlue
import com.frostlock.app.ui.theme.GlowIce
import com.frostlock.app.ui.theme.GradientFrost
import com.frostlock.app.ui.theme.InkOnIce
import com.frostlock.app.ui.theme.PrimaryIce
import com.frostlock.app.ui.theme.StrokeSoft
import com.frostlock.app.ui.theme.SuccessGreen
import com.frostlock.app.ui.theme.SurfaceCard
import com.frostlock.app.ui.theme.SurfaceHigh
import com.frostlock.app.ui.theme.TextPrimary
import com.frostlock.app.ui.theme.TextSecondary
import com.frostlock.app.ui.theme.WarnAmber
import com.frostlock.app.ui.util.Achievement
import com.frostlock.app.ui.util.AchievementCategory
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit,
    onExit: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { OnboardingPages.COUNT })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    BackHandler {
        if (pagerState.currentPage > 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        } else {
            onExit()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        OnboardingAurora()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            OnboardingTopBar(
                showSkip = pagerState.currentPage in
                    OnboardingPages.FOCUS..OnboardingPages.PROGRESS,
                onSkip = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(OnboardingPages.GOALS)
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            StepIndicator(current = pagerState.currentPage)

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 14.dp),
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    OnboardingPages.WELCOME -> WelcomePage()
                    OnboardingPages.FOCUS -> FeatureSlide(
                        kicker = "FOCUS TOOLKIT",
                        accent = AccentFrost,
                        title = "One timer. Zero excuses.",
                        subtitle = "Pomodoro, countdown or stopwatch — FrostLock keeps " +
                            "the clock and the apps in line.",
                        bullets = listOf(
                            "Pomodoro rounds with built-in breaks",
                            "Strict mode holds the exit locked",
                            "Your intention stays on screen"
                        ),
                        illustration = { FocusToolkitIllustration() }
                    )
                    OnboardingPages.CONTROL -> FeatureSlide(
                        kicker = "APP CONTROL",
                        accent = SuccessGreen,
                        title = "Freeze the time sinks.",
                        subtitle = "Choose what locks during focus, and cap the apps that " +
                            "quietly eat your day.",
                        bullets = listOf(
                            "Block distracting apps while you focus",
                            "One-tap 20-minute daily limits",
                            "Budgets reset at midnight, automatically"
                        ),
                        illustration = { AppControlIllustration() }
                    )
                    OnboardingPages.SCHEDULE -> FeatureSlide(
                        kicker = "AUTOMATIC ROUTINE",
                        accent = GlowIce,
                        title = "Let your week run itself.",
                        subtitle = "Set a schedule once and FrostLock blocks at the right " +
                            "times, on the right days.",
                        bullets = listOf(
                            "Auto-block by time and weekday",
                            "Per-subject app sets",
                            "Rules keep running outside focus mode"
                        ),
                        illustration = { ScheduleIllustration() }
                    )
                    OnboardingPages.PROGRESS -> FeatureSlide(
                        kicker = "VISIBLE PROGRESS",
                        accent = PrimaryIce,
                        title = "Watch the ice build up.",
                        subtitle = "Streaks, weekly charts and milestone badges turn small " +
                            "sessions into momentum.",
                        bullets = listOf(
                            "Day streaks and a 7-day chart",
                            "Badges for focus, streaks and sessions",
                            "Everything stored only on this device"
                        ),
                        illustration = { ProgressIllustration() }
                    )
                    OnboardingPages.GOALS -> GoalsPage(
                        uiState = uiState,
                        onGoalToggled = viewModel::onGoalToggled
                    )
                    OnboardingPages.TRIGGERS -> TriggersPage(
                        uiState = uiState,
                        onTriggerToggled = viewModel::onTriggerToggled
                    )
                    OnboardingPages.REASON -> ReasonPage(
                        reason = uiState.reason,
                        onReasonChanged = viewModel::onReasonChanged
                    )
                    OnboardingPages.COMMITMENT -> CommitmentPage(
                        uiState = uiState,
                        onFinish = { viewModel.finishOnboarding(onFinished) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    SecondaryButton(
                        text = stringResource(R.string.onb_back),
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (pagerState.currentPage < OnboardingPages.COMMITMENT) {
                    PrimaryButton(
                        text = stringResource(R.string.onb_next),
                        onClick = {
                            if (viewModel.canProceed()) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingTopBar(showSkip: Boolean, onSkip: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(GradientFrost)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AcUnit,
                contentDescription = null,
                tint = InkOnIce,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "FROSTLOCK",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
            color = TextSecondary
        )
        Spacer(modifier = Modifier.weight(1f))
        if (showSkip) {
            TextButton(onClick = onSkip) {
                Text(
                    text = "Skip intro",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentFrost
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        repeat(OnboardingPages.COUNT) { index ->
            val modifier = Modifier
                .weight(if (index == current) 1.8f else 1f)
                .height(4.dp)
                .clip(CircleShape)
                .background(
                    when {
                        index == current -> Brush.horizontalGradient(GradientFrost)
                        index < current -> androidx.compose.ui.graphics.SolidColor(
                            AccentFrost.copy(alpha = 0.45f)
                        )
                        else -> androidx.compose.ui.graphics.SolidColor(StrokeSoft)
                    }
                )
            Box(modifier = modifier)
        }
    }
}

@Composable
private fun WelcomePage() {
    val orbit = rememberInfiniteTransition(label = "orbit")
    val rotation by orbit.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26_000, easing = LinearEasing)
        ),
        label = "orbitRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = rotation }
            ) {
                drawCircle(
                    color = AccentFrost.copy(alpha = 0.30f),
                    radius = size.minDimension / 2f - 4.dp.toPx(),
                    style = Stroke(
                        width = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(5.dp.toPx(), 12.dp.toPx())
                        )
                    )
                )
            }
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                AccentFrost.copy(alpha = 0.26f),
                                PrimaryIce.copy(alpha = 0.06f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(GradientFrost)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AcUnit,
                        contentDescription = null,
                        tint = InkOnIce,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "FOCUS, FROZEN IN PLACE",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
            color = AccentFrost
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.onb_welcome_title),
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onb_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(modifier = Modifier.height(26.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WelcomePill(icon = Icons.Default.AcUnit, label = "Offline")
            WelcomePill(icon = Icons.Default.Lock, label = "No account")
            WelcomePill(icon = Icons.Default.Shield, label = "Private")
        }
    }
}

@Composable
private fun WelcomePill(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, StrokeSoft, CircleShape)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentFrost,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun FeatureSlide(
    kicker: String,
    accent: Color,
    title: String,
    subtitle: String,
    bullets: List<String>,
    illustration: @Composable () -> Unit
) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, animationSpec = tween(durationMillis = 460))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 26.dp.toPx()
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = kicker,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
            color = accent
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        Spacer(modifier = Modifier.height(22.dp))
        illustration()
        Spacer(modifier = Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            bullets.forEach { bullet ->
                FeatureBullet(text = bullet, accent = accent)
            }
        }
    }
}

@Composable
private fun FeatureBullet(text: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(13.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

// ---------------------------------------------------------------------------
// Illustrations
// ---------------------------------------------------------------------------

@Composable
private fun FocusToolkitIllustration() {
    ShowcaseCard(height = 238.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MiniChip(label = "Pomodoro", selected = true)
            MiniChip(label = "Timer")
            MiniChip(label = "Stopwatch")
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = WarnAmber,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "Strict",
                style = MaterialTheme.typography.labelSmall,
                color = WarnAmber
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            MiniTimerRing(progress = 0.92f, time = "24:58", label = "Stay in the zone")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniPill(icon = Icons.Default.Pause, label = "Take a break")
            MiniPill(icon = Icons.Default.PlayArrow, label = "Round 1 of 4")
        }
    }
}

@Composable
private fun MiniTimerRing(progress: Float, time: String, label: String) {
    Box(modifier = Modifier.size(148.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 6.dp.toPx()
            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(AccentFrost, Color(0xFF22D3EE), GlowIce, AccentFrost)
                ),
                startAngle = -90f,
                sweepAngle = progress.coerceIn(0f, 1f) * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = time,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun AppControlIllustration() {
    ShowcaseCard(height = 238.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = "App limits",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "2 active",
                style = MaterialTheme.typography.labelSmall,
                color = SuccessGreen
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        MockLimitRow(
            initial = "Y",
            tint = Color(0xFFFF4D4F),
            label = "YouTube",
            detail = "12 / 20 min",
            fraction = 0.6f
        )
        MockLimitRow(
            initial = "I",
            tint = Color(0xFFE1306C),
            label = "Instagram",
            detail = "8 / 15 min",
            fraction = 0.53f
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF4285F4).copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "C",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF8AB4F8),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Chrome",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Blocked during focus",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = AccentFrost,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun MockLimitRow(
    initial: String,
    tint: Color,
    label: String,
    detail: String,
    fraction: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.labelLarge,
                color = tint,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(SurfaceHigh)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(GradientFrost))
                )
            }
        }
    }
}

@Composable
private fun ScheduleIllustration() {
    ShowcaseCard(height = 238.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = GlowIce,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "This week",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            MiniChip(label = "Mon – Fri")
        }
        Spacer(modifier = Modifier.height(4.dp))
        MockScheduleRow(
            time = "06:00",
            title = "Morning study",
            duration = "2h",
            accent = SuccessGreen
        )
        MockScheduleRow(
            time = "14:00",
            title = "Afternoon deep work",
            duration = "2h",
            accent = AccentFrost
        )
        MockScheduleRow(
            time = "20:00",
            title = "Evening reading",
            duration = "45m",
            accent = Color(0xFFA78BFA)
        )
    }
}

@Composable
private fun MockScheduleRow(time: String, title: String, duration: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(30.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = duration,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun ProgressIllustration() {
    val bars = remember { listOf(0.45f, 0.72f, 0.55f, 0.95f, 0.62f, 0.30f, 0.82f) }
    val labels = remember { listOf("M", "T", "W", "T", "F", "S", "S") }
    val badges = remember {
        listOf(
            Achievement(
                id = "focus_1h", badge = "1H", title = "Focus 1h", description = "",
                current = 3600, target = 3600, category = AchievementCategory.FOCUS
            ),
            Achievement(
                id = "focus_10h", badge = "10H", title = "Focus 10h", description = "",
                current = 36000, target = 36000, category = AchievementCategory.FOCUS
            ),
            Achievement(
                id = "streak_7d", badge = "7D", title = "Streak 7d", description = "",
                current = 7, target = 7, category = AchievementCategory.STREAK
            ),
            Achievement(
                id = "focus_50h", badge = "50H", title = "Focus 50h", description = "",
                current = 36000, target = 180000, category = AchievementCategory.FOCUS
            )
        )
    }

    ShowcaseCard(height = 238.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "This week",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.AcUnit,
                contentDescription = null,
                tint = PrimaryIce,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "7 day streak",
                style = MaterialTheme.typography.labelSmall,
                color = AccentFrost
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEachIndexed { index, fraction ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((52 * fraction).dp.coerceAtLeast(6.dp))
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        PrimaryIce.copy(alpha = if (index == 3) 1f else 0.55f),
                                        GlacierBlue.copy(alpha = if (index == 3) 1f else 0.35f)
                                    )
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = labels[index],
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index == 3) TextPrimary else TextSecondary
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            badges.forEach { badge ->
                AchievementBadge(achievement = badge, size = 42.dp)
            }
        }
    }
}

@Composable
private fun ShowcaseCard(
    height: Dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B3B52), Color(0xFF06141F))
                )
            )
            .border(1.dp, StrokeSoft, RoundedCornerShape(26.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun MiniChip(label: String, selected: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (selected) Brush.horizontalGradient(GradientFrost)
                else androidx.compose.ui.graphics.SolidColor(SurfaceHigh)
            )
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) InkOnIce else TextSecondary
        )
    }
}

@Composable
private fun MiniPill(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentFrost,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun PersonalizationHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "PERSONALISE",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
            color = AccentFrost
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
fun GoalsPage(
    uiState: OnboardingUiState,
    onGoalToggled: (String) -> Unit
) {
    val goals = listOf(
        stringResource(R.string.onb_goal_study),
        stringResource(R.string.onb_goal_deep_work),
        stringResource(R.string.onb_goal_cut_social),
        stringResource(R.string.onb_goal_sleep_better),
        stringResource(R.string.onb_goal_build_habit)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        PersonalizationHeader(
            title = stringResource(R.string.onb_goals_title),
            subtitle = "Pick at least one. We'll shape your focus space around it."
        )
        Spacer(modifier = Modifier.height(18.dp))
        goals.forEach { goal ->
            val selected = uiState.selectedGoals.contains(goal)
            Spacer(modifier = Modifier.height(8.dp))
            MoodChip(
                text = goal,
                selected = selected,
                onClick = { onGoalToggled(goal) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TriggersPage(
    uiState: OnboardingUiState,
    onTriggerToggled: (String) -> Unit
) {
    val triggers = listOf(
        stringResource(R.string.onb_trigger_notifications),
        stringResource(R.string.onb_trigger_social),
        stringResource(R.string.onb_trigger_boredom),
        stringResource(R.string.onb_trigger_stress),
        stringResource(R.string.onb_trigger_night),
        stringResource(R.string.onb_trigger_other)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        PersonalizationHeader(
            title = stringResource(R.string.onb_triggers_title),
            subtitle = "Knowing the trigger is half the freeze."
        )
        Spacer(modifier = Modifier.height(18.dp))
        triggers.forEach { trigger ->
            val selected = uiState.selectedTriggers.contains(trigger)
            Spacer(modifier = Modifier.height(6.dp))
            MoodChip(
                text = trigger,
                selected = selected,
                onClick = { onTriggerToggled(trigger) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ReasonPage(
    reason: String,
    onReasonChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        PersonalizationHeader(
            title = stringResource(R.string.onb_reason_title),
            subtitle = "This line appears during focus when you need it most."
        )
        Spacer(modifier = Modifier.height(18.dp))
        OutlinedTextField(
            value = reason,
            onValueChange = onReasonChanged,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            placeholder = {
                Text(text = stringResource(R.string.onb_reason_hint), color = TextSecondary)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryIce,
                unfocusedBorderColor = StrokeSoft,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = MaterialTheme.shapes.medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Characters: ${reason.trim().length} / 300 (min 10)",
            style = MaterialTheme.typography.bodyMedium,
            color = if (reason.trim().length in 10..300) AccentFrost else TextSecondary
        )
    }
}

@Composable
fun CommitmentPage(
    uiState: OnboardingUiState,
    onFinish: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "READY",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
            color = AccentFrost
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.onb_commitment_title),
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Your plan",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                SummaryLine(
                    icon = Icons.Default.Check,
                    text = uiState.selectedGoals.joinToString(", ")
                        .ifBlank { "No goals selected" }
                )
                SummaryLine(
                    icon = Icons.Default.Shield,
                    text = uiState.selectedTriggers.joinToString(", ")
                        .ifBlank { "No triggers selected" }
                )
                SummaryLine(
                    icon = Icons.Default.AcUnit,
                    text = "“${uiState.reason.trim().ifBlank { "Keep showing up." }}”"
                )
            }
        }
        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(SurfaceCard)
                .border(1.dp, StrokeSoft, MaterialTheme.shapes.extraLarge)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            val pressStartTime = System.currentTimeMillis()
                            coroutineScope.launch {
                                progress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(durationMillis = 2000)
                                )
                                onFinish()
                            }
                            try {
                                awaitRelease()
                            } finally {
                                val pressDuration = System.currentTimeMillis() - pressStartTime
                                if (pressDuration < 2000) {
                                    coroutineScope.launch {
                                        progress.animateTo(
                                            0f,
                                            animationSpec = tween(durationMillis = 300)
                                        )
                                    }
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(progress.value)
                    .background(Brush.horizontalGradient(GradientFrost))
            )
            Text(
                text = stringResource(R.string.onb_start_journey),
                style = MaterialTheme.typography.labelLarge,
                color = if (progress.value > 0.55f) InkOnIce else TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Hold for two seconds. No sign-up, nothing leaves this phone.",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SummaryLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentFrost,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

// ---------------------------------------------------------------------------
// Backdrop
// ---------------------------------------------------------------------------

@Composable
private fun OnboardingAurora() {
    val transition = rememberInfiniteTransition(label = "onboarding aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraDrift"
    )
    val stars = remember {
        List(64) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()) }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF061620), Color(0xFF050B12), Color(0xFF03080E))
            )
        )
        val teal = Offset(w * (0.16f + drift * 0.10f), h * 0.14f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF2DD4BF).copy(alpha = 0.22f), Color.Transparent),
                center = teal,
                radius = w * 0.90f
            ),
            radius = w * 0.90f,
            center = teal
        )
        val ice = Offset(w * (0.88f - drift * 0.08f), h * 0.52f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF0EA5E9).copy(alpha = 0.20f), Color.Transparent),
                center = ice,
                radius = w * 0.80f
            ),
            radius = w * 0.80f,
            center = ice
        )
        val deep = Offset(w * (0.28f + drift * 0.12f), h * 0.90f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF60A5FA).copy(alpha = 0.13f), Color.Transparent),
                center = deep,
                radius = w * 0.70f
            ),
            radius = w * 0.70f,
            center = deep
        )
        stars.forEach { (x, y, brightness) ->
            drawCircle(
                color = Color.White.copy(alpha = 0.04f + brightness * 0.10f),
                radius = 0.8f + brightness * 1.5f,
                center = Offset(x * w, y * h)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050B12)
@Composable
fun OnboardingScreenPreview() {
    FrostLockTheme {
        WelcomePage()
    }
}
