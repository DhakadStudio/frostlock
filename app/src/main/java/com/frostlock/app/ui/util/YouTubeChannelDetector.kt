package com.frostlock.app.ui.util

/**
 * Pure rules for YouTube channel filtering.
 *
 * On the watch page this build of YouTube exposes the video owner's channel
 * through accessibility descriptions rather than plain text: the Subscribe
 * button ("Subscribe to Rick Astley.") and the channel avatar ("Rick Astley,
 * Official Artist Channel 4.56M subscribers"). Chips of recommended videos
 * ("Go to channel X") describe other channels and are deliberately not
 * parsed, so an unrelated recommendation can never wall an allowed video.
 */
object YouTubeChannelDetector {

    /** The main YouTube app. */
    const val PACKAGE = "com.google.android.youtube"

    /** YouTube Music exposes the same owner-row labels on its now-playing page. */
    const val PACKAGE_MUSIC = "com.google.android.apps.youtube.music"

    /** Both apps share the channel model and the same block/study lists. */
    val PACKAGES = setOf(PACKAGE, PACKAGE_MUSIC)

    private val subscribeLabel = Regex("^Subscribe to (.+?)\\.?$", RegexOption.IGNORE_CASE)
    private val subscriberLabel = Regex(
        "^(.+?)(?:, Official Artist Channel)? [0-9][0-9.,]*[KMB]? subscribers$",
        RegexOption.IGNORE_CASE
    )

    /**
     * The channel name out of an owner-row accessibility label, or null when
     * the label is something else (video cards, actions, captions).
     */
    fun ownerChannelName(label: String): String? {
        val text = label.trim()
        subscribeLabel.matchEntire(text)?.let { return normalize(it.groupValues[1]) }
        subscriberLabel.matchEntire(text)?.let { return normalize(it.groupValues[1]) }
        return null
    }

    /** Lower-cased, whitespace-collapsed name or handle without a leading "@". */
    fun normalize(raw: String): String =
        raw.lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
            .removePrefix("@")

    /**
     * The channel that should be walled right now, or null when the page is
     * fine. Study mode only decides when the allowlist is non-empty, so an
     * empty study list never walls every channel at once.
     */
    fun violatingOwner(
        allowOnly: Boolean,
        ownerNames: Set<String>,
        blocked: Set<String>,
        allowed: Set<String>
    ): String? {
        if (ownerNames.isEmpty()) return null
        return if (allowOnly) {
            if (allowed.isEmpty()) return null
            val permitted = allowed.map(::normalize).toSet()
            ownerNames.firstOrNull { normalize(it) !in permitted }
        } else {
            val banned = blocked.map(::normalize).toSet()
            ownerNames.firstOrNull { normalize(it) in banned }
        }
    }
}
