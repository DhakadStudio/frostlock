package com.focusflow.app.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.focusflow.app.R
import com.focusflow.app.ui.components.AppCard
import com.focusflow.app.ui.components.MoodChip
import com.focusflow.app.ui.components.PrimaryButton
import com.focusflow.app.ui.components.SecondaryButton
import com.focusflow.app.ui.components.SectionHeader
import com.focusflow.app.ui.theme.AccentPurple
import com.focusflow.app.ui.theme.BackgroundDark
import com.focusflow.app.ui.theme.PrimaryPurple
import com.focusflow.app.ui.theme.FocusFlowTheme
import com.focusflow.app.ui.theme.SurfaceCard
import com.focusflow.app.ui.theme.TextPrimary
import com.focusflow.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit,
    onExit: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 5 })
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(5) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 10.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) PrimaryPurple else SurfaceCard)
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 24.dp),
            userScrollEnabled = false
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> GoalsPage(uiState = uiState, onGoalToggled = viewModel::onGoalToggled)
                2 -> TriggersPage(uiState = uiState, onTriggerToggled = viewModel::onTriggerToggled)
                3 -> ReasonPage(reason = uiState.reason, onReasonChanged = viewModel::onReasonChanged)
                4 -> CommitmentPage(
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

            if (pagerState.currentPage < 4) {
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

@Composable
fun WelcomePage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.onb_welcome_title),
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onb_welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
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
        SectionHeader(title = stringResource(R.string.onb_goals_title))
        Spacer(modifier = Modifier.height(16.dp))
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
        SectionHeader(title = stringResource(R.string.onb_triggers_title))
        Spacer(modifier = Modifier.height(16.dp))
        triggers.forEach { trigger ->
            val selected = uiState.selectedTriggers.contains(trigger)
            Spacer(modifier = Modifier.height(8.dp))
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
        SectionHeader(title = stringResource(R.string.onb_reason_title))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = reason,
            onValueChange = onReasonChanged,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            placeholder = { Text(text = stringResource(R.string.onb_reason_hint), color = TextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = SurfaceCard,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = MaterialTheme.shapes.medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Characters: ${reason.trim().length} / 300 (min 10)",
            style = MaterialTheme.typography.bodyMedium,
            color = if (reason.trim().length in 10..300) AccentPurple else TextSecondary
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
        SectionHeader(title = stringResource(R.string.onb_commitment_title))
        Spacer(modifier = Modifier.height(16.dp))
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Goals: ${uiState.selectedGoals.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Triggers: ${uiState.selectedTriggers.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Reason: ${uiState.reason}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(SurfaceCard)
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
                                        progress.animateTo(0f, animationSpec = tween(durationMillis = 300))
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
                    .matchParentSize()
                    .fillMaxWidth(progress.value)
                    .background(PrimaryPurple)
            )
            Text(
                text = stringResource(R.string.onb_start_journey),
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0A1F)
@Composable
fun OnboardingScreenPreview() {
    FocusFlowTheme {
    }
}
