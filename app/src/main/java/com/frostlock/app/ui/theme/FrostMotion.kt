package com.frostlock.app.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Motion tokens.
 *
 * Geometry uses spatial springs; colour and opacity use critically damped
 * effects springs. Routine navigation uses the standard scheme, while the Focus
 * timer, Shield status and Insights hero may use the expressive one.
 *
 * The helpers are generic so the same token works for `Dp`, `Float`, `Offset`
 * and `Color` animations.
 */
object FrostMotion {
    fun <T> fastSpatial(): SpringSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)
    fun <T> defaultSpatial(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)
    fun <T> slowSpatial(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 200f)

    fun <T> fastEffects(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 3800f)
    fun <T> defaultEffects(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)
    fun <T> slowEffects(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 800f)

    /** Snappier spring for taps on small controls. */
    fun <T> control(): SpringSpec<T> = fastSpatial()
}
