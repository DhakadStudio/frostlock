package com.focusflow.app.ui.screens.block

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.focusflow.app.ui.components.AppCard
import com.focusflow.app.ui.components.PrimaryButton
import com.focusflow.app.ui.components.SectionHeader
import com.focusflow.app.ui.theme.AccentPurple
import com.focusflow.app.ui.theme.BackgroundDark
import com.focusflow.app.ui.theme.DangerRed
import com.focusflow.app.ui.theme.GradientViolet
import com.focusflow.app.ui.theme.PrimaryPurple
import com.focusflow.app.ui.theme.StrokeSoft
import com.focusflow.app.ui.theme.SuccessGreen
import com.focusflow.app.ui.theme.SurfaceCard
import com.focusflow.app.ui.theme.SurfaceHigh
import com.focusflow.app.ui.theme.TextPrimary
import com.focusflow.app.ui.theme.TextSecondary
import com.focusflow.app.ui.theme.WarnAmber

@Composable
fun BlockScreen(
    onOpenSchedules: () -> Unit = {},
    viewModel: BlockViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var limitTarget by remember { mutableStateOf<InstalledApp?>(null) }

    // Re-check the accessibility permission and today's usage when the user returns.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAccessibilityStatus()
                viewModel.refreshLimits()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val visibleApps = remember(state.apps, query) {
        if (query.isBlank()) {
            state.apps
        } else {
            state.apps.filter { it.label.contains(query, ignoreCase = true) }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
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
                    text = "Lock distracting apps during sessions, and cap daily use.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Master toggle hero
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF2E2560), Color(0xFF1B1637))
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
                        .background(
                            if (state.blockingEnabled) {
                                Brush.linearGradient(GradientViolet)
                            } else {
                                androidx.compose.ui.graphics.SolidColor(SurfaceHigh)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (state.blockingEnabled) Color.White else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Blocking",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = if (state.blockingEnabled) {
                            "Active — apps are protected"
                        } else {
                            "Off — turn on to protect focus"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.blockingEnabled) SuccessGreen else TextSecondary
                    )
                }
                Switch(
                    checked = state.blockingEnabled,
                    onCheckedChange = viewModel::setBlockingEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryPurple,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceHigh
                    )
                )
            }
        }

        // Permission banner (only when missing, keeps the screen compact)
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
                            text = "FocusFlow needs the accessibility permission to detect when you open a blocked app. It never reads what you type or see.",
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

        // Schedules card
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
                            tint = AccentPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Schedules",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Auto-block during weekly windows",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    TextButton(onClick = onOpenSchedules) {
                        Text(text = "Manage", color = AccentPurple)
                    }
                }
            }
        }

        // Search
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps", color = TextSecondary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = StrokeSoft,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentPurple
                ),
                shape = MaterialTheme.shapes.extraLarge
            )
        }

        // App list header
        item {
            SectionHeader(
                title = if (state.blockedPackages.isEmpty()) {
                    "All apps"
                } else {
                    "All apps · ${state.blockedPackages.size} blocked"
                }
            )
        }

        item {
            Text(
                text = "Tap the switch to block during focus · “Limit” caps daily use.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        if (state.loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentPurple)
                }
            }
        } else if (visibleApps.isEmpty()) {
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
            items(visibleApps, key = { it.packageName }) { app ->
                AppRow(
                    app = app,
                    checked = app.packageName in state.blockedPackages,
                    limitMinutes = state.limits[app.packageName] ?: 0,
                    usedMinutes = ((state.usageMillis[app.packageName] ?: 0L) / 60_000L).toInt(),
                    onToggle = { viewModel.togglePackage(app.packageName) },
                    onSetLimit = { limitTarget = app }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    limitTarget?.let { target ->
        LimitDialog(
            appLabel = target.label,
            current = state.limits[target.packageName] ?: 0,
            usedMinutes = ((state.usageMillis[target.packageName] ?: 0L) / 60_000L).toInt(),
            onSelect = { minutes ->
                viewModel.setLimit(target.packageName, minutes)
                limitTarget = null
            },
            onDismiss = { limitTarget = null }
        )
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    checked: Boolean,
    limitMinutes: Int,
    usedMinutes: Int,
    onToggle: () -> Unit,
    onSetLimit: () -> Unit
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                        color = AccentPurple
                    )
                }
            }

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
                        text = "$limitMinutes min/day · ${usedMinutes}m used",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentPurple
                    )
                }
            }

            // Limit pill
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
                    text = if (limitMinutes > 0) "Edit" else "Limit",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentPurple
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Block toggle (checkbox style, like Regain)
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        if (checked) {
                            Brush.linearGradient(GradientViolet)
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
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LimitDialog(
    appLabel: String,
    current: Int,
    usedMinutes: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "Daily limit",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "How long can $appLabel be used each day?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
                Text(
                    text = "$usedMinutes min used today",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AccentPurple
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LIMIT_OPTIONS.forEach { minutes ->
                        FilterChip(
                            selected = current == minutes,
                            onClick = { onSelect(minutes) },
                            label = {
                                Text(text = if (minutes == 0) "Off" else "${minutes}m")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceHigh,
                                labelColor = TextSecondary,
                                selectedContainerColor = PrimaryPurple,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = AccentPurple)
            }
        }
    )
}

private val LIMIT_OPTIONS = listOf(0, 15, 30, 45, 60, 90, 120)
