package com.focusflow.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A subject the user studies or works on, with its own set of apps to block.
 *
 * Subjects are intentionally separate from [FocusSchedule] rules and from daily
 * limits: a subject is the app list you reuse, while a rule decides *when* that
 * list applies. Keeping the two apart means one subject can be used by a timer
 * session and by several rules without duplicating the app selection.
 */
data class FocusSubject(
    val id: Long,
    val name: String,
    val packages: Set<String> = emptySet(),
    /** ARGB colour used for chips, dots and the session badge. */
    val colorArgb: Int = SUBJECT_COLORS.first()
) {
    /** True when this subject has its own app list instead of the global fallback. */
    val hasApps: Boolean get() = packages.isNotEmpty()

    companion object {
        val SUBJECT_COLORS = listOf(
            0xFFFACC15.toInt(), // amber
            0xFF60A5FA.toInt(), // blue
            0xFFF472B6.toInt(), // pink
            0xFF34D399.toInt(), // green
            0xFFFB923C.toInt(), // orange
            0xFFA78BFA.toInt(), // violet
            0xFF22D3EE.toInt() // cyan
        )

        fun colorFor(index: Int): Int =
            SUBJECT_COLORS[((index % SUBJECT_COLORS.size) + SUBJECT_COLORS.size) % SUBJECT_COLORS.size]
    }
}

/**
 * The app set a focus session should lock.
 *
 * A subject with apps wins. A missing or empty subject falls back to the global
 * focus-session list, which keeps older sessions and first-run users working.
 */
fun effectiveSessionPackages(
    subject: FocusSubject?,
    fallbackPackages: Set<String>
): Set<String> = subject?.packages?.takeIf { it.isNotEmpty() } ?: fallbackPackages

/** Case-insensitive identity for subject lookups ("Study" and "study" match). */
fun subjectKey(name: String): String = name.trim().lowercase(Locale.ROOT)

/**
 * The chosen subject names live in SharedPreferences as JSON so the accessibility
 * service and the Compose screens see exactly the same list without a database
 * round-trip on every window change.
 */
@Singleton
class SubjectStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        if (!prefs.contains(KEY_SUBJECTS)) save(defaults())
    }

    fun subjects(): List<FocusSubject> {
        val raw = prefs.getString(KEY_SUBJECTS, null) ?: return defaults()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val name = obj.optString(KEY_NAME).trim()
                    if (name.isBlank()) continue
                    val packagesJson = obj.optJSONArray(KEY_PACKAGES) ?: JSONArray()
                    val packages = buildSet {
                        for (p in 0 until packagesJson.length()) {
                            packagesJson.optString(p)
                                .takeIf { it.isNotBlank() }
                                ?.let { add(it) }
                        }
                    }
                    add(
                        FocusSubject(
                            id = obj.optLong(KEY_ID, name.hashCode().toLong()),
                            name = name,
                            packages = packages,
                            colorArgb = obj.optInt(KEY_COLOR, FocusSubject.colorFor(i))
                        )
                    )
                }
            }
        }.getOrDefault(defaults())
    }

    fun subjectFor(name: String): FocusSubject? {
        val key = subjectKey(name)
        if (key.isBlank()) return null
        return subjects().firstOrNull { subjectKey(it.name) == key }
    }

    /** Returns the existing subject or creates a new, app-less one. */
    fun ensureSubject(name: String): FocusSubject? {
        val cleaned = name.trim().take(MAX_NAME_LENGTH)
        if (cleaned.isBlank()) return null
        subjectFor(cleaned)?.let { return it }

        val subject = FocusSubject(
            id = System.currentTimeMillis(),
            name = cleaned,
            packages = emptySet(),
            colorArgb = FocusSubject.colorFor(subjects().size)
        )
        save(subjects() + subject)
        return subject
    }

    fun setPackages(name: String, packages: Set<String>) {
        val subject = ensureSubject(name) ?: return
        save(
            subjects().map {
                if (it.id == subject.id) it.copy(packages = packages) else it
            }
        )
    }

    fun save(subjects: List<FocusSubject>) {
        val array = JSONArray()
        subjects.distinctBy { subjectKey(it.name) }.forEach { subject ->
            array.put(
                JSONObject().apply {
                    put(KEY_ID, subject.id)
                    put(KEY_NAME, subject.name)
                    put(KEY_PACKAGES, JSONArray(subject.packages.sorted()))
                    put(KEY_COLOR, subject.colorArgb)
                }
            )
        }
        prefs.edit().putString(KEY_SUBJECTS, array.toString()).apply()
    }

    private fun defaults(): List<FocusSubject> = listOf(
        FocusSubject(id = 1L, name = "Study", colorArgb = FocusSubject.colorFor(0)),
        FocusSubject(id = 2L, name = "Work", colorArgb = FocusSubject.colorFor(1)),
        FocusSubject(id = 3L, name = "Reading", colorArgb = FocusSubject.colorFor(2)),
        FocusSubject(id = 4L, name = "Coding", colorArgb = FocusSubject.colorFor(3)),
        FocusSubject(id = 5L, name = "Revision", colorArgb = FocusSubject.colorFor(4))
    )

    companion object {
        const val PREFS_NAME = "focusflow_subjects"
        private const val KEY_SUBJECTS = "subjects"
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_PACKAGES = "packages"
        private const val KEY_COLOR = "color"
        const val MAX_NAME_LENGTH = 24
    }
}
