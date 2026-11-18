package com.frostlock.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.theme.FrostLockTheme
import com.frostlock.app.ui.theme.FrostTheme

@Composable
fun MoodChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) FrostTheme.colors.primary else FrostTheme.colors.surfaceContainer
    val borderColor = if (selected) FrostTheme.colors.secondary else FrostTheme.colors.surfaceContainer

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(backgroundColor)
            .border(1.dp, borderColor, MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else FrostTheme.colors.textPrimary
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050B12)
@Composable
fun MoodChipPreview() {
    FrostLockTheme {
        MoodChip(text = "Focused 🧘", selected = true, onClick = {})
    }
}
