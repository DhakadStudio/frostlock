package com.frostlock.app.ui.screens.stats

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.ui.components.AchievementBadge
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.SectionHeader
import com.frostlock.app.ui.util.Achievement
import com.frostlock.app.ui.util.AchievementCategory
import com.frostlock.app.ui.util.achievementCategoryLabel
import com.frostlock.app.ui.util.achievementShareText
import com.frostlock.app.ui.components.surface.frostPageBackground
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Full badge cabinet. Everything is derived from locally stored sessions, so no
 * account or sync is needed to unlock anything.
 */
@Composable
fun BadgesScreen(
    onBack: () -> Unit,
    viewModel: com.frostlock.app.ui.screens.insights.BadgesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val achievements = state.achievements
    val total = achievements.size
    val unlocked = state.unlockedCount
    var selected by remember { mutableStateOf<Achievement?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .frostPageBackground()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = FrostTheme.colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Badges",
                    style = MaterialTheme.typography.headlineMedium,
                    color = FrostTheme.colors.textPrimary
                )
                Text(
                    text = "Milestones from your focus history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
            }
        }

        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AcUnit,
                        contentDescription = null,
                        tint = FrostTheme.colors.secondary,
                        modifier = Modifier.width(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$unlocked of $total collected",
                            style = MaterialTheme.typography.titleLarge,
                            color = FrostTheme.colors.textPrimary
                        )
                        Text(
                            text = if (unlocked == total && total > 0) {
                                "Every badge is frozen in place. Outstanding."
                            } else {
                                "Keep focusing to thaw the next one."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = FrostTheme.colors.textSecondary
                        )
                    }
                }
                ProgressBar(
                    progress = if (total == 0) 0f else unlocked.toFloat() / total.toFloat()
                )
            }
        }

        AchievementCategory.entries.forEach { category ->
            val items = achievements.filter { it.category == category }
            if (items.isEmpty()) return@forEach

            SectionHeader(title = achievementCategoryLabel(category))
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    items.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { achievement ->
                                AchievementTile(
                                    achievement = achievement,
                                    modifier = Modifier.weight(1f),
                                    onClick = { selected = achievement }
                                )
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    selected?.let { achievement ->
        AchievementShareDialog(
            achievement = achievement,
            onDismiss = { selected = null }
        )
    }
}

@Composable
private fun AchievementShareDialog(
    achievement: Achievement,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(FrostTheme.colors.surfaceContainer)
                .border(1.dp, FrostTheme.colors.outline, RoundedCornerShape(28.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AchievementBadge(achievement = achievement, size = 92.dp)
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.headlineMedium,
                color = FrostTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Text(
                text = achievement.progressLabel,
                style = MaterialTheme.typography.labelMedium,
                color = FrostTheme.colors.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Close", color = FrostTheme.colors.textSecondary)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(Brush.horizontalGradient(FrostTheme.colors.actionGradient))
                        .clickable {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, achievementShareText(achievement))
                            }
                            context.startActivity(
                                Intent.createChooser(intent, "Share achievement")
                            )
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Share",
                        style = MaterialTheme.typography.labelLarge,
                        color = FrostTheme.colors.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementTile(
    achievement: Achievement,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier.clickable(enabled = achievement.unlocked, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        AchievementBadge(achievement = achievement, size = 62.dp)
        Text(
            text = achievement.title,
            style = MaterialTheme.typography.labelMedium,
            color = if (achievement.unlocked) FrostTheme.colors.textPrimary else FrostTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Text(
            text = achievement.progressLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (achievement.unlocked) FrostTheme.colors.secondary else FrostTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(FrostTheme.colors.surfaceContainerHigh)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(listOf(FrostTheme.colors.primary, FrostTheme.colors.glacier)))
        )
    }
}
