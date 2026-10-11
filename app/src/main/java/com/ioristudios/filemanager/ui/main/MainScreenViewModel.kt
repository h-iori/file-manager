package com.ioristudios.filemanager.ui.main

import androidx.lifecycle.ViewModel
import com.ioristudios.filemanager.data.DataRepository
import com.ioristudios.filemanager.data.DirectoryScreenState
import com.ioristudios.filemanager.data.HomeScreenData
import com.ioristudios.filemanager.data.SortOption
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
}
