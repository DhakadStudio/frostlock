package com.frostlock.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.theme.FrostLockTheme
import com.frostlock.app.ui.theme.FrostTheme

@Composable
fun PanicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .shadow(16.dp, CircleShape, spotColor = FrostTheme.colors.danger)
            .clip(CircleShape)
            .background(FrostTheme.colors.danger)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "PANIC",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050B12)
@Composable
fun PanicButtonPreview() {
    FrostLockTheme {
        PanicButton(onClick = {})
    }
}
