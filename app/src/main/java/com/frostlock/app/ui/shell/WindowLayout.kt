package com.frostlock.app.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

/**
 * Width-based layout buckets used by the adaptive shell.
 *
 * Mirrors the Material window size classes without pulling in the experimental
 * `material3-window-size-class` API: Compact < 600dp, Medium < 840dp, Expanded ≥ 840dp.
 */
enum class FrostWindowSizeClass {
    /** Phones in portrait. */
    COMPACT,

    /** Large phones in landscape and small tablets. */
    MEDIUM,

    /** Tablets and unfolded foldables. */
    EXPANDED
}

/**
 * Pure mapping from a window width in dp to a size class.
 *
 * Kept side-effect free so the boundaries are unit-tested.
 */
fun windowWidthClassFor(widthDp: Int): FrostWindowSizeClass = when {
    widthDp < 600 -> FrostWindowSizeClass.COMPACT
    widthDp < 840 -> FrostWindowSizeClass.MEDIUM
    else -> FrostWindowSizeClass.EXPANDED
}

/**
 * The current width class derived from the configuration, so rotation and
 * window resizing keep it honest.
 */
@Composable
fun rememberFrostWindowSizeClass(): FrostWindowSizeClass {
    val configuration = LocalConfiguration.current
    return windowWidthClassFor(configuration.screenWidthDp)
}

/**
 * True while the device is a foldable in tabletop posture — half-open with a
 * horizontal hinge. Content can then sit above the fold (display) with
 * controls below it.
 */
@Composable
fun rememberIsTabletopPosture(): Boolean {
    val context = LocalContext.current
    val posture = produceState(initialValue = false) {
        try {
            WindowInfoTracker.getOrCreate(context)
                .windowLayoutInfo(context)
                .collect { info ->
                    value = info.displayFeatures.any { feature ->
                        feature is FoldingFeature &&
                            feature.state == FoldingFeature.State.HALF_OPENED &&
                            feature.orientation == FoldingFeature.Orientation.HORIZONTAL
                    }
                }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Throwable) {
            // Non-Activity contexts (previews, tests) have no window features.
            value = false
        }
    }
    return posture.value
}
