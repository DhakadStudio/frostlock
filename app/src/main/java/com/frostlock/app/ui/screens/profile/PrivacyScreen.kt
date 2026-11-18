package com.frostlock.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.components.controls.FrostButton
import com.frostlock.app.ui.components.controls.FrostButtonVariant
import com.frostlock.app.ui.components.surface.FrostCard
import com.frostlock.app.ui.shell.FrostDetailScaffold
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Privacy: what FrostLock stores, what it reads, and why.
 *
 * It explains the permission model and links to Shield, but never duplicates a
 * protection toggle.
 */
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    onOpenShield: () -> Unit
) {
    FrostDetailScaffold(title = "Privacy", onBack = onBack) { padding ->
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = FrostTheme.colors.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(FrostTheme.spacing.sm))
                        Text(
                            text = "Works offline. Stays on this device.",
                            style = FrostTheme.typography.titleLarge,
                            color = FrostTheme.colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
                    Text(
                        text = "FrostLock has no account, no ads, no tracking and no " +
                            "internet permission. Everything below is stored only in the " +
                            "app's private storage on this phone.",
                        style = FrostTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }

            item {
                FrostCard {
                    Text(
                        text = "What is stored",
                        style = FrostTheme.typography.titleMedium,
                        color = FrostTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
                    BulletList(
                        listOf(
                            "Sessions: mode, start and end time, duration, subject, " +
                                "reflection and an optional photo.",
                            "Settings: daily target, theme, strict mode, pauses, " +
                                "reminders, sound and session alerts.",
                            "Protection: app lists, limits, schedules, websites and " +
                                "channels you choose."
                        )
                    )
                }
            }

            item {
                FrostCard {
                    Text(
                        text = "Permissions",
                        style = FrostTheme.typography.titleMedium,
                        color = FrostTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
                    Text(
                        text = "Each permission is requested only when you turn on the " +
                            "feature that needs it.",
                        style = FrostTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(FrostTheme.spacing.sm))
                    BulletList(
                        listOf(
                            "Accessibility holds apps while a focus session is running.",
                            "Notification access reads the notifications you choose to hold.",
                            "Display over other apps draws the optional floating timer.",
                            "Exact alarms keep schedule reminders on time.",
                            "Notifications show session alerts, reminders and limit warnings."
                        )
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
                        text = "FrostLock is free forever. There is nothing to subscribe to, " +
                            "and no data leaves your phone.",
                        style = FrostTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }

            item {
                FrostButton(
                    text = "Review protection in Shield",
                    onClick = onOpenShield,
                    variant = FrostButtonVariant.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = "Nothing leaves your device.",
                    style = FrostTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BulletList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)) {
        items.forEach { item ->
            Row {
                Text(
                    text = "•",
                    style = FrostTheme.typography.bodyMedium,
                    color = FrostTheme.colors.secondary
                )
                Spacer(modifier = Modifier.width(FrostTheme.spacing.sm))
                Text(
                    text = item,
                    style = FrostTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
            }
        }
    }
}
