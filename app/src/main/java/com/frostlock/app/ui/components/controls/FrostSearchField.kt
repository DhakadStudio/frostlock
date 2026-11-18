package com.frostlock.app.ui.components.controls

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.frostlock.app.ui.theme.FrostShapes
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Search field used by app pickers and long lists.
 *
 * Shows a clear action only when there is text, so the field stays quiet at rest.
 */
@Composable
fun FrostSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    onClear: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = {
            Text(
                text = placeholder,
                style = FrostTheme.typography.bodyLarge,
                color = FrostTheme.colors.textSecondary
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = FrostTheme.colors.textSecondary
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onClear?.invoke() ?: onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = FrostTheme.colors.textSecondary
                    )
                }
            }
        },
        textStyle = FrostTheme.typography.bodyLarge,
        shape = FrostShapes.pill,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Search
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = FrostTheme.colors.textPrimary,
            unfocusedTextColor = FrostTheme.colors.textPrimary,
            focusedContainerColor = FrostTheme.colors.surfaceContainerLow,
            unfocusedContainerColor = FrostTheme.colors.surfaceContainerLow,
            focusedBorderColor = FrostTheme.colors.primary,
            unfocusedBorderColor = FrostTheme.colors.outline,
            cursorColor = FrostTheme.colors.primary
        )
    )
}
