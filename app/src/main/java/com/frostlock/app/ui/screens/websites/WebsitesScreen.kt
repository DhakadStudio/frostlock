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
import com.frostlock.app.ui.components.PrimaryButton
import com.frostlock.app.ui.components.SectionHeader
import com.frostlock.app.ui.theme.AccentFrost
import com.frostlock.app.ui.theme.BackgroundDark
import com.frostlock.app.ui.theme.DangerRed
import com.frostlock.app.ui.theme.GradientFrost
import com.frostlock.app.ui.theme.InkOnIce
import com.frostlock.app.ui.theme.PrimaryIce
import com.frostlock.app.ui.theme.StrokeSoft
import com.frostlock.app.ui.theme.SuccessGreen
import com.frostlock.app.ui.theme.SurfaceHigh
import com.frostlock.app.ui.theme.TextPrimary
import com.frostlock.app.ui.theme.TextSecondary
import com.frostlock.app.ui.theme.WarnAmber
import com.frostlock.app.ui.util.WebsiteCategory

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
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Website blocking",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Held only while focus is active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        WebsiteToggleCard(
            enabled = state.enabled,
            siteCount = state.domains.size,
            onToggle = viewModel::setEnabled
        )

        if (!state.accessibilityEnabled) {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = WarnAmber
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Accessibility is off",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Website blocking reads the address bar through the " +
                            "accessibility service. Without it, sites cannot be detected. " +
                            "FrostLock never stores pages or typing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
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
                        .background(SurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = AccentFrost,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sites are checked only during a focus session or an " +
                        "active schedule. No VPN, no internet permission, and " +
                        "nothing leaves this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

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
                            Text(text = "example.com", color = TextSecondary)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIce,
                            unfocusedBorderColor = StrokeSoft,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentFrost
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(GradientFrost))
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
                            tint = InkOnIce
                        )
                    }
                }

                error?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = DangerRed
                    )
                }

                if (state.customDomains.isEmpty()) {
                    Text(
                        text = "No custom sites yet. Curated lists above are always " +
                            "editable, and any domain you add stays on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
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

        SectionHeader(title = "Allowed sites")
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Sites here are never blocked, even when they match the " +
                        "block list. Useful for study resources and reference pages.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
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
                            Text(text = "wikipedia.org", color = TextSecondary)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIce,
                            unfocusedBorderColor = StrokeSoft,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentFrost
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(GradientFrost))
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
                            tint = InkOnIce
                        )
                    }
                }

                allowedError?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = DangerRed
                    )
                }

                if (state.allowedDomains.isEmpty()) {
                    Text(
                        text = "No allowed sites yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
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
    siteCount: Int,
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
                            Brush.linearGradient(GradientFrost)
                        } else {
                            androidx.compose.ui.graphics.SolidColor(SurfaceHigh)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = if (enabled) InkOnIce else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Block distracting sites",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = when {
                        enabled -> "On — $siteCount site(s) on the local list"
                        else -> "Off — websites are never checked"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) SuccessGreen else TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryIce,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = SurfaceHigh
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
                            .background(StrokeSoft)
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
            color = if (checked) TextPrimary else TextSecondary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryIce,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = SurfaceHigh
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
            .background(SurfaceHigh)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            tint = AccentFrost,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = domain,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove $domain",
                tint = TextSecondary
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
            .background(SurfaceHigh)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = domain,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove $domain",
                tint = TextSecondary
            )
        }
    }
}
