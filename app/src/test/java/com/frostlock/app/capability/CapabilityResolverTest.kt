package com.frostlock.app.capability

import org.junit.Assert.assertEquals
import org.junit.Test

class CapabilityResolverTest {

    @Test
    fun `unsupported capabilities are unavailable even when reported granted`() {
        assertEquals(
            CapabilityState.UNAVAILABLE,
            resolveCapability(supported = false, granted = true)
        )
        assertEquals(
            CapabilityState.UNAVAILABLE,
            resolveCapability(supported = false, granted = false)
        )
    }

    @Test
    fun `supported and granted is granted`() {
        assertEquals(
            CapabilityState.GRANTED,
            resolveCapability(supported = true, granted = true)
        )
    }

    @Test
    fun `supported but not granted is denied`() {
        assertEquals(
            CapabilityState.DENIED,
            resolveCapability(supported = true, granted = false)
        )
    }
}
