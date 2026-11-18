package com.frostlock.app.ui.screens.shield

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frostlock.app.data.InstalledApp
import com.frostlock.app.ui.theme.FrostTheme

/** App icon with a letter fallback, shared by the Shield app and limit lists. */
@Composable
internal fun ShieldAppIcon(
    app: InstalledApp,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    val shape = RoundedCornerShape(12.dp)
    if (app.icon != null) {
        Image(
            bitmap = app.icon,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(shape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(FrostTheme.colors.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.label.take(1).uppercase(),
                style = FrostTheme.typography.titleLarge,
                color = FrostTheme.colors.secondary
            )
        }
    }
}
