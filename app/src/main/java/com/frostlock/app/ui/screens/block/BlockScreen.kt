package com.frostlock.app.ui.screens.block

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.frostlock.app.data.InstalledApp
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.PrimaryButton
import com.frostlock.app.ui.components.SectionHeader
import com.frostlock.app.ui.theme.AccentFrost
import com.frostlock.app.ui.theme.GradientHero
import com.frostlock.app.ui.theme.BackgroundDark
import com.frostlock.app.ui.theme.DangerRed
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
import com.frostlock.app.ui.util.SUGGESTED_LIMIT_MINUTES
import com.frostlock.app.ui.util.suggestedLimitApps

private enum class BlockTab(val label: String) {
    BLOCKING("Focus blocking"),
    LIMITS("Daily limits")
}

@Composable
fun BlockScreen(
    onOpenSchedules: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    viewModel: BlockViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var tab by rememberSaveable { mutableStateOf(BlockTab.BLOCKING) }
    var blockQuery by rememberSaveable { mutableStateOf("") }
    var limitQuery by rememberSaveable { mutableStateOf("") }
    var selectedOnly by rememberSaveable { mutableStateOf(false) }
    var limitTarget by remember { mutableStateOf<InstalledApp?>(null) }

    // Re-check the accessibility permission, usage and rules when the user returns.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAccessibilityStatus()
                viewModel.refreshNotificationAccess()
                viewModel.refreshLimits()
                viewModel.refreshSchedules()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val blockApps = remember(state.apps, blockQuery, selectedOnly, state.blockedPackages) {
        state.apps
            .filter { blockQuery.isBlank() || it.label.contains(blockQuery, ignoreCase = true) }
            .filter { !selectedOnly || it.packageName in state.blockedPackages }
    }
    val limitApps = remember(state.apps, limitQuery) {
        state.apps.filter {
            limitQuery.isBlank() || it.label.contains(limitQuery, ignoreCase = true)
        }
    }
    val suggestedApps = remember(state.apps, state.limits) {
        suggestedLimitApps(state.apps, state.limits)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "App Blocker",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Two independent tools: block apps during focus, and cap daily use.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        item {
            BlockTabSwitcher(
                selected = tab,
                blockedCount = state.blockedCount,
                limitedCount = state.limitedCount,
                onSelect = { tab = it }
            )
        }

        when (tab) {
            BlockTab.BLOCKING -> {
                item {
                    BlockingToggleCard(
                        enabled = state.blockingEnabled,
                        onToggle = viewModel::setBlockingEnabled
                    )
                }

                if (!state.accessibilityEnabled) {
                    item {
                        AppCard {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = WarnAmber
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Permission needed",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "FrostLock needs the accessibility permission to detect when you open a blocked app. It never reads what you type or see.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                PrimaryButton(
                                    text = "Open accessibility settings",
                                    onClick = viewModel::openAccessibilitySettings
                                )
                            }
                        }
                    }
                }

                item {
                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = AccentFrost,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Blocking rules",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = when {
                                        state.schedules.isEmpty() ->
                                            "No rules yet — add one for different apps, days or dates"
                                        state.activeRuleLabels.isNotEmpty() ->
                                            "${state.schedules.size} rule(s) · running ${
                                                state.activeRuleLabels.joinToString(", ")
                                            }"
                                        else ->
                                            "${state.schedules.size} rule(s) · none active right now"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (state.activeRuleLabels.isNotEmpty()) {
                                        SuccessGreen
                                    } else {
                                        TextSecondary
                                    }
                                )
                            }
                            TextButton(onClick = onOpenSchedules) {
                                Text(text = "Manage", color = AccentFrost)
                            }
                        }
                    }
                }

                item {
                    NotificationShieldCard(
                        state = state.notifications,
                        onToggle = viewModel::setNotificationBlocking,
                        onOpenInbox = onOpenNotifications,
                        onGrantAccess = viewModel::openNotificationAccessSettings
                    )
                }

                item {
                    SearchField(
                        value = blockQuery,
                        onValueChange = { blockQuery = it },
                        placeholder = "Search apps to block"
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(
                            title = "Focus session apps · ${state.blockedCount} selected",
                            modifier = Modifier.weight(1f)
                        )
                        if (state.blockedCount > 0) {
                            TextButton(onClick = { selectedOnly = !selectedOnly }) {
                                Text(
                                    text = if (selectedOnly) "Show all" else "Selected only",
                                    color = AccentFrost
                                )
                            }
                        }
                    }
                }

                if (state.loading) {
                    item { LoadingRow() }
                } else if (blockApps.isEmpty()) {
                    item {
                        AppCard {
                            Text(
                                text = "No apps match that search.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    items(blockApps, key = { it.packageName }) { app ->
                        BlockAppRow(
                            app = app,
                            checked = app.packageName in state.blockedPackages,
                            onToggle = { viewModel.togglePackage(app.packageName) }
                        )
                    }
                }
            }

            BlockTab.LIMITS -> {
                item {
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = AccentFrost
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Daily limits are independent",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "They apply on their own, even when focus blocking is off. Budgets reset at midnight.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                if (suggestedApps.isNotEmpty()) {
                    item { SectionHeader(title = "Suggested limits") }
                    item {
                        AppCard {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "The usual time sinks. One tap gives each a " +
                                        "${SUGGESTED_LIMIT_MINUTES}-minute daily budget.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                suggestedApps.forEach { app ->
                                    SuggestedLimitRow(
                                        app = app,
                                        onAdd = {
                                            viewModel.setLimit(
                                                app.packageName,
                                                SUGGESTED_LIMIT_MINUTES
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SearchField(
                        value = limitQuery,
                        onValueChange = { limitQuery = it },
                        placeholder = "Search apps to limit"
                    )
                }

                item {
                    SectionHeader(
                        title = if (state.limitedCount > 0) {
                            "Daily limits · ${state.limitedCount} set"
                        } else {
                            "Daily limits"
                        }
                    )
                }

                if (state.loading) {
                    item { LoadingRow() }
                } else if (limitApps.isEmpty()) {
                    item {
                        AppCard {
                            Text(
                                text = "No apps match that search.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    items(limitApps, key = { it.packageName }) { app ->
                        LimitAppRow(
                            app = app,
                            limitMinutes = state.limits[app.packageName] ?: 0,
                            usedMinutes = ((state.usageMillis[app.packageName] ?: 0L) / 60_000L).toInt(),
                            onSetLimit = { limitTarget = app }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    limitTarget?.let { target ->
        LimitDialog(
            appLabel = target.label,
            current = state.limits[target.packageName] ?: 0,
            usedMinutes = ((state.usageMillis[target.packageName] ?: 0L) / 60_000L).toInt(),
            onSave = { minutes ->
                viewModel.setLimit(target.packageName, minutes)
                limitTarget = null
            },
            onDismiss = { limitTarget = null }
        )
    }
}

@Composable
private fun BlockTabSwitcher(
    selected: BlockTab,
    blockedCount: Int,
    limitedCount: Int,
    onSelect: (BlockTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(SurfaceCard)
            .border(1.dp, StrokeSoft, MaterialTheme.shapes.extraLarge)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        BlockTab.entries.forEach { entry ->
            val isSelected = entry == selected
            val badge = when (entry) {
                BlockTab.BLOCKING -> blockedCount
                BlockTab.LIMITS -> limitedCount
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        if (isSelected) {
                            Brush.linearGradient(GradientFrost)
                        } else {
                            androidx.compose.ui.graphics.SolidColor(Color.Transparent)
                        }
                    )
                    .clickable { onSelect(entry) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badge > 0) "${entry.label} · $badge" else entry.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun BlockingToggleCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(Brush.horizontalGradient(GradientHero))
            .border(1.dp, StrokeSoft, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) {
                        Brush.linearGradient(GradientFrost)
                    } else {
                        androidx.compose.ui.graphics.SolidColor(SurfaceHigh)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = if (enabled) InkOnIce else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Focus blocking",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Text(
                text = if (enabled) {
                    "On — selected apps lock during focus sessions"
                } else {
                    "Off — schedules and daily limits still apply"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) SuccessGreen else TextSecondary
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryIce,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceHigh
            )
        )
    }
}

@Composable
private fun NotificationShieldCard(
    state: NotificationUiState,
    onToggle: (Boolean) -> Unit,
    onOpenInbox: () -> Unit,
    onGrantAccess: () -> Unit
) {
    val active = state.enabled && state.accessGranted

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) {
                                Brush.linearGradient(GradientFrost)
                            } else {
                                androidx.compose.ui.graphics.SolidColor(SurfaceHigh)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = if (active) InkOnIce else TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notification shield",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = when {
                            state.enabled && !state.accessGranted ->
                                "On — notification access still needs to be granted"
                            state.enabled ->
                                "On — delaying alerts from blocked apps"
                            else ->
                                "Off — alerts arrive normally"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            state.enabled && !state.accessGranted -> WarnAmber
                            state.enabled -> SuccessGreen
                            else -> TextSecondary
                        }
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = state.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryIce,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceHigh
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!state.accessGranted) {
                    TextButton(onClick = onGrantAccess) {
                        Text(text = "Grant notification access", color = AccentFrost)
                    }
                } else {
                    TextButton(onClick = onOpenInbox) {
                        Text(
                            text = if (state.total > 0) {
                                "Open inbox · ${state.total}"
                            } else {
                                "Open inbox"
                            },
                            color = AccentFrost
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (state.heldToday > 0) {
                        Text(
                            text = "${state.heldToday} held today",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = TextSecondary) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextSecondary
            )
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryIce,
            unfocusedBorderColor = StrokeSoft,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = AccentFrost
        ),
        shape = MaterialTheme.shapes.extraLarge
    )
}

@Composable
private fun LoadingRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = AccentFrost)
    }
}

@Composable
private fun AppIdentity(app: InstalledApp) {
    if (app.icon != null) {
        Image(
            bitmap = app.icon,
            contentDescription = null,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.label.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = AccentFrost
            )
        }
    }
}

@Composable
private fun BlockAppRow(
    app: InstalledApp,
    checked: Boolean,
    onToggle: () -> Unit
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIdentity(app)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        if (checked) {
                            Brush.linearGradient(GradientFrost)
                        } else {
                            androidx.compose.ui.graphics.SolidColor(SurfaceHigh)
                        }
                    )
                    .border(
                        1.dp,
                        if (checked) Color.Transparent else StrokeSoft,
                        CircleShape
                    )
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Blocked",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LimitAppRow(
    app: InstalledApp,
    limitMinutes: Int,
    usedMinutes: Int,
    onSetLimit: () -> Unit
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIdentity(app)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1
                )
                if (limitMinutes > 0) {
                    Text(
                        text = "$usedMinutes / $limitMinutes min today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (usedMinutes >= limitMinutes) DangerRed else TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = {
                            (usedMinutes.toFloat() / limitMinutes.toFloat()).coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = if (usedMinutes >= limitMinutes) DangerRed else PrimaryIce,
                        trackColor = SurfaceHigh
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(if (limitMinutes > 0) SurfaceHigh else Color.Transparent)
                    .border(
                        1.dp,
                        if (limitMinutes > 0) StrokeSoft else SurfaceHigh,
                        MaterialTheme.shapes.extraLarge
                    )
                    .clickable(onClick = onSetLimit)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (limitMinutes > 0) "Edit" else "Set limit",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentFrost
                )
            }
        }
    }
}

@Composable
private fun SuggestedLimitRow(
    app: InstalledApp,
    onAdd: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIdentity(app)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1
            )
            Text(
                text = "${SUGGESTED_LIMIT_MINUTES}m daily budget",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .background(Brush.linearGradient(GradientFrost))
                .clickable(onClick = onAdd)
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(
                text = "Add ${SUGGESTED_LIMIT_MINUTES}m",
                style = MaterialTheme.typography.labelMedium,
                color = InkOnIce,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LimitDialog(
    appLabel: String,
    current: Int,
    usedMinutes: Int,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var minutes by remember { mutableIntStateOf(if (current > 0) current else DEFAULT_LIMIT_MINUTES) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceCard)
                .border(1.dp, StrokeSoft, RoundedCornerShape(28.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily limit",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = appLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(SurfaceHigh)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "$usedMinutes min used today · resets at midnight",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentFrost
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepperButton(text = "−", onClick = {
                    minutes = (minutes - 5).coerceAtLeast(5)
                })
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$minutes",
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "minutes / day",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                StepperButton(text = "+", onClick = {
                    minutes = (minutes + 5).coerceAtMost(MAX_LIMIT_MINUTES)
                })
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LIMIT_PRESETS.forEach { preset ->
                    FilterChip(
                        selected = minutes == preset,
                        onClick = { minutes = preset },
                        label = { Text("${preset}m") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SurfaceHigh,
                            labelColor = TextSecondary,
                            selectedContainerColor = PrimaryIce,
                            selectedLabelColor = InkOnIce
                        )
                    )
                }
            }

            if (current > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .border(1.dp, DangerRed.copy(alpha = 0.4f), CircleShape)
                        .clickable { onSave(0) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Remove limit",
                        style = MaterialTheme.typography.labelLarge,
                        color = DangerRed
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Cancel", color = TextSecondary)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(GradientFrost))
                        .clickable { onSave(minutes) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save limit",
                        style = MaterialTheme.typography.labelLarge,
                        color = InkOnIce
                    )
                }
            }
        }
    }
}

@Composable
private fun StepperButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(SurfaceHigh)
            .border(1.dp, StrokeSoft, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
    }
}

private val LIMIT_PRESETS = listOf(15, 30, 60, 120)
private const val DEFAULT_LIMIT_MINUTES = 30
private const val MAX_LIMIT_MINUTES = 480
