package com.frostlock.app.ui.util

/** A named group of sites shown on the website-blocking page. */
data class WebsiteCategory(
    val label: String,
    val domains: List<String>
)

/**
 * Curated starting list. Users can switch any entry off, and the choice is
 * stored locally; nothing is fetched or updated remotely.
 */
val WebsiteCategories: List<WebsiteCategory> = listOf(
    WebsiteCategory(
        label = "Social & video",
        domains = listOf(
            "youtube.com",
            "instagram.com",
            "facebook.com",
            "x.com",
            "twitter.com",
            "tiktok.com",
            "reddit.com",
            "snapchat.com",
            "twitch.tv",
            "netflix.com",
            "pinterest.com",
            "threads.net",
            "discord.com"
        )
    ),
    WebsiteCategory(
        label = "Adult sites",
        domains = listOf(
            "pornhub.com",
            "xvideos.com",
            "xnxx.com",
            "redtube.com",
            "onlyfans.com"
        )
    )
)

/** Every curated domain, used to seed the local block list. */
val DefaultBlockedDomains: Set<String> =
    WebsiteCategories.flatMap { it.domains }.toSet()

private val DOMAIN_REGEX =
    Regex("^([a-z0-9]([a-z0-9-]*[a-z0-9])?\\.)+[a-z]{2,}$")

/**
 * Cleans a typed domain or URL into a bare host. Returns null for anything that
 * is not a plausible domain, so a stray search query can never enter the list.
 */
fun normalizeDomain(raw: String): String? {
    val trimmed = raw.trim().lowercase()
    if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null

    val host = runCatching {
        java.net.URI(if ("://" in trimmed) trimmed else "https://$trimmed").host
    }.getOrNull()
        ?: trimmed.substringBefore('/').substringBefore('?').substringBefore('#')

    val cleaned = host
        .removePrefix("www.")
        .trimEnd('.')
        .lowercase()

    if (cleaned.isEmpty() || cleaned.length > 253) return null
    if (!DOMAIN_REGEX.matches(cleaned)) return null
    return cleaned
}

/** True when [host] is [domain] or one of its subdomains. */
fun domainMatches(host: String, domain: String): Boolean {
    val normalizedHost = host.trim().lowercase().removePrefix("www.")
    val normalizedDomain = domain.trim().lowercase().removePrefix("www.")
    if (normalizedHost.isEmpty() || normalizedDomain.isEmpty()) return false
    return normalizedHost == normalizedDomain ||
        normalizedHost.endsWith(".$normalizedDomain")
}

/**
 * Extracts a host from the text shown in a browser address bar. Full URLs,
 * bare hosts and paths are accepted; queries and page titles are rejected.
 */
fun hostFromUrlText(text: String): String? {
    val trimmed = text.trim()
    if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
    return normalizeDomain(trimmed)
}

/** The first blocked domain that matches the address-bar text, if any. */
fun blockedDomainFor(text: String, blockedDomains: Set<String>): String? {
    val host = hostFromUrlText(text) ?: return null
    return blockedDomains.firstOrNull { domainMatches(host, it) }
}

/** True when [host] matches any domain on the local allowlist. */
fun isDomainAllowed(host: String, allowedDomains: Set<String>): Boolean =
    allowedDomains.any { domainMatches(host, it) }

/** Which curated category a domain belongs to, or null for a custom site. */
fun websiteCategoryLabel(domain: String): String? =
    WebsiteCategories.firstOrNull { category ->
        category.domains.any { it == domain }
    }?.label
