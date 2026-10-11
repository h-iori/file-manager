package com.ioristudios.filemanager.data

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

interface DataRepository {
    val homeScreenData: StateFlow<HomeScreenData>
    val directoryState: StateFlow<DirectoryScreenState>
    fun refresh(hasPermission: Boolean)
    fun loadDirectory(path: String? = null, sort: SortOption? = null)
    fun navigateUp(): Boolean
    fun setSortOption(sort: SortOption)
}

class DefaultDataRepository(private val context: Context) : DataRepository {
    private val scanner = StorageScanner(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _homeScreenData = MutableStateFlow(HomeScreenData())
    override val homeScreenData: StateFlow<HomeScreenData> = _homeScreenData.asStateFlow()

    private val rootStoragePath: String = Environment.getExternalStorageDirectory().absolutePath

    private val _directoryState = MutableStateFlow(
        DirectoryScreenState(
            currentPath = rootStoragePath,
            rootPath = rootStoragePath,
            breadcrumbs = listOf(BreadcrumbSegment("Storage", rootStoragePath))
        )
    )
    override val directoryState: StateFlow<DirectoryScreenState> = _directoryState.asStateFlow()

    init {
        refresh(hasPermission = false)
        loadDirectory(rootStoragePath)
    }

    override fun refresh(hasPermission: Boolean) {
        scope.launch {
            val data = scanner.scanHomeScreenData(hasPermission)
            _homeScreenData.value = data
            loadDirectory(_directoryState.value.currentPath)
        }
    }

    override fun loadDirectory(path: String?, sort: SortOption?) {
        val targetPath = path ?: _directoryState.value.currentPath
        val targetSort = sort ?: _directoryState.value.sortOption

        scope.launch {
            _directoryState.value = _directoryState.value.copy(isLoading = true)

            val items = scanner.scanDirectory(targetPath, targetSort)
            val breadcrumbs = buildBreadcrumbs(targetPath)

            _directoryState.value = _directoryState.value.copy(
                currentPath = targetPath,
                items = items,
                breadcrumbs = breadcrumbs,
                sortOption = targetSort,
                isLoading = false
            )
        }
    }

    override fun navigateUp(): Boolean {
        val current = _directoryState.value.currentPath
        if (current == rootStoragePath || current.isEmpty()) {
            return false
        }
        val parent = File(current).parentFile
        if (parent != null && parent.exists() && parent.absolutePath.startsWith(rootStoragePath)) {
            loadDirectory(parent.absolutePath)
            return true
        } else if (parent != null && parent.absolutePath == rootStoragePath) {
            loadDirectory(rootStoragePath)
            return true
        }
        return false
    }

    override fun setSortOption(sort: SortOption) {
        loadDirectory(_directoryState.value.currentPath, sort)
    }

    private fun buildBreadcrumbs(path: String): List<BreadcrumbSegment> {
        val list = mutableListOf<BreadcrumbSegment>()
        list.add(BreadcrumbSegment("Storage", rootStoragePath))

        if (path == rootStoragePath) return list

        val relative = path.removePrefix(rootStoragePath).trimStart(File.separatorChar)
        if (relative.isNotEmpty()) {
            val parts = relative.split(File.separatorChar)
            var accumulated = rootStoragePath
            for (part in parts) {
                accumulated = "$accumulated${File.separator}$part"
                list.add(BreadcrumbSegment(part, accumulated))
            }
        }
        return list
    }
}
