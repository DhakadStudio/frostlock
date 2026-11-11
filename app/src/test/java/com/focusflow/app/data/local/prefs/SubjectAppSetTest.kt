package com.focusflow.app.data.local.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubjectAppSetTest {

    private val global = setOf("com.youtube", "com.instagram")

    @Test
    fun `a subject with apps overrides the global focus list`() {
        val work = FocusSubject(1L, "Work", setOf("com.slack", "com.gmail"))

        assertEquals(
            setOf("com.slack", "com.gmail"),
            effectiveSessionPackages(work, global)
        )
    }

    @Test
    fun `a subject without apps falls back to the global focus list`() {
        assertEquals(global, effectiveSessionPackages(FocusSubject(1L, "Study"), global))
        assertEquals(global, effectiveSessionPackages(null, global))
    }

    @Test
    fun `subject names compare without case or surrounding spaces`() {
        val subject = FocusSubject(1L, "Study", emptySet())

        assertEquals(subjectKey("  study "), subjectKey(subject.name))
        assertTrue(subject.hasApps.not())
        assertFalse(subject.copy(packages = setOf("a")).hasApps.not())
    }
}
