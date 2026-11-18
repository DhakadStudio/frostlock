package com.frostlock.app.ui.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.frostlock.app.insights.SubjectFocus
import com.frostlock.app.ui.theme.FrostTheme
import com.frostlock.app.ui.util.formatDuration
import kotlin.math.roundToInt

/**
 * 100% stacked bar of a period's subjects.
 *
 * Segments are separated by a small gap so adjacent subject colours never blend
 * into a new, meaningless colour.
 */
@Composable
fun SubjectMixBar(
    subjects: List<SubjectFocus>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 10.dp
) {
    if (subjects.isEmpty()) return
    val shape = RoundedCornerShape(height / 2)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .semantics {
                contentDescription = subjects.joinToString(", ") { subject ->
                    "${subject.tag} ${percent(subject.share)}%"
                }
            },
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        subjects.forEach { subject ->
            Spacer(
                modifier = Modifier
                    .weight(subject.share.coerceAtLeast(0.01f))
                    .fillMaxWidth()
                    .height(height)
                    .background(subjectColor(subject.colorArgb))
            )
        }
    }
}

/**
 * Ranked subject rows: dot, name, duration and share.
 *
 * Durations are shown in Trends and History; the compact home legend can omit
 * them to keep the card quiet.
 */
@Composable
fun SubjectBreakdownList(
    subjects: List<SubjectFocus>,
    modifier: Modifier = Modifier,
    showDurations: Boolean = true,
    maxRows: Int = 5
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(FrostTheme.spacing.sm)
    ) {
        subjects.take(maxRows).forEach { subject ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = if (showDurations) {
                            "${subject.tag}, ${formatDuration(subject.seconds)}, " +
                                "${percent(subject.share)} percent"
                        } else {
                            "${subject.tag}, ${percent(subject.share)} percent"
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = subjectColor(subject.colorArgb))
                Spacer(modifier = Modifier.width(FrostTheme.spacing.sm))
                Text(
                    text = subject.tag,
                    style = FrostTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (showDurations) {
                    Text(
                        text = formatDuration(subject.seconds),
                        style = FrostTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textSecondary,
                        modifier = Modifier.padding(horizontal = FrostTheme.spacing.sm)
                    )
                }
                Text(
                    text = "${percent(subject.share)}%",
                    style = FrostTheme.typography.labelLarge,
                    color = FrostTheme.colors.textSecondary
                )
            }
        }
    }
}

internal fun percent(share: Float): Int = (share * 100f).roundToInt()
