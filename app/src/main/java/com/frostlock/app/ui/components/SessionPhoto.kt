package com.frostlock.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.frostlock.app.data.SessionPhotoStore
import com.frostlock.app.ui.util.SessionPhotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Photo slot shared by the post-session summary and the diary editor: a local
 * thumbnail plus choose/change/remove actions. Uses the system photo picker, so
 * no camera or storage permission is required.
 */
@Composable
fun SessionPhotoEditor(
    name: String,
    onPick: (Uri) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var viewingPhoto by remember { mutableStateOf(false) }

    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onPick(uri)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(FrostTheme.colors.surfaceContainerHigh.copy(alpha = 0.45f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (name.isNotBlank()) {
                SessionPhotoThumbnail(
                    name = name,
                    modifier = Modifier.size(58.dp),
                    corner = 12.dp,
                    onClick = { viewingPhoto = true }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Session photo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textPrimary
                    )
                    Text(
                        text = "Stays on this device",
                        style = MaterialTheme.typography.bodySmall,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(FrostTheme.colors.surfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = FrostTheme.colors.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add a photo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FrostTheme.colors.textPrimary
                    )
                    Text(
                        text = "A quick visual note from this session",
                        style = MaterialTheme.typography.bodySmall,
                        color = FrostTheme.colors.textSecondary
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    pickPhoto.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
            ) {
                Text(
                    text = if (name.isBlank()) "Choose photo" else "Change photo",
                    color = FrostTheme.colors.secondary
                )
            }
            if (name.isNotBlank()) {
                TextButton(onClick = onRemove) {
                    Text(text = "Remove", color = FrostTheme.colors.textSecondary)
                }
            }
        }
    }

    if (viewingPhoto) {
        SessionPhotoViewer(name = name, onDismiss = { viewingPhoto = false })
    }
}

/**
 * A locally stored session photo, decoded off the main thread at the size the
 * slot needs. Missing files fall back to a quiet placeholder instead of
 * crashing, so a cleared cache never breaks the diary.
 */
@Composable
fun SessionPhotoThumbnail(
    name: String,
    modifier: Modifier = Modifier,
    corner: Dp = 12.dp,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val file = remember(name) { SessionPhotoStore.file(context, name) }
    val bitmap by produceState<ImageBitmap?>(null, file?.path, file?.lastModified()) {
        value = withContext(Dispatchers.IO) {
            decodeSampled(file, maxEdge = 480)?.asImageBitmap()
        }
    }

    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .clip(shape)
            .background(FrostTheme.colors.surfaceContainerHigh)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "Session photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = null,
                tint = FrostTheme.colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Full-screen, themed viewer for one local session photo. */
@Composable
fun SessionPhotoViewer(name: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val file = remember(name) { SessionPhotoStore.file(context, name) }
    val bitmap by produceState<ImageBitmap?>(null, file?.path, file?.lastModified()) {
        value = withContext(Dispatchers.IO) {
            decodeSampled(file, maxEdge = 2048)?.asImageBitmap()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF2071019))
                .clickable(onClick = onDismiss)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val image = bitmap
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = "Session photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize(0.9f)
                            .clip(RoundedCornerShape(20.dp))
                    )
                } else {
                    Text(
                        text = "This photo is no longer available.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = FrostTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Stored only on this device",
                    style = MaterialTheme.typography.bodySmall,
                    color = FrostTheme.colors.secondary
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close photo",
                    tint = Color.White
                )
            }
        }
    }
}

private fun decodeSampled(file: File?, maxEdge: Int) = runCatching {
    file ?: return@runCatching null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
    val options = BitmapFactory.Options().apply {
        inSampleSize = SessionPhotos.sampleSizeFor(bounds.outWidth, bounds.outHeight, maxEdge)
    }
    BitmapFactory.decodeFile(file.path, options)
}.getOrNull()
