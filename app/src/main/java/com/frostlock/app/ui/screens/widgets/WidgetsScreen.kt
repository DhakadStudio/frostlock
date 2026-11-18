package com.frostlock.app.ui.screens.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.frostlock.app.ui.components.surface.FrostCard
import com.frostlock.app.ui.shell.FrostDetailScaffold
import com.frostlock.app.ui.theme.FrostTheme
import com.frostlock.app.widget.CalendarWidgetProvider
import com.frostlock.app.widget.DailyGoalWidgetProvider
import com.frostlock.app.widget.FocusWidgetProvider
import com.frostlock.app.widget.UnlockWidgetProvider
import com.frostlock.app.widget.UsageWidgetProvider
import com.frostlock.app.widget.WeeklyWidgetProvider
import com.frostlock.app.widget.WidgetPinning

private data class WidgetOption(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val provider: Class<out android.appwidget.AppWidgetProvider>
)

private val WIDGETS = listOf(
    WidgetOption(
        title = "Today",
        description = "Today's focus time and session count.",
        icon = Icons.Default.Today,
        provider = FocusWidgetProvider::class.java
    ),
    WidgetOption(
        title = "Daily target",
        description = "Progress toward your daily focus target.",
        icon = Icons.Default.Flag,
        provider = DailyGoalWidgetProvider::class.java
    ),
    WidgetOption(
        title = "This week",
        description = "The week at a glance, Monday to Sunday.",
        icon = Icons.Default.Widgets,
        provider = WeeklyWidgetProvider::class.java
    ),
    WidgetOption(
        title = "History",
        description = "A month heat-map of the days you focused.",
        icon = Icons.Default.CalendarMonth,
        provider = CalendarWidgetProvider::class.java
    ),
    WidgetOption(
        title = "Usage",
        description = "Daily screen time and how often you picked up the phone.",
        icon = Icons.Default.PhoneAndroid,
        provider = UsageWidgetProvider::class.java
    ),
    WidgetOption(
        title = "Unlocks",
        description = "How many times you unlocked the phone today.",
        icon = Icons.Default.Lock,
        provider = UnlockWidgetProvider::class.java
    )
)

/**
 * Widgets: the one place to pin a FrostLock widget.
 *
 * The old Profile page buried six buttons in a settings card. This page explains
 * each widget and stays honest when the launcher does not support pinning.
 */
@Composable
fun WidgetsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val supported = WidgetPinning.isSupported(context)

    FrostDetailScaffold(title = "Widgets", onBack = onBack) { padding ->
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
                Text(
                    text = "Keep your focus visible without opening the app.",
                    style = FrostTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
            }

            if (!supported) {
                item {
                    FrostCard {
                        Text(
                            text = "This launcher doesn't support pinning from inside an app.",
                            style = FrostTheme.typography.titleMedium,
                            color = FrostTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.padding(top = FrostTheme.spacing.sm))
                        Text(
                            text = "Long-press your home screen, choose Widgets, then pick " +
                                "FrostLock. Every widget below is still available there.",
                            style = FrostTheme.typography.bodyMedium,
                            color = FrostTheme.colors.textSecondary
                        )
                    }
                }
            }

            items(WIDGETS.size) { index ->
                val widget = WIDGETS[index]
                FrostCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = widget.icon,
                            contentDescription = null,
                            tint = FrostTheme.colors.secondary
                        )
                        Spacer(modifier = Modifier.width(FrostTheme.spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = widget.title,
                                style = FrostTheme.typography.titleMedium,
                                color = FrostTheme.colors.textPrimary
                            )
                            Text(
                                text = widget.description,
                                style = FrostTheme.typography.bodyMedium,
                                color = FrostTheme.colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(FrostTheme.spacing.sm))
                        TextButton(
                            onClick = {
                                WidgetPinning.request(context, widget.provider)
                            },
                            enabled = supported
                        ) {
                            Text(
                                text = "Pin",
                                style = FrostTheme.typography.labelLarge,
                                color = if (supported) {
                                    FrostTheme.colors.primary
                                } else {
                                    FrostTheme.colors.textSecondary
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "All widget data is calculated on this device.",
                    style = FrostTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
