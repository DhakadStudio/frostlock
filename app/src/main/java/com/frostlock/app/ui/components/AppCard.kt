package com.frostlock.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.theme.FrostLockTheme
import com.frostlock.app.ui.theme.FrostTheme

/** Standard content card: soft surface, subtle border, large corners. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = FrostTheme.colors.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, FrostTheme.colors.outline)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050B12)
@Composable
private fun AppCardPreview() {
    FrostLockTheme {
        AppCard { Text(text = "Card", color = FrostTheme.colors.textPrimary) }
    }
}
