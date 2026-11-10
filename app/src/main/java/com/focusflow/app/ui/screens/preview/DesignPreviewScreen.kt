package com.focusflow.app.ui.screens.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.focusflow.app.ui.components.AppCard
import com.focusflow.app.ui.components.GlowCard
import com.focusflow.app.ui.components.MoodChip
import com.focusflow.app.ui.components.PanicButton
import com.focusflow.app.ui.components.PrimaryButton
import com.focusflow.app.ui.components.SecondaryButton
import com.focusflow.app.ui.components.SectionHeader
import com.focusflow.app.ui.components.StatTile
import com.focusflow.app.ui.components.StreakRing
import com.focusflow.app.ui.theme.BackgroundDark
import com.focusflow.app.ui.theme.FocusFlowTheme
import com.focusflow.app.ui.theme.TextPrimary

@Composable
fun DesignPreviewScreen(
    onBack: () -> Unit = {}
) {
    var moodSelected by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Design System Preview",
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary
        )

        SectionHeader(title = "Buttons")
        PrimaryButton(text = "Primary Button", onClick = {})
        SecondaryButton(text = "Secondary Button", onClick = {})

        SectionHeader(title = "Cards")
        AppCard {
            Text(text = "Standard AppCard content", color = TextPrimary)
        }
        GlowCard {
            Text(text = "GlowCard with soft purple shadow", color = TextPrimary)
        }

        SectionHeader(title = "Streak Ring")
        StreakRing(streakCount = 12, progress = 0.85f)

        SectionHeader(title = "Mood Chip")
        MoodChip(text = "Calm 🌿", selected = moodSelected, onClick = { moodSelected = !moodSelected })

        SectionHeader(title = "Stat Tile")
        StatTile(title = "Focus Streak", value = "5 Days", icon = Icons.Default.Star)

        SectionHeader(title = "Panic Button")
        PanicButton(onClick = {})

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0A1F)
@Composable
fun DesignPreviewScreenPreview() {
    FocusFlowTheme {
        DesignPreviewScreen()
    }
}
