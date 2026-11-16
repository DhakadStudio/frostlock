package com.frostlock.app.ui.util

/**
 * Pure rules for Play Store search blocking.
 *
 * Searching the store by a blocked app's name is an easy way around app
 * blocking: the result page puts the app one tap away even while its own
 * package never runs. The text typed into the store's search box is matched
 * against the display labels of the blocked apps instead of package names,
 * because the store only knows apps by their visible names.
 */
object PlayStoreDetector {

    const val PACKAGE = "com.android.vending"

    /**
     * The blocked app whose name the [query] spells, or null.
     *
     * Both sides are tokenized into lowercase alphanumeric words and the label
     * must appear as one consecutive run. That way extra words around the label
     * still match ("best youtube music apps") while a short label ("X") never
     * lights up inside another word ("xbox").
     */
    fun matchedLabel(query: String, blockedLabels: Collection<String>): String? {
        val tokens = tokenize(query)
        if (tokens.isEmpty()) return null
        return blockedLabels.firstOrNull { label ->
            val labelTokens = tokenize(label)
            labelTokens.isNotEmpty() && containsRun(tokens, labelTokens)
        }
    }

    /** Splits text into lowercase alphanumeric word tokens. */
    private fun tokenize(text: String): List<String> =
        text.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotEmpty() }

    /** True when [needle] appears in [haystack] as one consecutive run. */
    private fun containsRun(haystack: List<String>, needle: List<String>): Boolean {
        if (needle.isEmpty() || haystack.size < needle.size) return false
        for (start in 0..haystack.size - needle.size) {
            var matched = true
            for (offset in needle.indices) {
                if (haystack[start + offset] != needle[offset]) {
                    matched = false
                    break
                }
            }
            if (matched) return true
        }
        return false
    }
}
