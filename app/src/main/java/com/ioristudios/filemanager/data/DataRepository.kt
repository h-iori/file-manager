package com.ioristudios.filemanager.data

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

interface DataRepository {
    val homeScreenData: StateFlow<HomeScreenData>
    val directoryState: StateFlow<DirectoryScreenState>
    val settingsState: StateFlow<SettingsUiState>
    val clipboardState: StateFlow<ClipboardState>
    val searchState: StateFlow<SearchUiState>

    fun refresh(hasPermission: Boolean)
    fun selectStorageVolume(volumeId: String)
    fun loadDirectory(path: String? = null, sort: SortOption? = null, customRoot: String? = null)
    fun navigateUp(): Boolean
    fun setSortOption(sort: SortOption)
    fun setThemeMode(mode: ThemeMode)
    fun setShowHiddenFiles(enabled: Boolean)
    fun setShowFileExtensions(enabled: Boolean)
    fun setRecycleBinEnabled(enabled: Boolean)

    // Clipboard and File Operations
    fun copyItems(paths: List<String>, sourceDir: String)
    fun cutItems(paths: List<String>, sourceDir: String)
    fun clearClipboard()
    suspend fun pasteClipboard(targetPath: String, conflictStrategy: ConflictResolution = ConflictResolution.AUTO_RENAME): PasteResult
    suspend fun renameItem(filePath: String, newName: String): Result<File>
    suspend fun createFolder(parentPath: String, name: String): Result<File>
    suspend fun createBlankFile(parentPath: String, name: String): Result<File>
    suspend fun deleteItems(filePaths: List<String>): Result<Int>

    // Enterprise Search
    suspend fun search(query: String, category: SearchFilterCategory = SearchFilterCategory.ALL)
    fun setSearchCategory(category: SearchFilterCategory)
    fun clearSearch()

    // ZArchiver Power Features
    suspend fun extractArchive(archivePath: String, targetDir: String): Result<Int>
    suspend fun createArchive(sourcePaths: List<String>, destFile: File, format: ArchiveType, level: CompressionLevel): Result<File>
    suspend fun testArchiveIntegrity(archivePath: String): Result<String>
    suspend fun extractAndPrepareArchiveFile(virtualPath: String): Result<File>
    suspend fun getInstalledApps(includeSystem: Boolean = false): List<InstalledAppItem>
    suspend fun extractAppApk(app: InstalledAppItem): Result<File>
}

class DefaultDataRepository(private val context: Context) : DataRepository {
    private val scanner = StorageScanner(context)
    private val prefs = SettingsPreferences(context)
    private val searchEngine = FileSearchEngine(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _settingsState = MutableStateFlow(prefs.getSettings())
    override val settingsState: StateFlow<SettingsUiState> = _settingsState.asStateFlow()

    private val _homeScreenData = MutableStateFlow(HomeScreenData())
    override val homeScreenData: StateFlow<HomeScreenData> = _homeScreenData.asStateFlow()

    private val defaultRootPath: String = Environment.getExternalStorageDirectory().absolutePath
    private var activeRootPath: String = defaultRootPath
    private var activeVolumeName: String = "Storage"

    private val _directoryState = MutableStateFlow(
        DirectoryScreenState(
            currentPath = defaultRootPath,
            rootPath = defaultRootPath,
            breadcrumbs = listOf(BreadcrumbSegment("Storage", defaultRootPath))
        )
    )
    override val directoryState: StateFlow<DirectoryScreenState> = _directoryState.asStateFlow()

    private val _clipboardState = MutableStateFlow(ClipboardState())
    override val clipboardState: StateFlow<ClipboardState> = _clipboardState.asStateFlow()

    override val searchState: StateFlow<SearchUiState> = searchEngine.searchState

    init {
        refresh(hasPermission = false)
        loadDirectory(defaultRootPath)
    }

    override fun refresh(hasPermission: Boolean) {
        scope.launch {
            scanner.clearDirCache()
            val selectedId = _homeScreenData.value.selectedVolumeId
            val data = scanner.scanHomeScreenData(hasPermission, selectedId)
            _homeScreenData.value = data
            loadDirectory(_directoryState.value.currentPath)
        }
    }

    override fun selectStorageVolume(volumeId: String) {
        scope.launch {
            val hasPerm = _homeScreenData.value.hasPermission
            val data = scanner.scanHomeScreenData(hasPerm, volumeId)
            _homeScreenData.value = data

            val selectedVol = data.availableVolumes.find { it.id == volumeId }
                ?: data.availableVolumes.firstOrNull()

            if (selectedVol != null) {
                activeRootPath = selectedVol.path
                activeVolumeName = when (selectedVol.type) {
                    StorageVolumeType.INTERNAL -> "Internal"
                    StorageVolumeType.SD_CARD -> "SD Card"
                    StorageVolumeType.USB_OTG -> "USB Drive"
                }
                loadDirectory(path = selectedVol.path, customRoot = selectedVol.path)
            }
        }
    }

    override fun loadDirectory(path: String?, sort: SortOption?, customRoot: String?) {
        val root = customRoot ?: activeRootPath
        val targetPath = path ?: _directoryState.value.currentPath
        val targetSort = sort ?: _directoryState.value.sortOption

        scope.launch {
            val protectionInfo = SecurityProtectionManager.checkProtection(targetPath)

            val items: List<DirectoryItem>
            val breadcrumbs: List<BreadcrumbSegment>

            if (targetPath.contains("::")) {
                val archiveFilePath = targetPath.substringBefore("::")
                val innerPath = targetPath.substringAfter("::")
                val archiveFile = File(archiveFilePath)
                items = ArchiveManager.listArchiveEntries(archiveFile, innerPath)
                breadcrumbs = buildArchiveBreadcrumbs(archiveFile, innerPath, root)
            } else if (ArchiveManager.isArchive(File(targetPath))) {
                val archiveFile = File(targetPath)
                items = ArchiveManager.listArchiveEntries(archiveFile, "")
                breadcrumbs = buildArchiveBreadcrumbs(archiveFile, "", root)
            } else {
                items = scanner.scanDirectory(
                    directoryPath = targetPath,
                    sortOption = targetSort,
                    showHiddenFiles = _settingsState.value.showHiddenFiles
                )
                breadcrumbs = buildBreadcrumbs(targetPath, root)
            }

            _directoryState.value = _directoryState.value.copy(
                currentPath = targetPath,
                rootPath = root,
                items = items,
                breadcrumbs = breadcrumbs,
                sortOption = targetSort,
                isLoading = false,
                protectionInfo = if (protectionInfo.isProtected) protectionInfo else null
            )
        }
    }

    override fun navigateUp(): Boolean {
        val current = _directoryState.value.currentPath
        val root = _directoryState.value.rootPath

        if (current.contains("::")) {
            val archivePath = current.substringBefore("::")
            val inner = current.substringAfter("::").trim('/')
            if (inner.contains('/')) {
                val parentInner = inner.substringBeforeLast('/')
                loadDirectory("$archivePath::$parentInner")
                return true
            } else {
                val parentOfArchive = File(archivePath).parentFile?.absolutePath ?: root
                loadDirectory(parentOfArchive)
                return true
            }
        } else if (ArchiveManager.isArchive(File(current))) {
            val parentOfArchive = File(current).parentFile?.absolutePath ?: root
            loadDirectory(parentOfArchive)
            return true
        }

        if (current == root || current.isEmpty()) {
            return false
        }
        val parent = File(current).parentFile
        if (parent != null && parent.exists() && parent.absolutePath.startsWith(root)) {
            loadDirectory(parent.absolutePath)
            return true
        } else if (parent != null && parent.absolutePath == root) {
            loadDirectory(root)
            return true
        }
        return false
    }

    override fun setSortOption(sort: SortOption) {
        loadDirectory(_directoryState.value.currentPath, sort)
    }

    override fun setThemeMode(mode: ThemeMode) {
        prefs.setThemeMode(mode)
        _settingsState.value = prefs.getSettings()
    }

    override fun setShowHiddenFiles(enabled: Boolean) {
        prefs.setShowHiddenFiles(enabled)
        _settingsState.value = prefs.getSettings()
        loadDirectory(_directoryState.value.currentPath)
    }

    override fun setShowFileExtensions(enabled: Boolean) {
        prefs.setShowFileExtensions(enabled)
        _settingsState.value = prefs.getSettings()
    }

    override fun setRecycleBinEnabled(enabled: Boolean) {
        prefs.setRecycleBinEnabled(enabled)
        _settingsState.value = prefs.getSettings()
    }

    override fun copyItems(paths: List<String>, sourceDir: String) {
        _clipboardState.value = ClipboardState(
            items = paths,
            mode = ClipboardMode.COPY,
            sourceDirectory = sourceDir,
            isActive = paths.isNotEmpty()
        )
    }

    override fun cutItems(paths: List<String>, sourceDir: String) {
        _clipboardState.value = ClipboardState(
            items = paths,
            mode = ClipboardMode.CUT,
            sourceDirectory = sourceDir,
            isActive = paths.isNotEmpty()
        )
    }

    override fun clearClipboard() {
        _clipboardState.value = ClipboardState(isActive = false)
    }

    override suspend fun pasteClipboard(
        targetPath: String,
        conflictStrategy: ConflictResolution
    ): PasteResult {
        val clip = _clipboardState.value
        if (!clip.isActive || clip.items.isEmpty()) {
            return PasteResult(0, 0, 0, "Clipboard is empty")
        }

        val result = FileOperationsManager.executePaste(
            sourcePaths = clip.items,
            mode = clip.mode,
            targetDirectoryPath = targetPath,
            conflictStrategy = conflictStrategy
        )

        if (clip.mode == ClipboardMode.CUT && result.successCount > 0) {
            clearClipboard()
        }

        loadDirectory(targetPath)
        refresh(_homeScreenData.value.hasPermission)
        return result
    }

    override suspend fun renameItem(filePath: String, newName: String): Result<File> {
        val target = File(filePath)
        val result = FileOperationsManager.renameItem(target, newName)
        if (result.isSuccess) {
            loadDirectory(_directoryState.value.currentPath)
            refresh(_homeScreenData.value.hasPermission)
        }
        return result
    }

    override suspend fun createFolder(parentPath: String, name: String): Result<File> {
        val parent = File(parentPath)
        val result = FileOperationsManager.createFolder(parent, name)
        if (result.isSuccess) {
            loadDirectory(parentPath)
            refresh(_homeScreenData.value.hasPermission)
        }
        return result
    }

    override suspend fun createBlankFile(parentPath: String, name: String): Result<File> {
        val parent = File(parentPath)
        val result = FileOperationsManager.createBlankFile(parent, name)
        if (result.isSuccess) {
            loadDirectory(parentPath)
            refresh(_homeScreenData.value.hasPermission)
        }
        return result
    }

    override suspend fun deleteItems(filePaths: List<String>): Result<Int> {
        val files = filePaths.map { File(it) }
        val result = FileOperationsManager.deleteItems(files)
        if (result.isSuccess) {
            loadDirectory(_directoryState.value.currentPath)
            refresh(_homeScreenData.value.hasPermission)
        }
        return result
    }

    override suspend fun search(query: String, category: SearchFilterCategory) {
        searchEngine.search(
            query = query,
            rootPath = activeRootPath,
            category = category,
            showHidden = _settingsState.value.showHiddenFiles
        )
    }

    override fun setSearchCategory(category: SearchFilterCategory) {
        searchEngine.setFilterCategory(category)
    }

    override fun clearSearch() {
        searchEngine.clearSearch()
    }

    // ZArchiver Power Features Implementations
    override suspend fun extractArchive(archivePath: String, targetDir: String): Result<Int> = withContext(Dispatchers.IO) {
        val archiveFile = File(archivePath)
        val outDir = File(targetDir)
        val res = ArchiveManager.extractAll(archiveFile, outDir)
        if (res.isSuccess) {
            loadDirectory(_directoryState.value.currentPath)
            refresh(_homeScreenData.value.hasPermission)
        }
        return@withContext res
    }

    override suspend fun createArchive(
        sourcePaths: List<String>,
        destFile: File,
        format: ArchiveType,
        level: CompressionLevel
    ): Result<File> = withContext(Dispatchers.IO) {
        val files = sourcePaths.map { File(it) }
        val res = when (format) {
            ArchiveType.TAR -> ArchiveManager.createTarArchive(files, destFile)
            else -> ArchiveManager.createZipArchive(files, destFile, level)
        }
        if (res.isSuccess) {
            loadDirectory(_directoryState.value.currentPath)
            refresh(_homeScreenData.value.hasPermission)
        }
        return@withContext res
    }

    override suspend fun testArchiveIntegrity(archivePath: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext ArchiveManager.testArchiveIntegrity(File(archivePath))
    }

    override suspend fun extractAndPrepareArchiveFile(virtualPath: String): Result<File> = withContext(Dispatchers.IO) {
        if (!virtualPath.contains("::")) {
            return@withContext Result.failure(IllegalArgumentException("Not an archive item"))
        }
        val archivePath = virtualPath.substringBefore("::")
        val inner = virtualPath.substringAfter("::")
        val archiveFile = File(archivePath)
        val safeName = File(inner).name.ifEmpty { "item" }
        val tempDir = File(context.cacheDir, "archive_preview")
        val tempOut = File(tempDir, safeName)
        return@withContext ArchiveManager.extractSingleFile(archiveFile, inner, tempOut)
    }

    override suspend fun getInstalledApps(includeSystem: Boolean): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        return@withContext AppManager.getInstalledApps(context, includeSystem)
    }

    override suspend fun extractAppApk(app: InstalledAppItem): Result<File> = withContext(Dispatchers.IO) {
        val res = AppManager.extractApk(app)
        if (res.isSuccess) {
            refresh(_homeScreenData.value.hasPermission)
        }
        return@withContext res
    }

    private fun buildBreadcrumbs(path: String, root: String): List<BreadcrumbSegment> {
        val list = mutableListOf<BreadcrumbSegment>()
        list.add(BreadcrumbSegment(activeVolumeName, root))

        if (path == root) return list

        val relative = path.removePrefix(root).trimStart(File.separatorChar)
        if (relative.isNotEmpty()) {
            val parts = relative.split(File.separatorChar)
            var accumulated = root
            for (part in parts) {
                accumulated = "$accumulated${File.separator}$part"
                list.add(BreadcrumbSegment(part, accumulated))
            }
        }
        return list
    }

    private fun buildArchiveBreadcrumbs(archiveFile: File, innerPath: String, root: String): List<BreadcrumbSegment> {
        val list = mutableListOf<BreadcrumbSegment>()
        val parentDir = archiveFile.parentFile?.absolutePath ?: root
        list.addAll(buildBreadcrumbs(parentDir, root))
        list.add(BreadcrumbSegment(archiveFile.name, archiveFile.absolutePath))

        val cleanInner = innerPath.trim('/')
        if (cleanInner.isNotEmpty()) {
            val parts = cleanInner.split('/')
            var accumulated = ""
            for (part in parts) {
                accumulated = if (accumulated.isEmpty()) part else "$accumulated/$part"
                list.add(BreadcrumbSegment(part, "${archiveFile.absolutePath}::$accumulated"))
            }
        }
        return list
    }
}
