package com.frostlock.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebsiteBlockingTest {

    @Test
    fun `normalizes urls paths and bare hosts`() {
        assertEquals("youtube.com", normalizeDomain("https://www.YouTube.com/watch?v=abc"))
        assertEquals("example.com", normalizeDomain("example.com/path"))
        assertEquals("example.com", normalizeDomain("  WWW.Example.COM.  "))
        assertEquals("sub.example.co.uk", normalizeDomain("sub.example.co.uk"))
    }

    @Test
    fun `rejects search queries and malformed hosts`() {
        assertNull(normalizeDomain(""))
        assertNull(normalizeDomain("   "))
        assertNull(normalizeDomain("how to focus"))
        assertNull(normalizeDomain("youtube"))
        assertNull(normalizeDomain("localhost"))
        assertNull(normalizeDomain("https://"))
    }

    @Test
    fun `domain matching includes subdomains but not lookalikes`() {
        assertTrue(domainMatches("youtube.com", "youtube.com"))
        assertTrue(domainMatches("m.youtube.com", "youtube.com"))
        assertTrue(domainMatches("www.youtube.com", "youtube.com"))
        assertFalse(domainMatches("notyoutube.com", "youtube.com"))
        assertFalse(domainMatches("youtube.com.evil.example", "youtube.com"))
        assertFalse(domainMatches("", "youtube.com"))
    }

    @Test
    fun `address bar text accepts urls and hosts`() {
        assertEquals("instagram.com", hostFromUrlText("https://instagram.com/explore"))
        assertEquals("x.com", hostFromUrlText("x.com/home"))
        assertNull(hostFromUrlText("Instagram — explore"))
        assertNull(hostFromUrlText(""))
    }

    @Test
    fun `blocked domain is found through the address bar`() {
        val blocked = setOf("youtube.com", "instagram.com", "reddit.com")

        assertEquals("youtube.com", blockedDomainFor("https://m.youtube.com/watch?v=1", blocked))
        assertEquals("instagram.com", blockedDomainFor("instagram.com", blocked))
        assertNull(blockedDomainFor("example.com", blocked))
        assertNull(blockedDomainFor("not youtube", blocked))
    }

    @Test
    fun `allowed domains cover subdomains but not lookalikes`() {
        val allowed = setOf("wikipedia.org", "docs.google.com")

        assertTrue(isDomainAllowed("wikipedia.org", allowed))
        assertTrue(isDomainAllowed("en.wikipedia.org", allowed))
        assertTrue(isDomainAllowed("docs.google.com", allowed))
        assertFalse(isDomainAllowed("notwikipedia.org", allowed))
        assertFalse(isDomainAllowed("drive.google.com", allowed))
    }

    @Test
    fun `categories and defaults stay in sync`() {
        assertTrue(DefaultBlockedDomains.contains("youtube.com"))
        assertTrue(DefaultBlockedDomains.contains("pornhub.com"))
        assertEquals("Social & video", websiteCategoryLabel("youtube.com"))
        assertEquals("Adult sites", websiteCategoryLabel("pornhub.com"))
        assertNull(websiteCategoryLabel("example.com"))
    }
}
