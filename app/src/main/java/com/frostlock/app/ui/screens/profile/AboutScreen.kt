package com.frostlock.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.frostlock.app.ui.components.surface.FrostCard
import com.frostlock.app.ui.components.surface.StatusRow
import com.frostlock.app.ui.shell.FrostDetailScaffold
import com.frostlock.app.ui.theme.FrostTheme

/**
 * About: what FrostLock is, what it will never charge for, and where the
 * roadmap lives.
 *
 * The old Profile page advertised a paid tier; this page states the same
 * promise as a quiet line, and the roadmap is no longer mixed into settings.
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenDesignPreview: () -> Unit
) {
    FrostDetailScaffold(title = "About", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = FrostTheme.spacing.screenHorizontal,
                end = FrostTheme.spacing.screenHorizontal,
                top = FrostTheme.spacing.sm,
                bottom = FrostTheme.spacing.huge
            ),
            verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.md)
        ) {
            item {
                FrostCard {
                    Text(
                        text = "FrostLock",
                        style = FrostTheme.typography.headlineMedium,
                        color = FrostTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
                    Text(
                        text = "A free focus timer with a built-in app blocker. Track " +
                            "sessions, build a streak and keep distracting apps out of " +
                            "your way.",
                        style = FrostTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(FrostTheme.spacing.md))
                    Text(
                        text = "Free forever · no ads · no subscriptions",
                        style = FrostTheme.typography.labelLarge,
                        color = FrostTheme.colors.secondary
                    )
                }
            }

            item {
                FrostCard {
                    Text(
                        text = "Roadmap",
                        style = FrostTheme.typography.titleMedium,
                        color = FrostTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
                    Text(
                        text = "•  Difficulty tiers, from gentle to locked in",
                        style = FrostTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textSecondary
                    )
                    Text(
                        text = "•  Pause or schedule notification blocking",
                        style = FrostTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }

            item {
                FrostCard(
                    contentPadding = PaddingValues(
                        horizontal = FrostTheme.spacing.lg,
                        vertical = FrostTheme.spacing.xs
                    )
                ) {
                    StatusRow(
                        title = "Design preview",
                        status = "See the components and motion tokens",
                        icon = Icons.Default.Palette,
                        onClick = onOpenDesignPreview
                    )
                }
            }

            item {
                FrostCard {
                    Text(
                        text = "No account. No ads. No tracking.",
                        style = FrostTheme.typography.titleMedium,
                        color = FrostTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(FrostTheme.spacing.xs))
                    Text(
                        text = "FrostLock works offline. Your sessions, reflections and " +
                            "settings stay on this device.",
                        style = FrostTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }

            item {
                Text(
                    text = "Made for focused work.",
                    style = FrostTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
