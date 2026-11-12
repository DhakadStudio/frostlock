package com.frostlock.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.theme.AccentFrost
import com.frostlock.app.ui.theme.FrostLockTheme
import com.frostlock.app.ui.theme.GlacierBlue
import com.frostlock.app.ui.theme.InkOnIce
import com.frostlock.app.ui.theme.PrimaryIce
import com.frostlock.app.ui.theme.StrokeSoft
import com.frostlock.app.ui.theme.SurfaceHigh
import com.frostlock.app.ui.theme.TextPrimary
import com.frostlock.app.ui.theme.TextSecondary
import com.frostlock.app.ui.util.Achievement

/**
 * Hexagonal ice badge, matching the freezer motif. Unlocked badges get a frost
 * gradient and glowing rim; locked badges stay as a cool, muted outline.
 */
@Composable
fun AchievementBadge(
    achievement: Achievement,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 68.dp,
    label: String? = null,
    caption: String? = null
) {
    val unlocked = achievement.unlocked

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = hexagonPath(this.size.width, this.size.height)
                if (unlocked) {
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            colors = listOf(PrimaryIce, GlacierBlue)
                        )
                    )
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.35f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                } else {
                    drawPath(path = path, color = SurfaceHigh)
                    drawPath(
                        path = path,
                        color = StrokeSoft,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
            Text(
                text = label ?: achievement.badge,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (unlocked) InkOnIce else TextSecondary,
                textAlign = TextAlign.Center
            )
        }
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = if (unlocked) AccentFrost else TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/** Pointy-top hexagon inscribed in the given width/height. */
private fun hexagonPath(width: Float, height: Float): Path {
    val cx = width / 2f
    val cy = height / 2f
    val r = minOf(width, height) / 2f
    val points = (0 until 6).map { index ->
        val angle = Math.toRadians((60 * index - 90).toDouble())
        Offset(
            x = cx + (r * kotlin.math.cos(angle)).toFloat(),
            y = cy + (r * kotlin.math.sin(angle)).toFloat()
        )
    }
    return Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        close()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050B12)
@Composable
private fun AchievementBadgePreview() {
    FrostLockTheme {
        AchievementBadge(
            achievement = Achievement(
                id = "focus_1h",
                badge = "1H",
                title = "Focus 1h",
                description = "Focus for 1h in total",
                current = 3600,
                target = 3600,
                category = com.frostlock.app.ui.util.AchievementCategory.FOCUS
            ),
            caption = "1H FOCUS"
        )
    }
}
