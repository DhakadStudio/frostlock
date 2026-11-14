package com.frostlock.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IcePaletteTest {

    @Test
    fun `unknown keys fall back to frost`() {
        assertEquals(IcePalettes.Frost, IcePalettes.fromKey(null))
        assertEquals(IcePalettes.Frost, IcePalettes.fromKey(""))
        assertEquals(IcePalettes.Frost, IcePalettes.fromKey("neon"))
    }

    @Test
    fun `every key resolves to its palette`() {
        IcePalettes.all.forEach { palette ->
            assertEquals(palette, IcePalettes.fromKey(palette.key))
        }
    }

    @Test
    fun `keys and names are unique`() {
        assertEquals(IcePalettes.all.size, IcePalettes.all.map { it.key }.toSet().size)
        assertEquals(IcePalettes.all.size, IcePalettes.all.map { it.name }.toSet().size)
        assertTrue(IcePalettes.all.all { it.key.isNotBlank() && it.name.isNotBlank() })
    }

    @Test
    fun `palettes are visually distinct`() {
        val backgrounds = IcePalettes.all.map { it.background }.toSet()
        val primaries = IcePalettes.all.map { it.primary }.toSet()
        assertEquals(IcePalettes.all.size, backgrounds.size)
        assertEquals(IcePalettes.all.size, primaries.size)
        IcePalettes.all.forEach { palette ->
            assertNotEquals(palette.background, palette.surfaceCard)
            assertEquals(2, palette.gradient.size)
        }
    }
}
