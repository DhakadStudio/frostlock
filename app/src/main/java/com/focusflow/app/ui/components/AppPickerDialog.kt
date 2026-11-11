package com.focusflow.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.focusflow.app.data.InstalledApp
import com.focusflow.app.ui.theme.AccentPurple
import com.focusflow.app.ui.theme.GradientViolet
import com.focusflow.app.ui.theme.PrimaryPurple
import com.focusflow.app.ui.theme.StrokeSoft
import com.focusflow.app.ui.theme.SurfaceCard
import com.focusflow.app.ui.theme.SurfaceHigh
import com.focusflow.app.ui.theme.TextPrimary
import com.focusflow.app.ui.theme.TextSecondary

/** Searchable multi-select app picker shared by subjects and blocking rules. */
@Composable
fun AppPickerDialog(
    apps: List<InstalledApp>,
    selected: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
    title: String = "Apps to block"
) {
    var query by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(selected) }

    val visible = remember(apps, query) {
        apps.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceCard,
        titleContentColor = TextPrimary,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "$title · ${working.size}",
                    style = MaterialTheme.typography.headlineMedium
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search apps", color = TextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = StrokeSoft,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentPurple
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                )
            }
        },
        text = {
            if (visible.isEmpty()) {
                Text(
                    text = "No apps match that search.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(visible, key = { it.packageName }) { app ->
                        val checked = app.packageName in working
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    working = if (checked) {
                                        working - app.packageName
                                    } else {
                                        working + app.packageName
                                    }
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (app.icon != null) {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = app.label.take(1).uppercase(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = AccentPurple
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (checked) {
                                            Brush.linearGradient(GradientViolet)
                                        } else {
                                            androidx.compose.ui.graphics.SolidColor(SurfaceHigh)
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (checked) Color.Transparent else StrokeSoft,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (checked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(working) }) {
                Text(text = "Done", color = AccentPurple)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextSecondary)
            }
        }
    )
}
