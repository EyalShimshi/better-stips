package io.github.eyalshimshi.betterstips.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { System, Light, Dark }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val pureBlack: Boolean = false,
    val dynamicColor: Boolean = true,
    val accent: Int = DEFAULT_ACCENT,
    val showImages: Boolean = true,
    val animations: Boolean = true,
    /** New questions appear on Home by themselves (polls every 30 s while Home is open). */
    val autoRefreshNew: Boolean = false,
    /** Phone notifications from a background check every 15 minutes. */
    val backgroundNotifications: Boolean = false,
) {
    companion object {
        /** Stips green: the fallback seed when dynamic color is off (D-06). */
        val DEFAULT_ACCENT: Int = 0xFF09C286.toInt()

        /** Stips green, then DESIGN-PHILOSOPHY's 8 presets. */
        val ACCENTS: List<Int> = listOf(
            0xFF09C286, 0xFF6750A4, 0xFF0061A4, 0xFF006A6A, 0xFF386A20, 0xFF8B5000, 0xFF9C4146, 0xFFB4236C, 0xFF0038B8,
        ).map { it.toInt() }
    }
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        prefs.edit()
            .putInt("themeMode", next.themeMode.ordinal)
            .putBoolean("pureBlack", next.pureBlack)
            .putBoolean("dynamicColor", next.dynamicColor)
            .putInt("accent", next.accent)
            .putBoolean("showImages", next.showImages)
            .putBoolean("animations", next.animations)
            .putBoolean("autoRefreshNew", next.autoRefreshNew)
            .putBoolean("backgroundNotifications", next.backgroundNotifications)
            .apply()
    }

    private fun read(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = ThemeMode.entries.getOrNull(prefs.getInt("themeMode", 0)) ?: ThemeMode.System,
            pureBlack = prefs.getBoolean("pureBlack", defaults.pureBlack),
            dynamicColor = prefs.getBoolean("dynamicColor", defaults.dynamicColor),
            accent = prefs.getInt("accent", defaults.accent),
            showImages = prefs.getBoolean("showImages", defaults.showImages),
            animations = prefs.getBoolean("animations", defaults.animations),
            autoRefreshNew = prefs.getBoolean("autoRefreshNew", defaults.autoRefreshNew),
            backgroundNotifications = prefs.getBoolean("backgroundNotifications", defaults.backgroundNotifications),
        )
    }
}
