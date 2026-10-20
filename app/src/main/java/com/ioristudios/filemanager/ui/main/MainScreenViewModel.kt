package com.ioristudios.filemanager.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioristudios.filemanager.data.ArchiveType
import com.ioristudios.filemanager.data.ClipboardMode
import com.ioristudios.filemanager.data.ClipboardState
import com.ioristudios.filemanager.data.CompressionLevel
import com.ioristudios.filemanager.data.ConflictResolution
import com.ioristudios.filemanager.data.DataRepository
import com.ioristudios.filemanager.data.DirectoryScreenState
import com.ioristudios.filemanager.data.FileOpProgress
import com.ioristudios.filemanager.data.FileOperationController
import com.ioristudios.filemanager.data.HomeScreenData
import com.ioristudios.filemanager.data.InstalledAppItem
import com.ioristudios.filemanager.data.PasteResult
import com.ioristudios.filemanager.data.SearchFilterCategory
import com.ioristudios.filemanager.data.SearchUiState
import com.ioristudios.filemanager.data.SettingsUiState
import com.ioristudios.filemanager.data.SortOption
import com.ioristudios.filemanager.data.ThemeMode
import com.ioristudios.filemanager.ui.components.AppFileOpener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class NavTab {
    HOME,
    FILES,
    SEARCH,
    SHARE,
    SETTINGS
}

class MainScreenViewModel(private val repository: DataRepository) : ViewModel() {
    val homeUiState: StateFlow<HomeScreenData> = repository.homeScreenData
    val directoryUiState: StateFlow<DirectoryScreenState> = repository.directoryState
    val settingsUiState: StateFlow<SettingsUiState> = repository.settingsState
    val clipboardState: StateFlow<ClipboardState> = repository.clipboardState
    val searchUiState: StateFlow<SearchUiState> = repository.searchState
    val fileOpProgress: StateFlow<FileOpProgress> = FileOperationController.progressState

    fun pauseFileOperation() {
        FileOperationController.pause()
    }

    fun resumeFileOperation() {
        FileOperationController.resume()
    }

    fun cancelFileOperation() {
        FileOperationController.cancel()
    }

    fun dismissFileOperationModal() {
        FileOperationController.reset()
    }

    private val _selectedTab = MutableStateFlow(NavTab.HOME)
    val selectedTab: StateFlow<NavTab> = _selectedTab.asStateFlow()

    fun selectTab(tab: NavTab) {
        _selectedTab.value = tab
    }

    fun selectStorageVolume(volumeId: String) {
        repository.selectStorageVolume(volumeId)
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

    // Directory Scroll State Restoration Cache
    // Maps directory path -> Pair(firstVisibleItemIndex, firstVisibleItemScrollOffset)
    private val directoryScrollPositions = java.util.concurrent.ConcurrentHashMap<String, Pair<Int, Int>>()

    fun saveDirectoryScrollPosition(path: String, index: Int, offset: Int) {
        if (path.isNotBlank()) {
            directoryScrollPositions[path] = Pair(index, offset)
        }
    }

    fun getDirectoryScrollPosition(path: String): Pair<Int, Int>? {
        return if (path.isNotBlank()) directoryScrollPositions[path] else null
    }

    fun clearDirectoryScrollPosition(path: String) {
        directoryScrollPositions.remove(path)
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

    // Clipboard & File Operations
    fun copyItem(path: String, sourceDir: String) {
        repository.copyItems(listOf(path), sourceDir)
    }

    fun cutItem(path: String, sourceDir: String) {
        repository.cutItems(listOf(path), sourceDir)
    }

    fun copyMultipleItems(paths: List<String>, sourceDir: String) {
        repository.copyItems(paths, sourceDir)
    }

    fun cutMultipleItems(paths: List<String>, sourceDir: String) {
        repository.cutItems(paths, sourceDir)
    }

    fun clearClipboard() {
        repository.clearClipboard()
    }

    fun pasteClipboard(
        targetPath: String,
        conflictStrategy: ConflictResolution = ConflictResolution.AUTO_RENAME,
        onComplete: (PasteResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.pasteClipboard(targetPath, conflictStrategy)
            onComplete(result)
        }
    }

    fun renameItem(
        filePath: String,
        newName: String,
        onResult: (Result<File>) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.renameItem(filePath, newName)
            onResult(result)
        }
    }

    fun createFolder(
        parentPath: String,
        name: String,
        onResult: (Result<File>) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.createFolder(parentPath, name)
            onResult(result)
        }
    }

    fun createBlankFile(
        parentPath: String,
        name: String,
        onResult: (Result<File>) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.createBlankFile(parentPath, name)
            onResult(result)
        }
    }

    fun deleteItem(
        filePath: String,
        onResult: (Result<Int>) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.deleteItems(listOf(filePath))
            onResult(result)
        }
    }

    fun deleteMultipleItems(
        filePaths: List<String>,
        onResult: ((Result<Int>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = repository.deleteItems(filePaths)
            onResult?.invoke(result)
        }
    }

    // Search operations
    private var searchJob: kotlinx.coroutines.Job? = null

    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isBlank()) {
                repository.search("", searchUiState.value.selectedCategory)
            } else {
                kotlinx.coroutines.delay(120)
                repository.search(query, searchUiState.value.selectedCategory)
            }
        }
    }

    fun onSearchCategorySelected(category: SearchFilterCategory) {
        repository.setSearchCategory(category)
    }

    fun clearSearch() {
        searchJob?.cancel()
        repository.clearSearch()
    }

    // ZArchiver Power Features
    fun extractArchive(
        archivePath: String,
        targetDir: String,
        onResult: (Result<Int>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.extractArchive(archivePath, targetDir)
            onResult(res)
        }
    }

    fun createArchive(
        sourcePaths: List<String>,
        destFile: File,
        format: ArchiveType,
        level: CompressionLevel,
        onResult: (Result<File>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.createArchive(sourcePaths, destFile, format, level)
            onResult(res)
        }
    }

    fun testArchiveIntegrity(
        archivePath: String,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.testArchiveIntegrity(archivePath)
            onResult(res)
        }
    }

    fun extractAndOpenArchiveItem(
        context: Context,
        virtualPath: String,
        mimeType: String?,
        showMessage: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.extractAndPrepareArchiveFile(virtualPath)
            res.onSuccess { tempFile ->
                AppFileOpener.openFile(context, tempFile.absolutePath, mimeType, showMessage)
            }.onFailure { err ->
                showMessage("Failed to open archive item: ${err.message}")
            }
        }
    }

    fun loadInstalledApps(
        includeSystem: Boolean = false,
        onResult: (List<InstalledAppItem>) -> Unit
    ) {
        viewModelScope.launch {
            val apps = repository.getInstalledApps(includeSystem)
            onResult(apps)
        }
    }

    fun extractAppApk(
        app: InstalledAppItem,
        onResult: (Result<File>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.extractAppApk(app)
            onResult(res)
        }
    }
}
