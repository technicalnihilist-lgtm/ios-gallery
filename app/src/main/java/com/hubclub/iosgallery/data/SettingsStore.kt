package com.hubclub.iosgallery.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Everything the user can change about how the app looks.
 * Stored in private SharedPreferences — on the device, nowhere else.
 */
data class AppSettings(
    /** Dock tile scale: 0.75 (compact) … 1.35 (large). */
    val dockScale: Float = 1f,
    /** Glass fill opacity: 0.30 (very transparent) … 1.00 (solid). */
    val glassOpacity: Float = 1f,
    /** "system" | "light" | "dark". */
    val themeMode: String = "system"
) {
    val darkOverride: Boolean?
        get() = when (themeMode) {
            "light" -> false
            "dark" -> true
            else -> null
        }
}

class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ios_gallery_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    private fun read(): AppSettings = AppSettings(
        dockScale = prefs.getFloat(KEY_DOCK, 1f).coerceIn(MIN_DOCK, MAX_DOCK),
        glassOpacity = prefs.getFloat(KEY_GLASS, 1f).coerceIn(MIN_GLASS, 1f),
        themeMode = prefs.getString(KEY_THEME, "system") ?: "system"
    )

    fun setDockScale(value: Float) = update { it.copy(dockScale = value.coerceIn(MIN_DOCK, MAX_DOCK)) }

    fun setGlassOpacity(value: Float) = update { it.copy(glassOpacity = value.coerceIn(MIN_GLASS, 1f)) }

    fun setThemeMode(value: String) = update { it.copy(themeMode = value) }

    fun reset() {
        prefs.edit().clear().apply()
        _state.value = AppSettings()
    }

    private fun update(block: (AppSettings) -> AppSettings) {
        val next = block(_state.value)
        _state.value = next
        prefs.edit()
            .putFloat(KEY_DOCK, next.dockScale)
            .putFloat(KEY_GLASS, next.glassOpacity)
            .putString(KEY_THEME, next.themeMode)
            .apply()
    }

    companion object {
        const val MIN_DOCK = 0.75f
        const val MAX_DOCK = 1.35f
        const val MIN_GLASS = 0.30f
        private const val KEY_DOCK = "dockScale"
        private const val KEY_GLASS = "glassOpacity"
        private const val KEY_THEME = "themeMode"
    }
}
