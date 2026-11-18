package com.frostlock.app.ui.screens.notifications

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.frostlock.app.notifications.BlockReason
import com.frostlock.app.notifications.BlockedNotification
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.PrimaryButton
import com.frostlock.app.ui.components.SectionHeader
import com.frostlock.app.ui.util.NotificationAppTotal
import com.frostlock.app.ui.util.NotificationDay
import com.frostlock.app.ui.util.formatSessionTime
import com.frostlock.app.ui.util.notificationDayLabel
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Inbox of notifications that were held while an app was blocked. Everything is
 * local; deleting an entry removes it from the device permanently.
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    // Notification access is granted in system settings, so re-check on return.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshAccess()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(FrostTheme.colors.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = FrostTheme.colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Notification shield",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FrostTheme.colors.textPrimary
                    )
                    Text(
                        text = "Held locally while an app was blocked.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }
        }

        item {
            ShieldCard(
                enabled = state.enabled,
                accessGranted = state.accessGranted,
                total = state.total,
                onToggle = viewModel::setEnabled,
                onGrantAccess = viewModel::openAccessSettings
            )
        }

        if (state.isEmpty) {
            item { EmptyInbox(accessGranted = state.accessGranted, enabled = state.enabled) }
        } else {
            item {
                AppRankingCard(totals = state.appTotals, icons = state.icons)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = "Inbox · ${state.total} held",
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = viewModel::clearAll) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = FrostTheme.colors.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Clear all", color = FrostTheme.colors.secondary)
                    }
                }
            }

            state.days.forEach { day ->
                item(key = "day-${day.date}") {
                    DayHeader(day = day)
                }
                items(day.items, key = { "held-${it.id}" }) { item ->
                    HeldNotificationCard(
                        item = item,
                        icon = state.icons[item.packageName],
                        onDelete = { viewModel.remove(item) }
                    )
                }
            }
        }

        // Keep the last card clear of the gesture bar (this route hides the bottom bar).
        item { Spacer(modifier = Modifier.height(8.dp).navigationBarsPadding()) }
    }
}

@Composable
private fun ShieldCard(
    enabled: Boolean,
    accessGranted: Boolean,
    total: Int,
    onToggle: (Boolean) -> Unit,
    onGrantAccess: () -> Unit
) {
    val active = enabled && accessGranted

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) {
                                Brush.linearGradient(FrostTheme.colors.actionGradient)
                            } else {
                                androidx.compose.ui.graphics.SolidColor(FrostTheme.colors.surfaceContainerHigh)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = if (active) FrostTheme.colors.onPrimary else FrostTheme.colors.textSecondary,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (active) "Shield is on" else "Shield is off",
                        style = MaterialTheme.typography.titleLarge,
                        color = FrostTheme.colors.textPrimary
                    )
                    Text(
                        text = when {
                            enabled && !accessGranted ->
                                "Grant notification access so FrostLock can hold alerts."
                            enabled ->
                                "Alerts from blocked apps wait here instead of interrupting you."
                            else ->
                                "Turn on to hold distracting alerts during focus."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled && !accessGranted) FrostTheme.colors.warning else FrostTheme.colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = FrostTheme.colors.primary,
                        uncheckedThumbColor = FrostTheme.colors.textSecondary,
                        uncheckedTrackColor = FrostTheme.colors.surfaceContainerHigh
                    )
                )
            }

            if (!accessGranted) {
                PrimaryButton(
                    text = "Grant notification access",
                    onClick = onGrantAccess
                )
            } else {
                Text(
                    text = if (total > 0) {
                        "$total notification${if (total == 1) "" else "s"} held on this device"
                    } else {
                        "Nothing has been held yet."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun AppRankingCard(
    totals: List<NotificationAppTotal>,
    icons: Map<String, androidx.compose.ui.graphics.ImageBitmap?>
) {
    val max = totals.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Most held apps",
                    style = MaterialTheme.typography.titleLarge,
                    color = FrostTheme.colors.textPrimary
                )
                Text(
                    text = "Where the interruptions came from.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary
                )
            }
            totals.forEach { total ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(label = total.appLabel, icon = icons[total.packageName])
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = total.appLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = FrostTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = total.count.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                color = FrostTheme.colors.secondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(FrostTheme.colors.surfaceContainerHigh)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(total.count.toFloat() / max.toFloat())
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(Brush.horizontalGradient(FrostTheme.colors.actionGradient))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(day: NotificationDay) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = notificationDayLabel(day.date),
            style = MaterialTheme.typography.titleMedium,
            color = FrostTheme.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${day.items.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = FrostTheme.colors.textSecondary
        )
    }
}

@Composable
private fun HeldNotificationCard(
    item: BlockedNotification,
    icon: androidx.compose.ui.graphics.ImageBitmap?,
    onDelete: () -> Unit
) {
    val reasonColor = when (item.reason) {
        BlockReason.FOCUS -> FrostTheme.colors.secondary
        BlockReason.SCHEDULE -> FrostTheme.colors.glacier
        BlockReason.LIMIT -> FrostTheme.colors.warning
    }

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(label = item.appLabel, icon = icon)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.appLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = FrostTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatSessionTime(item.postedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = FrostTheme.colors.textSecondary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = FrostTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = FrostTheme.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.text.isNotBlank()) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(FrostTheme.colors.surfaceContainerHigh)
                        .border(1.dp, FrostTheme.colors.outline, MaterialTheme.shapes.extraLarge)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Held by ${item.ruleLabel ?: item.reason.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = reasonColor
                    )
                }
            }
        }
    }
}

@Composable
private fun AppIcon(label: String, icon: androidx.compose.ui.graphics.ImageBitmap?) {
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(FrostTheme.colors.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = FrostTheme.colors.secondary
            )
        }
    }
}

@Composable
private fun EmptyInbox(accessGranted: Boolean, enabled: Boolean) {
    AppCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(FrostTheme.colors.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AcUnit,
                    contentDescription = null,
                    tint = FrostTheme.colors.secondary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = "Nothing held yet",
                style = MaterialTheme.typography.titleMedium,
                color = FrostTheme.colors.textPrimary
            )
            Text(
                text = when {
                    !enabled ->
                        "Turn on the shield and distracting alerts will wait here during focus."
                    !accessGranted ->
                        "Grant notification access, then alerts from blocked apps will appear here."
                    else ->
                        "The next alert from a blocked app will wait here instead of interrupting you."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = FrostTheme.colors.textSecondary
            )
        }
    }
}
