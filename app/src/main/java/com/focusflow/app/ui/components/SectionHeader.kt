package com.focusflow.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.focusflow.app.ui.theme.AccentPurple
import com.focusflow.app.ui.theme.FocusFlowTheme
import com.focusflow.app.ui.theme.TextPrimary

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
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        action?.invoke()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0A1F)
@Composable
fun SectionHeaderPreview() {
    FocusFlowTheme {
        SectionHeader(title = "Recent Sessions") {
            Text(text = "See All", color = AccentPurple)
        }
    }
}
