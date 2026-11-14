package com.frostlock.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import com.frostlock.app.ui.theme.IcePalettes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Remembers the selected ice palette; the theme itself lives in the UI layer. */
@Singleton
class ThemeStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _paletteKey = MutableStateFlow(
        prefs.getString(KEY_PALETTE, IcePalettes.Frost.key) ?: IcePalettes.Frost.key
    )
    val paletteKey: StateFlow<String> = _paletteKey.asStateFlow()

    fun setPalette(key: String) {
        val palette = IcePalettes.fromKey(key)
        prefs.edit().putString(KEY_PALETTE, palette.key).apply()
        _paletteKey.value = palette.key
    }

    companion object {
        const val PREFS_NAME = "frostlock_theme"
        const val KEY_PALETTE = "ice_palette"
    }
}
