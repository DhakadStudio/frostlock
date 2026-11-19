package com.frostlock.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.components.surface.FrostCard
import com.frostlock.app.ui.components.surface.FrostCardVariant
import com.frostlock.app.ui.theme.FrostLockTheme
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Legacy content card used by a few older screens.
 *
 * Delegates to [FrostCard] so those screens automatically share the app's
 * frosted-glass surface instead of keeping a second card style alive.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    FrostCard(
        modifier = modifier,
        variant = FrostCardVariant.OUTLINED,
        contentPadding = PaddingValues(16.dp)
    ) {
        content()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF050B12)
@Composable
private fun AppCardPreview() {
    FrostLockTheme {
        AppCard { androidx.compose.material3.Text(text = "Card", color = FrostTheme.colors.textPrimary) }
    }
}
