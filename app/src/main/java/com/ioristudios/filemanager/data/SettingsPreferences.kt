package com.ioristudios.filemanager.data

import android.content.Context
import android.content.SharedPreferences

class SettingsPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("file_manager_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_SHOW_HIDDEN_FILES = "pref_show_hidden_files"
        private const val KEY_SHOW_FILE_EXTENSIONS = "pref_show_file_extensions"
    }

    fun getSettings(): SettingsUiState {
        val themeModeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        val themeMode = try {
            ThemeMode.valueOf(themeModeStr)
        } catch (_: Exception) {
            ThemeMode.DARK
        }
        val showHidden = prefs.getBoolean(KEY_SHOW_HIDDEN_FILES, true)
        val showExt = prefs.getBoolean(KEY_SHOW_FILE_EXTENSIONS, true)

        return SettingsUiState(
            themeMode = themeMode,
            showHiddenFiles = showHidden,
            showFileExtensions = showExt
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setShowHiddenFiles(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_HIDDEN_FILES, enabled).apply()
    }

    fun setShowFileExtensions(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_FILE_EXTENSIONS, enabled).apply()
    }

}
