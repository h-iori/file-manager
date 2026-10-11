package com.ioristudios.filemanager.ui.main

import androidx.lifecycle.ViewModel
import com.ioristudios.filemanager.data.DataRepository
import com.ioristudios.filemanager.data.DirectoryScreenState
import com.ioristudios.filemanager.data.HomeScreenData
import com.ioristudios.filemanager.data.SettingsUiState
import com.ioristudios.filemanager.data.SortOption
import com.ioristudios.filemanager.data.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NavTab {
    HOME,
    FILES,
    SHARE,
    SETTINGS
}

class MainScreenViewModel(private val repository: DataRepository) : ViewModel() {
    val homeUiState: StateFlow<HomeScreenData> = repository.homeScreenData
    val directoryUiState: StateFlow<DirectoryScreenState> = repository.directoryState
    val settingsUiState: StateFlow<SettingsUiState> = repository.settingsState

    private val _selectedTab = MutableStateFlow(NavTab.HOME)
    val selectedTab: StateFlow<NavTab> = _selectedTab.asStateFlow()

    fun selectTab(tab: NavTab) {
        _selectedTab.value = tab
    }

    fun refresh(hasPermission: Boolean) {
        repository.refresh(hasPermission)
    }

    fun openDirectory(path: String) {
        repository.loadDirectory(path)
    }

    fun navigateUpDirectory(): Boolean {
        return repository.navigateUp()
    }

    fun setSortOption(sort: SortOption) {
        repository.setSortOption(sort)
    }

    fun setThemeMode(mode: ThemeMode) {
        repository.setThemeMode(mode)
    }

    fun toggleShowHiddenFiles(enabled: Boolean) {
        repository.setShowHiddenFiles(enabled)
    }

    fun toggleShowFileExtensions(enabled: Boolean) {
        repository.setShowFileExtensions(enabled)
    }

    fun toggleRecycleBin(enabled: Boolean) {
        repository.setRecycleBinEnabled(enabled)
    }
}
