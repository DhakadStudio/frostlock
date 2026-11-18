package com.frostlock.app.ui.shell

import org.junit.Assert.assertEquals
import org.junit.Test

class WindowLayoutTest {

    @Test
    fun `phones in portrait are compact`() {
        assertEquals(FrostWindowSizeClass.COMPACT, windowWidthClassFor(320))
        assertEquals(FrostWindowSizeClass.COMPACT, windowWidthClassFor(411))
        assertEquals(FrostWindowSizeClass.COMPACT, windowWidthClassFor(599))
    }

    @Test
    fun `large phones in landscape are medium`() {
        assertEquals(FrostWindowSizeClass.MEDIUM, windowWidthClassFor(600))
        assertEquals(FrostWindowSizeClass.MEDIUM, windowWidthClassFor(720))
        assertEquals(FrostWindowSizeClass.MEDIUM, windowWidthClassFor(839))
    }

    @Test
    fun `tablets are expanded`() {
        assertEquals(FrostWindowSizeClass.EXPANDED, windowWidthClassFor(840))
        assertEquals(FrostWindowSizeClass.EXPANDED, windowWidthClassFor(1024))
        assertEquals(FrostWindowSizeClass.EXPANDED, windowWidthClassFor(1440))
    }

    @Test
    fun `boundary is exact`() {
        assertEquals(FrostWindowSizeClass.COMPACT, windowWidthClassFor(599))
        assertEquals(FrostWindowSizeClass.MEDIUM, windowWidthClassFor(600))
        assertEquals(FrostWindowSizeClass.MEDIUM, windowWidthClassFor(839))
        assertEquals(FrostWindowSizeClass.EXPANDED, windowWidthClassFor(840))
    }
}
