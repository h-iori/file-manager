package com.ioristudios.filemanager.data

enum class ThemeMode {
    LIGHT,
    DARK,
    AUTO
}

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val showHiddenFiles: Boolean = true,
    val showFileExtensions: Boolean = true,
    val recycleBinEnabled: Boolean = true
)
