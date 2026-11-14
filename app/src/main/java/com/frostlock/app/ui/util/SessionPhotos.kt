package com.frostlock.app.ui.util

/**
 * Naming and sizing rules for locally stored session photos.
 *
 * Photos are copied into app-private storage and referenced by file name, never
 * by an absolute path, so the reference survives app data moves. Only names this
 * object generates are ever opened or deleted.
 */
object SessionPhotos {
    const val DIRECTORY = "session_photos"

    /** Longest edge kept when a photo is copied in. Keeps files small. */
    const val MAX_EDGE_PX = 1280

    private val MANAGED_NAME = Regex("^session_[0-9]+\\.jpg$")

    fun fileName(sessionId: Long): String = "session_$sessionId.jpg"

    /** True when [name] is one of our own generated photo names. */
    fun isManagedName(name: String): Boolean = MANAGED_NAME.matches(name)

    /**
     * Power-of-two sample size that brings the longest edge under [maxEdge].
     * Decoding with this avoids loading a full-resolution camera shot into
     * memory just to shrink it.
     */
    fun sampleSizeFor(
        width: Int,
        height: Int,
        maxEdge: Int = MAX_EDGE_PX
    ): Int {
        if (width <= 0 || height <= 0 || maxEdge <= 0) return 1
        var sample = 1
        while (maxOf(width, height) / sample > maxEdge) {
            sample *= 2
        }
        return sample
    }
}
