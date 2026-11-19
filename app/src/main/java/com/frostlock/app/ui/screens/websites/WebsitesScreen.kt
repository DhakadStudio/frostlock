package com.frostlock.app.ui.screens.websites

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.frostlock.app.ui.components.AppCard
import com.frostlock.app.ui.components.MoodChip
import com.frostlock.app.ui.components.PrimaryButton
import com.frostlock.app.ui.components.SectionHeader
import com.frostlock.app.ui.util.WebsiteCategory
import com.frostlock.app.ui.components.surface.frostPageBackground
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Opt-in, fully local website blocking. FrostLock reads the browser address bar
 * through the accessibility service already used for app blocking — there is no
 * VPN, no network permission and no browsing history stored.
 */
@Composable
fun WebsitesScreen(
    onBack: () -> Unit,
    viewModel: WebsitesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var newDomain by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var newAllowedDomain by remember { mutableStateOf("") }
    var allowedError by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAccessibilityStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                    text = "Website blocking",
                    style = MaterialTheme.typography.headlineMedium,
                    color = FrostTheme.colors.textPrimary
                )
                Text(
                    text = "Held only while focus is active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FrostTheme.colors.textSecondary
                )
            }
        }

        WebsiteToggleCard(
            enabled = state.enabled,
            allowOnly = state.allowOnly,
            siteCount = state.domains.size,
            studyCount = state.allowedDomains.size,
            onToggle = viewModel::setEnabled
        )

        if (!state.accessibilityEnabled) {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = FrostTheme.colors.warning
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Accessibility is off",
                            style = MaterialTheme.typography.titleMedium,
                            color = FrostTheme.colors.textPrimary
                        )
                    }
                    Text(
                        text = "Website blocking reads the address bar through the " +
                            "accessibility service. Without it, sites cannot be detected. " +
                            "FrostLock never stores pages or typing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textSecondary
                    )
                    PrimaryButton(
                        text = "Open accessibility settings",
                        onClick = viewModel::openAccessibilitySettings
                    )
                }
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
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = FrostTheme.colors.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sites are checked only during a focus session or an " +
                        "active schedule. No VPN, no internet permission, and " +
                        "nothing leaves this device.",
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
                            "Study mode: only the sites on the allowed list can " +
                                "open during focus. Everyone else is held."
                        else ->
                            "Block list: the sites below are held during focus; " +
                                "everything else opens."
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

        if (!state.allowOnly) {
            state.categories.forEach { category ->
                SectionHeader(title = category.label)
                CategoryCard(
                    category = category,
                    domains = state.domains,
                    onToggle = viewModel::setDomainBlocked
                )
            }

            SectionHeader(title = "Your sites")
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newDomain,
                            onValueChange = {
                                newDomain = it
                                error = null
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = {
                                Text(text = "example.com", color = FrostTheme.colors.textSecondary)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
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
                                .clickable {
                                    val added = newDomain.isNotBlank() &&
                                        viewModel.addDomain(newDomain)
                                    if (added) {
                                        newDomain = ""
                                        error = null
                                    } else {
                                        error = "Enter a domain like example.com"
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add site",
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

                    if (state.customDomains.isEmpty()) {
                        Text(
                            text = "No custom sites yet. Curated lists above are always " +
                                "editable, and any domain you add stays on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = FrostTheme.colors.textSecondary
                        )
                    } else {
                        state.customDomains.forEach { domain ->
                            CustomDomainRow(
                                domain = domain,
                                onRemove = { viewModel.removeDomain(domain) }
                            )
                        }
                    }
                }
            }
        }

        SectionHeader(title = "Allowed sites")
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = when {
                        state.allowOnly ->
                            "Only these sites can open during focus. Everyone else is held."
                        else ->
                            "Sites here are never blocked, even when they match the " +
                                "block list. Useful for study resources and reference pages."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = FrostTheme.colors.textSecondary
                )
                if (state.allowOnly && state.allowedDomains.isEmpty()) {
                    Text(
                        text = "The study list is empty, so nothing is held yet. " +
                            "Add the sites you want to keep.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FrostTheme.colors.warning
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newAllowedDomain,
                        onValueChange = {
                            newAllowedDomain = it
                            allowedError = null
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(text = "wikipedia.org", color = FrostTheme.colors.textSecondary)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
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
                            .clickable {
                                val added = newAllowedDomain.isNotBlank() &&
                                    viewModel.addAllowedDomain(newAllowedDomain)
                                if (added) {
                                    newAllowedDomain = ""
                                    allowedError = null
                                } else {
                                    allowedError = "Enter a domain like wikipedia.org"
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Allow site",
                            tint = FrostTheme.colors.onPrimary
                        )
                    }
                }

                allowedError?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = FrostTheme.colors.danger
                    )
                }

                if (state.allowedDomains.isEmpty()) {
                    Text(
                        text = "No allowed sites yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FrostTheme.colors.textSecondary
                    )
                } else {
                    state.allowedDomains.sorted().forEach { domain ->
                        AllowedDomainRow(
                            domain = domain,
                            onRemove = { viewModel.removeAllowedDomain(domain) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun WebsiteToggleCard(
    enabled: Boolean,
    allowOnly: Boolean,
    siteCount: Int,
    studyCount: Int,
    onToggle: (Boolean) -> Unit
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled) {
                            Brush.linearGradient(FrostTheme.colors.actionGradient)
                        } else {
                            androidx.compose.ui.graphics.SolidColor(FrostTheme.colors.surfaceContainerHigh)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = if (enabled) FrostTheme.colors.onPrimary else FrostTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Block distracting sites",
                    style = MaterialTheme.typography.titleMedium,
                    color = FrostTheme.colors.textPrimary
                )
                Text(
                    text = when {
                        enabled && allowOnly -> "On — study list of $studyCount site(s)"
                        enabled -> "On — $siteCount site(s) on the local list"
                        else -> "Off — websites are never checked"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) FrostTheme.colors.success else FrostTheme.colors.textSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = FrostTheme.colors.primary,
                    uncheckedThumbColor = FrostTheme.colors.textSecondary,
                    uncheckedTrackColor = FrostTheme.colors.surfaceContainerHigh
                )
            )
        }
    }
}

@Composable
private fun CategoryCard(
    category: WebsiteCategory,
    domains: Set<String>,
    onToggle: (String, Boolean) -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            category.domains.forEachIndexed { index, domain ->
                DomainRow(
                    domain = domain,
                    checked = domain in domains,
                    onToggle = { checked -> onToggle(domain, checked) }
                )
                if (index != category.domains.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(FrostTheme.colors.outline)
                    )
                }
            }
        }
    }
}

@Composable
private fun DomainRow(
    domain: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = domain,
            style = MaterialTheme.typography.bodyLarge,
            color = if (checked) FrostTheme.colors.textPrimary else FrostTheme.colors.textSecondary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = FrostTheme.colors.primary,
                uncheckedThumbColor = FrostTheme.colors.textSecondary,
                uncheckedTrackColor = FrostTheme.colors.surfaceContainerHigh
            )
        )
    }
}

@Composable
private fun CustomDomainRow(domain: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FrostTheme.colors.surfaceContainerHigh)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            tint = FrostTheme.colors.secondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = domain,
            style = MaterialTheme.typography.bodyLarge,
            color = FrostTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove $domain",
                tint = FrostTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun AllowedDomainRow(domain: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FrostTheme.colors.surfaceContainerHigh)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = FrostTheme.colors.success,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = domain,
            style = MaterialTheme.typography.bodyLarge,
            color = FrostTheme.colors.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove $domain",
                tint = FrostTheme.colors.textSecondary
            )
        }
    }
}
