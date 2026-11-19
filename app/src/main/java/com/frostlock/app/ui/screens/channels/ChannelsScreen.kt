package com.frostlock.app.ui.screens.channels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.MoodChip
import com.frostlock.app.ui.components.SectionHeader
import com.frostlock.app.ui.components.surface.frostPageBackground
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Opt-in, fully local YouTube / YouTube Music channel filtering. The
 * accessibility service reads the watch page's owner row (Subscribe button and
 * channel avatar), so a block list or a study allowlist can hold individual
 * channels while focus or a rule is active. Both apps share the same lists.
 * Nothing leaves this device.
 */
@Composable
fun ChannelsScreen(
    onBack: () -> Unit,
    viewModel: ChannelsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var newBlocked by remember { mutableStateOf("") }
    var blockedError by remember { mutableStateOf<String?>(null) }
    var newAllowed by remember { mutableStateOf("") }
    var allowedError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .frostPageBackground()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = FrostTheme.colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "YouTube & Music",
                    style = MaterialTheme.typography.headlineMedium,
                    color = FrostTheme.colors.textPrimary
                )
                Text(
                    text = "Channel filtering, held only while focus is active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
            }
        }

        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.enabled) {
                                Brush.linearGradient(FrostTheme.colors.actionGradient)
                            } else {
                                androidx.compose.ui.graphics.SolidColor(FrostTheme.colors.surfaceContainerHigh)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = if (state.enabled) FrostTheme.colors.onPrimary else FrostTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Channel filtering",
                        style = MaterialTheme.typography.titleMedium,
                        color = FrostTheme.colors.textPrimary
                    )
                    Text(
                        text = when {
                            state.enabled && state.allowOnly ->
                                "On — study list of ${state.allowed.size} channel(s)"
                            state.enabled ->
                                "On — ${state.blocked.size} blocked channel(s)"
                            else -> "Off — every channel plays"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.enabled) FrostTheme.colors.success else FrostTheme.colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = state.enabled,
                    onCheckedChange = viewModel::setEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = FrostTheme.colors.primary,
                        uncheckedThumbColor = FrostTheme.colors.textSecondary,
                        uncheckedTrackColor = FrostTheme.colors.surfaceContainerHigh
                    )
                )
            }
        }

        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(FrostTheme.colors.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = FrostTheme.colors.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "The channel is read from the owner row on YouTube " +
                        "and YouTube Music, so search results, recommendations " +
                        "and the home feed are never judged by their channel. " +
                        "Only checked during a focus session or an active schedule.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary
                )
            }
        }

        SectionHeader(title = "Mode")
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = when {
                        state.allowOnly ->
                            "Study mode: only the channels on the study list can play during focus."
                        else ->
                            "Block list: the channels below are held during focus; everything else plays."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MoodChip(
                        text = "Block list",
                        selected = !state.allowOnly,
                        onClick = { viewModel.setAllowOnly(false) }
                    )
                    MoodChip(
                        text = "Study list",
                        selected = state.allowOnly,
                        onClick = { viewModel.setAllowOnly(true) }
                    )
                }
            }
        }

        if (state.enabled && state.allowOnly && state.allowed.isEmpty()) {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(FrostTheme.colors.warning.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = FrostTheme.colors.warning,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "The study list is empty, so every channel is held while " +
                            "filtering is on. Add a channel, or switch to the block list.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FrostTheme.colors.warning
                    )
                }
            }
        }

        if (state.allowOnly) {
            StudyListCard(
                channels = state.allowed,
                newValue = newAllowed,
                error = allowedError,
                onValueChange = { value ->
                    newAllowed = value
                    allowedError = null
                },
                onAdd = {
                    val added = newAllowed.isNotBlank() && viewModel.addAllowed(newAllowed)
                    if (added) {
                        newAllowed = ""
                        allowedError = null
                    } else {
                        allowedError = "Type a channel name or @handle"
                    }
                },
                onRemove = viewModel::removeAllowed
            )
        } else {
            StudyListCard(
                channels = state.blocked,
                newValue = newBlocked,
                error = blockedError,
                onValueChange = { value ->
                    newBlocked = value
                    blockedError = null
                },
                onAdd = {
                    val added = newBlocked.isNotBlank() && viewModel.addBlocked(newBlocked)
                    if (added) {
                        newBlocked = ""
                        blockedError = null
                    } else {
                        blockedError = "Type a channel name or @handle"
                    }
                },
                onRemove = viewModel::removeBlocked,
                blockedMode = true
            )
        }

        Spacer(modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun StudyListCard(
    channels: List<String>,
    newValue: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    blockedMode: Boolean = false
) {
    SectionHeader(
        title = if (blockedMode) "Blocked channels" else "Study channels"
    )
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = if (blockedMode) {
                    "Videos from these channels are held during focus."
                } else {
                    "Only these channels can play during focus. Everyone else is held."
                },
                style = MaterialTheme.typography.bodySmall,
                color = FrostTheme.colors.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newValue,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = if (blockedMode) "@distractingchannel" else "@studychannel",
                            color = FrostTheme.colors.textSecondary
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FrostTheme.colors.primary,
                        unfocusedBorderColor = FrostTheme.colors.outline,
                        focusedTextColor = FrostTheme.colors.textPrimary,
                        unfocusedTextColor = FrostTheme.colors.textPrimary,
                        cursorColor = FrostTheme.colors.secondary
                    ),
                    shape = MaterialTheme.shapes.medium
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(FrostTheme.colors.actionGradient))
                        .clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add channel",
                        tint = FrostTheme.colors.onPrimary
                    )
                }
            }

            error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = FrostTheme.colors.danger
                )
            }

            if (channels.isEmpty()) {
                Text(
                    text = if (blockedMode) {
                        "No blocked channels yet."
                    } else {
                        "No study channels yet."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary
                )
            } else {
                channels.forEach { channel ->
                    ChannelRow(
                        channel = channel,
                        blockedMode = blockedMode,
                        onRemove = { onRemove(channel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(
    channel: String,
    blockedMode: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FrostTheme.colors.surfaceContainerHigh)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = if (blockedMode) FrostTheme.colors.secondary else FrostTheme.colors.success,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = channel,
            style = MaterialTheme.typography.bodyLarge,
            color = FrostTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove $channel",
                tint = FrostTheme.colors.textSecondary
            )
        }
    }
}
