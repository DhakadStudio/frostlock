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
    val packages: Set<String> = emptySet()
) {
    /** True when this subject has its own app list instead of the global fallback. */
    val hasApps: Boolean get() = packages.isNotEmpty()
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
                            packages = packages
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
            packages = emptySet()
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
                }
            )
        }
        prefs.edit().putString(KEY_SUBJECTS, array.toString()).apply()
    }

    private fun defaults(): List<FocusSubject> = listOf(
        FocusSubject(id = 1L, name = "Study"),
        FocusSubject(id = 2L, name = "Work"),
        FocusSubject(id = 3L, name = "Reading"),
        FocusSubject(id = 4L, name = "Coding"),
        FocusSubject(id = 5L, name = "Revision")
    )

    companion object {
        const val PREFS_NAME = "focusflow_subjects"
        private const val KEY_SUBJECTS = "subjects"
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_PACKAGES = "packages"
        const val MAX_NAME_LENGTH = 24
    }
}
