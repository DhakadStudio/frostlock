package com.frostlock.app.ui.components.controls

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.frostlock.app.ui.theme.FrostShapes
import com.frostlock.app.ui.theme.FrostTheme

/**
 * Strict Mode's only exit: a three-second press that cannot happen by accident.
 *
 * The fill grows with the press so the user can see the commitment before the
 * session ends.
 */
@Composable
fun HoldToEndButton(
    onEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var holding by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(holding) {
        if (holding) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 3_000, easing = LinearEasing))
            if (progress.value >= 1f) onEnd()
        } else {
            progress.animateTo(0f, tween(durationMillis = 200))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(FrostTheme.colors.surfaceContainerHigh, FrostShapes.pill)
            .border(1.dp, FrostTheme.colors.outline, FrostShapes.pill)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        holding = true
                        tryAwaitRelease()
                        holding = false
                    }
                )
            }
            .padding(vertical = FrostTheme.spacing.lg),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .align(Alignment.CenterStart)
                .background(
                    Brush.horizontalGradient(FrostTheme.colors.actionGradient)
                )
        )
        Text(
            text = if (holding) "Keep holding to end…" else "Hold to end session",
            style = FrostTheme.typography.labelLarge,
            color = FrostTheme.colors.onPrimary
        )
    }
}
