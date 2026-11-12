package com.frostlock.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.theme.GlowIce
import com.frostlock.app.ui.theme.FrostLockTheme
import com.frostlock.app.ui.theme.SurfaceCard
import com.frostlock.app.ui.theme.TextPrimary

@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = MaterialTheme.shapes.medium,
                spotColor = GlowIce,
                ambientColor = GlowIce
            )
            .border(
                width = 1.dp,
                color = GlowIce.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.medium
            )
            .background(
                color = SurfaceCard,
                shape = MaterialTheme.shapes.medium
            )
            .padding(16.dp)
    ) {
        content()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050B12)
@Composable
fun GlowCardPreview() {
    FrostLockTheme {
        GlowCard {
            Text(text = "Frost Glow Card", color = TextPrimary)
        }
    }
}
