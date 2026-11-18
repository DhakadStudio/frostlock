package com.frostlock.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * A complete ice palette. FrostLockTheme turns a palette into the semantic
 * roles in [FrostColors]; screens read those via FrostTheme.colors, so
 * switching the palette restyles every screen.
 */
data class IcePalette(
    val key: String,
    val name: String,
    val background: Color,
    val backgroundElevated: Color,
    val surfaceCard: Color,
    val surfaceHigh: Color,
    val strokeSoft: Color,
    val primary: Color,
    val glacier: Color,
    val accent: Color,
    val glow: Color,
    val ink: Color,
    val textPrimary: Color,
    val textSecondary: Color
) {
    val gradient: List<Color>
        get() = listOf(primary, glacier)

    val heroGradient: List<Color>
        get() = listOf(glacier.copy(alpha = 0.55f), backgroundElevated)
}

object IcePalettes {
    val Frost = IcePalette(
        key = "frost",
        name = "Frost",
        background = Color(0xFF050B12),
        backgroundElevated = Color(0xFF0A1520),
        surfaceCard = Color(0xFF0E1B28),
        surfaceHigh = Color(0xFF152838),
        strokeSoft = Color(0xFF20394F),
        primary = Color(0xFF38BDF8),
        glacier = Color(0xFF0EA5E9),
        accent = Color(0xFF7DD3FC),
        glow = Color(0xFFBAE6FD),
        ink = Color(0xFF04202E),
        textPrimary = Color(0xFFEAF7FF),
        textSecondary = Color(0xFF93AEC4)
    )

    val Aurora = IcePalette(
        key = "aurora",
        name = "Aurora",
        background = Color(0xFF04100C),
        backgroundElevated = Color(0xFF081A15),
        surfaceCard = Color(0xFF0B2019),
        surfaceHigh = Color(0xFF123128),
        strokeSoft = Color(0xFF1E4A3D),
        primary = Color(0xFF34D399),
        glacier = Color(0xFF10B981),
        accent = Color(0xFF6EE7B7),
        glow = Color(0xFFA7F3D0),
        ink = Color(0xFF04231A),
        textPrimary = Color(0xFFE9FBF4),
        textSecondary = Color(0xFF8FB8A8)
    )

    val Midnight = IcePalette(
        key = "midnight",
        name = "Midnight",
        background = Color(0xFF080B1A),
        backgroundElevated = Color(0xFF0D1124),
        surfaceCard = Color(0xFF11162B),
        surfaceHigh = Color(0xFF1A2040),
        strokeSoft = Color(0xFF2A3157),
        primary = Color(0xFF818CF8),
        glacier = Color(0xFF6366F1),
        accent = Color(0xFFA5B4FC),
        glow = Color(0xFFC7D2FE),
        ink = Color(0xFF0B0F2A),
        textPrimary = Color(0xFFF0F2FF),
        textSecondary = Color(0xFFA0A8C8)
    )

    val Crystal = IcePalette(
        key = "crystal",
        name = "Crystal",
        background = Color(0xFF120818),
        backgroundElevated = Color(0xFF190C21),
        surfaceCard = Color(0xFF1E0F28),
        surfaceHigh = Color(0xFF2A1536),
        strokeSoft = Color(0xFF43224F),
        primary = Color(0xFFF472B6),
        glacier = Color(0xFFEC4899),
        accent = Color(0xFFF9A8D4),
        glow = Color(0xFFFBCFE8),
        ink = Color(0xFF2A0A1C),
        textPrimary = Color(0xFFFFF0F8),
        textSecondary = Color(0xFFC0A0B4)
    )

    val all: List<IcePalette> = listOf(Frost, Aurora, Midnight, Crystal)

    /** Unknown or missing keys fall back to the default Frost palette. */
    fun fromKey(key: String?): IcePalette =
        all.firstOrNull { it.key == key } ?: Frost
}

/**
 * The palette chosen by the user, used as the default for [FrostLockTheme].
 */
object FrostThemeState {
    var palette: IcePalette by mutableStateOf(IcePalettes.Frost)
}
