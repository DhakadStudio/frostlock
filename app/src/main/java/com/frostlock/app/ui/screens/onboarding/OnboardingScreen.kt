package com.frostlock.app.ui.screens.onboarding

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.frostlock.app.data.InstalledApp
import com.frostlock.app.R
import com.frostlock.app.ui.components.AchievementBadge
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.MoodChip
import com.frostlock.app.ui.components.PrimaryButton
import com.frostlock.app.ui.components.SecondaryButton
import com.frostlock.app.ui.theme.FrostLockTheme
import com.frostlock.app.ui.theme.rememberReducedMotion
import com.frostlock.app.ui.util.Achievement
import com.frostlock.app.ui.util.AchievementCategory
import com.frostlock.app.ui.util.onboardingShowcaseApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import com.frostlock.app.ui.theme.FrostTheme

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
            .background(FrostTheme.colors.background)
    ) {
        OnboardingAurora()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            OnboardingTopBar(
                showSkip = pagerState.currentPage in
                    OnboardingPages.RADAR..OnboardingPages.PROGRESS,
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
                    OnboardingPages.RADAR -> FeatureSlide(
                        kicker = "DISTRACTION RADAR",
                        accent = FrostTheme.colors.tertiary,
                        title = "They're circling your focus.",
                        subtitle = "The pull of a notification is designed, not accidental. " +
                            "FrostLock watches the usual suspects and freezes them first.",
                        bullets = listOf(
                            "The apps that quietly eat an hour",
                            "Frozen the moment focus starts",
                            "Your own app list decides what locks"
                        ),
                        illustration = { DistractionRadarIllustration() }
                    )
                    OnboardingPages.FOCUS -> FeatureSlide(
                        kicker = "FOCUS TOOLKIT",
                        accent = FrostTheme.colors.secondary,
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
                        accent = FrostTheme.colors.success,
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
                        accent = FrostTheme.colors.tertiary,
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
                        accent = FrostTheme.colors.primary,
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
                .background(Brush.linearGradient(FrostTheme.colors.actionGradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AcUnit,
                contentDescription = null,
                tint = FrostTheme.colors.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "FROSTLOCK",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
            color = FrostTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.weight(1f))
        if (showSkip) {
            TextButton(onClick = onSkip) {
                Text(
                    text = "Skip intro",
                    style = MaterialTheme.typography.labelMedium,
                    color = FrostTheme.colors.secondary
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
                        index == current -> Brush.horizontalGradient(FrostTheme.colors.actionGradient)
                        index < current -> androidx.compose.ui.graphics.SolidColor(
                            FrostTheme.colors.secondary.copy(alpha = 0.45f)
                        )
                        else -> androidx.compose.ui.graphics.SolidColor(FrostTheme.colors.outline)
                    }
                )
            Box(modifier = modifier)
        }
    }
}

@Composable
private fun WelcomePage() {
    val orbit = rememberInfiniteTransition(label = "orbit")
    val animatedRotation by orbit.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26_000, easing = LinearEasing)
        ),
        label = "orbitRotation"
    )
    val reducedOrbit = rememberReducedMotion()
    val rotation = if (reducedOrbit) 0f else animatedRotation

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
            val colors = FrostTheme.colors
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = rotation }
            ) {
                drawCircle(
                    color = colors.secondary.copy(alpha = 0.30f),
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
                                FrostTheme.colors.secondary.copy(alpha = 0.26f),
                                FrostTheme.colors.primary.copy(alpha = 0.06f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(FrostTheme.colors.actionGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AcUnit,
                        contentDescription = null,
                        tint = FrostTheme.colors.onPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "FOCUS, FROZEN IN PLACE",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
            color = FrostTheme.colors.secondary
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.onb_welcome_title),
            style = MaterialTheme.typography.displayLarge,
            color = FrostTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onb_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = FrostTheme.colors.textSecondary,
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
            .border(1.dp, FrostTheme.colors.outline, CircleShape)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = FrostTheme.colors.secondary,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = FrostTheme.colors.textSecondary
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
            color = FrostTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = FrostTheme.colors.textSecondary,
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
            color = FrostTheme.colors.textSecondary
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
                tint = FrostTheme.colors.warning,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "Strict",
                style = MaterialTheme.typography.labelSmall,
                color = FrostTheme.colors.warning
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
        val colors = FrostTheme.colors
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
                    listOf(colors.secondary, Color(0xFF22D3EE), colors.tertiary, colors.secondary)
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
                color = FrostTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = FrostTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun AppControlIllustration() {
    val apps by rememberInstalledShowcaseApps()
    val limited = apps.take(2)
    val blocked = apps.getOrNull(2)

    ShowcaseCard(height = 244.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(FrostTheme.colors.success.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = FrostTheme.colors.success,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = "App limits",
                style = MaterialTheme.typography.titleMedium,
                color = FrostTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (apps.isEmpty()) "Reading apps…" else "${limited.size} active",
                style = MaterialTheme.typography.labelSmall,
                color = FrostTheme.colors.success
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (apps.isEmpty()) {
            repeat(3) { index ->
                PlaceholderAppRow(blocked = index == 2)
            }
        } else {
            limited.forEachIndexed { index, app ->
                MockLimitRow(
                    app = app,
                    detail = if (index == 0) "12 / 20 min" else "8 / 15 min",
                    fraction = if (index == 0) 0.6f else 0.53f
                )
            }
            blocked?.let { app -> BlockedAppRow(app) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            showcaseBrands.take(6).forEach { brand ->
                BrandTile(brand = brand, size = 22.dp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "+4 more",
                style = MaterialTheme.typography.labelSmall,
                color = FrostTheme.colors.textSecondary
            )
        }
    }
}

/**
 * Reads the real launcher icons from this device so the illustration shows
 * genuine app artwork instead of bundled stand-ins. Only the three apps the
 * card needs are kept.
 */
@Composable
private fun rememberInstalledShowcaseApps(): State<List<InstalledApp>> {
    val context = LocalContext.current
    return produceState(initialValue = emptyList(), context) {
        value = withContext(Dispatchers.IO) { loadShowcaseApps(context) }
    }
}

private fun loadShowcaseApps(context: Context): List<InstalledApp> {
    val packageManager = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(
            launcherIntent,
            PackageManager.ResolveInfoFlags.of(0L)
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(launcherIntent, 0)
    }

    val installed = resolved
        .mapNotNull { it.activityInfo?.applicationInfo }
        .distinctBy { it.packageName }
        .filter { it.packageName != context.packageName }
        .map { info ->
            InstalledApp(
                packageName = info.packageName,
                label = runCatching {
                    packageManager.getApplicationLabel(info).toString()
                }.getOrDefault(info.packageName),
                icon = runCatching {
                    packageManager.getApplicationIcon(info).toBitmap(96, 96).asImageBitmap()
                }.getOrNull()
            )
        }

    return onboardingShowcaseApps(installed)
}

@Composable
private fun AppIcon(app: InstalledApp, size: Dp = 34.dp) {
    if (app.icon != null) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
                .background(FrostTheme.colors.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.label.take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = FrostTheme.colors.textSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MockLimitRow(
    app: InstalledApp,
    detail: String,
    fraction: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(app = app)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = FrostTheme.colors.textSecondary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(FrostTheme.colors.surfaceContainerHigh)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(FrostTheme.colors.actionGradient))
                )
            }
        }
    }
}

@Composable
private fun BlockedAppRow(app: InstalledApp) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(app = app)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyMedium,
                color = FrostTheme.colors.textPrimary
            )
            Text(
                text = "Blocked during focus",
                style = MaterialTheme.typography.labelSmall,
                color = FrostTheme.colors.textSecondary
            )
        }
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = FrostTheme.colors.secondary,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun PlaceholderAppRow(blocked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(FrostTheme.colors.surfaceContainerHigh)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (blocked) 0.45f else 0.35f)
                    .height(9.dp)
                    .clip(CircleShape)
                    .background(FrostTheme.colors.surfaceContainerHigh)
            )
            if (!blocked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(FrostTheme.colors.surfaceContainerHigh)
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
                tint = FrostTheme.colors.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "This week",
                style = MaterialTheme.typography.titleMedium,
                color = FrostTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            MiniChip(label = "Mon – Fri")
        }
        Spacer(modifier = Modifier.height(4.dp))
        MockScheduleRow(
            time = "06:00",
            title = "Morning study",
            duration = "2h",
            accent = FrostTheme.colors.success
        )
        MockScheduleRow(
            time = "14:00",
            title = "Afternoon deep work",
            duration = "2h",
            accent = FrostTheme.colors.secondary
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
            color = FrostTheme.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = duration,
            style = MaterialTheme.typography.labelSmall,
            color = FrostTheme.colors.textSecondary
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
                color = FrostTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.AcUnit,
                contentDescription = null,
                tint = FrostTheme.colors.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "7 day streak",
                style = MaterialTheme.typography.labelSmall,
                color = FrostTheme.colors.secondary
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
                                        FrostTheme.colors.primary.copy(alpha = if (index == 3) 1f else 0.55f),
                                        FrostTheme.colors.glacier.copy(alpha = if (index == 3) 1f else 0.35f)
                                    )
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = labels[index],
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index == 3) FrostTheme.colors.textPrimary else FrostTheme.colors.textSecondary
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

/**
 * Real app artwork shown in the radar and on the app-control card. The PNGs are
 * official marks pulled from Wikimedia Commons, bundled so onboarding works offline.
 * A few brands only publish a glyph, so those draw on their brand-coloured tile.
 */
private data class BrandApp(
    val iconRes: Int,
    val label: String,
    val background: Color? = null,
    val tint: Color? = null,
    val frozen: Boolean = false
)

private val showcaseBrands = listOf(
    BrandApp(R.drawable.ic_app_instagram, "Instagram"),
    BrandApp(R.drawable.ic_app_tiktok, "TikTok", frozen = true),
    BrandApp(R.drawable.ic_app_youtube, "YouTube", frozen = true),
    BrandApp(R.drawable.ic_app_facebook, "Facebook"),
    BrandApp(R.drawable.ic_app_snapchat, "Snapchat"),
    BrandApp(
        iconRes = R.drawable.ic_app_x,
        label = "X",
        background = Color(0xFF000000),
        tint = Color.White
    ),
    BrandApp(
        iconRes = R.drawable.ic_app_reddit,
        label = "Reddit",
        background = Color(0xFFFF4500),
        tint = Color.White
    ),
    BrandApp(R.drawable.ic_app_netflix, "Netflix"),
    BrandApp(R.drawable.ic_app_spotify, "Spotify"),
    BrandApp(R.drawable.ic_app_pinterest, "Pinterest")
)

@Composable
private fun BrandTile(brand: BrandApp, size: Dp) {
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .then(
                if (brand.background != null) Modifier.background(brand.background) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(brand.iconRes),
            contentDescription = brand.label,
            contentScale = ContentScale.Fit,
            colorFilter = brand.tint?.let { ColorFilter.tint(it) },
            modifier = Modifier
                .fillMaxSize()
                .padding(if (brand.background != null) size * 0.18f else 0.dp)
        )
    }
}

@Composable
private fun DistractionRadarIllustration() {
    val transition = rememberInfiniteTransition(label = "radar")
    val animatedSweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9_000, easing = LinearEasing)
        ),
        label = "radarSweep"
    )
    val reducedRadar = rememberReducedMotion()
    val sweep = if (reducedRadar) 0f else animatedSweep

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(258.dp),
        contentAlignment = Alignment.Center
    ) {
        val colors = FrostTheme.colors
        Canvas(modifier = Modifier.size(232.dp)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(color = Color(0xFF04121B), radius = radius)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(colors.secondary.copy(alpha = 0.12f), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius
            )
            listOf(0.36f, 0.64f, 0.92f).forEach { scale ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.07f),
                    radius = radius * scale,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            val crossColor = Color.White.copy(alpha = 0.05f)
            drawLine(
                color = crossColor,
                start = Offset(0f, center.y),
                end = Offset(size.width, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = crossColor,
                start = Offset(center.x, 0f),
                end = Offset(center.x, size.height),
                strokeWidth = 1.dp.toPx()
            )

            rotate(sweep) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.00f to Color.Transparent,
                        0.72f to colors.secondary.copy(alpha = 0.02f),
                        0.94f to colors.secondary.copy(alpha = 0.34f),
                        1.00f to Color.Transparent
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = true
                )
            }

            drawCircle(
                color = colors.secondary.copy(alpha = 0.22f),
                radius = radius,
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawCircle(color = colors.secondary.copy(alpha = 0.55f), radius = 2.dp.toPx())
        }

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(FrostTheme.colors.actionGradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AcUnit,
                contentDescription = null,
                tint = FrostTheme.colors.onPrimary,
                modifier = Modifier.size(25.dp)
            )
        }

        showcaseBrands.forEachIndexed { index, brand ->
            RadarAppIcon(brand = brand, index = index)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.RadarAppIcon(
    brand: BrandApp,
    index: Int
) {
    val onOuterRing = index < 5
    val angle = if (onOuterRing) -90f + index * 72f else -54f + (index - 5) * 72f
    val orbit = if (onOuterRing) 0.80f else 0.52f
    val radians = (angle * PI / 180.0).toFloat()
    val radiusDp = 116f * orbit

    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .absoluteOffset(
                x = (cos(radians) * radiusDp).dp,
                y = (sin(radians) * radiusDp).dp
            )
            .size(42.dp)
    ) {
        BrandTile(brand = brand, size = 42.dp)
        if (brand.frozen) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .absoluteOffset(x = 5.dp, y = 5.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(FrostTheme.colors.background)
                    .border(1.dp, FrostTheme.colors.secondary.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = FrostTheme.colors.secondary,
                    modifier = Modifier.size(9.dp)
                )
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
            .border(1.dp, FrostTheme.colors.outline, RoundedCornerShape(26.dp))
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
                if (selected) Brush.horizontalGradient(FrostTheme.colors.actionGradient)
                else androidx.compose.ui.graphics.SolidColor(FrostTheme.colors.surfaceContainerHigh)
            )
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) FrostTheme.colors.onPrimary else FrostTheme.colors.textSecondary
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
            tint = FrostTheme.colors.secondary,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = FrostTheme.colors.textSecondary
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
            color = FrostTheme.colors.secondary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = FrostTheme.colors.textPrimary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = FrostTheme.colors.textSecondary
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
                Text(text = stringResource(R.string.onb_reason_hint), color = FrostTheme.colors.textSecondary)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FrostTheme.colors.primary,
                unfocusedBorderColor = FrostTheme.colors.outline,
                focusedTextColor = FrostTheme.colors.textPrimary,
                unfocusedTextColor = FrostTheme.colors.textPrimary
            ),
            shape = MaterialTheme.shapes.medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Characters: ${reason.trim().length} / 300 (min 10)",
            style = MaterialTheme.typography.bodyMedium,
            color = if (reason.trim().length in 10..300) FrostTheme.colors.secondary else FrostTheme.colors.textSecondary
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
            color = FrostTheme.colors.secondary
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.onb_commitment_title),
            style = MaterialTheme.typography.headlineLarge,
            color = FrostTheme.colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Your plan",
                    style = MaterialTheme.typography.titleMedium,
                    color = FrostTheme.colors.textPrimary
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
                .background(FrostTheme.colors.surfaceContainer)
                .border(1.dp, FrostTheme.colors.outline, MaterialTheme.shapes.extraLarge)
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
                    .background(Brush.horizontalGradient(FrostTheme.colors.actionGradient))
            )
            Text(
                text = stringResource(R.string.onb_start_journey),
                style = MaterialTheme.typography.labelLarge,
                color = if (progress.value > 0.55f) FrostTheme.colors.onPrimary else FrostTheme.colors.textPrimary
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Hold for two seconds. No sign-up, nothing leaves this phone.",
            style = MaterialTheme.typography.labelSmall,
            color = FrostTheme.colors.textSecondary,
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
            tint = FrostTheme.colors.secondary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = FrostTheme.colors.textSecondary
        )
    }
}

// ---------------------------------------------------------------------------
// Backdrop
// ---------------------------------------------------------------------------

@Composable
private fun OnboardingAurora() {
    val transition = rememberInfiniteTransition(label = "onboarding aurora")
    val animatedDrift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraDrift"
    )
    val reducedAurora = rememberReducedMotion()
    val drift = if (reducedAurora) 0f else animatedDrift
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
