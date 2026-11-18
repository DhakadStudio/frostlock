package com.frostlock.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.frostlock.app.ui.theme.FrostLockTheme
import com.frostlock.app.ui.theme.FrostTheme

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = FrostTheme.colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050B12)
@Composable
fun SectionHeaderPreview() {
    FrostLockTheme {
        SectionHeader(title = "Recent Sessions") {
            Text(text = "See All", color = FrostTheme.colors.secondary)
        }
    }
}
