package com.frostlock.app.ui.screens.shield

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.data.InstalledApp
import com.frostlock.app.ui.components.controls.FrostButton
import com.frostlock.app.ui.components.controls.FrostButtonVariant
import com.frostlock.app.ui.components.controls.FrostSearchField
import com.frostlock.app.ui.components.controls.FrostSwitchRow
import com.frostlock.app.ui.components.surface.FrostCard
import com.frostlock.app.ui.components.surface.PermissionCard
import com.frostlock.app.ui.components.surface.StatusRow
import com.frostlock.app.ui.shell.FrostDetailScaffold
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Shield → Apps.
 *
 * One job: choose which apps are held during focus. Schedules, websites and
 * channels each have their own screen, so nothing here competes for attention.
 */
@Composable
fun AppsScreen(
    onBack: () -> Unit,
    onOpenAdvanced: () -> Unit,
    viewModel: AppsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var selectedOnly by rememberSaveable { mutableStateOf(false) }

    val visibleApps = remember(state.apps, query, selectedOnly, state.blockedPackages) {
        state.apps
            .filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
            .filter { !selectedOnly || it.packageName in state.blockedPackages }
    }

    FrostDetailScaffold(title = "Apps", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = FrostTheme.spacing.xl,
                vertical = FrostTheme.spacing.md
            ),
            verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.md)
        ) {
            item {
                FrostCard {
                    FrostSwitchRow(
                        title = "Focus blocking",
                        description = when {
                            !state.ready -> "Access is off — nothing can be held."
                            state.blockingEnabled ->
                                "Selected apps are held during focus and schedules."
                            else -> "Off — schedules and limits still apply."
                        },
                        checked = state.blockingEnabled,
                        onCheckedChange = viewModel::setBlockingEnabled
                    )
                }
            }

            if (!state.ready) {
                item {
                    PermissionCard(
                        title = "Access needed",
                        message = "FrostLock needs accessibility access to hold apps during focus. It reads only the app in front, and nothing leaves this device.",
                        actionLabel = "Open accessibility settings",
                        onAction = viewModel::openAccessibilitySettings
                    )
                }
            }

            item {
                FrostCard(
                    onClick = onOpenAdvanced,
                    contentPadding = PaddingValues(
                        horizontal = FrostTheme.spacing.lg,
                        vertical = FrostTheme.spacing.xs
                    )
                ) {
                    StatusRow(
                        title = "Advanced",
                        status = "Home screen, YouTube home, Play Store",
                        icon = Icons.Filled.Tune,
                        onClick = null
                    )
                }
            }

            item {
                FrostSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search apps"
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Focus apps · ${state.blockedCount} selected",
                        style = FrostTheme.typography.titleLarge,
                        color = FrostTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.blockedCount > 0) {
                        FrostButton(
                            text = if (selectedOnly) "Show all" else "Selected only",
                            onClick = { selectedOnly = !selectedOnly },
                            variant = FrostButtonVariant.GHOST
                        )
                    }
                }
            }

            when {
                state.loading -> item { LoadingRow() }
                visibleApps.isEmpty() -> item {
                    FrostCard {
                        Text(
                            text = if (query.isBlank()) {
                                "No apps found on this device."
                            } else {
                                "No apps match that search."
                            },
                            style = FrostTheme.typography.bodyMedium,
                            color = FrostTheme.colors.textSecondary
                        )
                    }
                }
                else -> items(visibleApps, key = { it.packageName }) { app ->
                    AppSelectRow(
                        app = app,
                        checked = app.packageName in state.blockedPackages,
                        onToggle = { viewModel.togglePackage(app.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppSelectRow(
    app: InstalledApp,
    checked: Boolean,
    onToggle: () -> Unit
) {
    FrostCard(
        onClick = onToggle,
        contentPadding = PaddingValues(
            horizontal = FrostTheme.spacing.lg,
            vertical = FrostTheme.spacing.md
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "${app.label}, ${if (checked) "selected" else "not selected"}"
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShieldAppIcon(app)
            Spacer(modifier = Modifier.width(FrostTheme.spacing.md))
            Text(
                text = app.label,
                style = FrostTheme.typography.titleMedium,
                color = FrostTheme.colors.textPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        if (checked) {
                            FrostTheme.colors.primary
                        } else {
                            FrostTheme.colors.surfaceContainerHigh
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (checked) {
                            FrostTheme.colors.primary
                        } else {
                            FrostTheme.colors.outline
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = FrostTheme.colors.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun LoadingRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FrostTheme.spacing.xxxl),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = FrostTheme.colors.secondary)
    }
}

