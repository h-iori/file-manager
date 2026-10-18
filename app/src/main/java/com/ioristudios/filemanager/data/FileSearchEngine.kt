package com.ioristudios.filemanager.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FileSearchEngine(private val context: Context) {

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val indexMutex = Mutex()

    // Thread-safe immutable list reference
    @Volatile
    private var cachedIndex: List<SearchResultItem> = emptyList()
    @Volatile
    private var lastIndexedRoot: String = ""
    @Volatile
    private var lastIndexTimestamp: Long = 0L

    suspend fun search(
        query: String,
        rootPath: String,
        category: SearchFilterCategory = SearchFilterCategory.ALL,
        showHidden: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _searchState.value = _searchState.value.copy(
                query = "",
                results = emptyList(),
                categoryCounts = emptyMap(),
                isSearching = false
            )
            return@withContext
        }

        _searchState.value = _searchState.value.copy(
            query = trimmed,
            selectedCategory = category,
            isSearching = true
        )

        android.util.Log.i("FileSearchEngine", "search: query='$trimmed' root='$rootPath' cat=$category")
        // Ensure index for rootPath is fresh (cached up to 3 minutes)
        val now = System.currentTimeMillis()
        if (lastIndexedRoot != rootPath || now - lastIndexTimestamp > 180_000L || cachedIndex.isEmpty()) {
            buildIndex(rootPath, showHidden)
        }

        val snapshot = cachedIndex
        val allMatched = filterIndex(trimmed, snapshot)
        android.util.Log.i("FileSearchEngine", "search: indexSize=${snapshot.size} allMatched=${allMatched.size}")

        // Calculate dynamic category counts
        val counts = mutableMapOf<SearchFilterCategory, Int>()
        counts[SearchFilterCategory.ALL] = allMatched.size
        counts[SearchFilterCategory.DOCUMENTS] = allMatched.count { it.category == FileCategory.DOCUMENT || it.category == FileCategory.SPREADSHEET }
        counts[SearchFilterCategory.IMAGES] = allMatched.count { it.category == FileCategory.IMAGE }
        counts[SearchFilterCategory.AUDIO] = allMatched.count { it.category == FileCategory.AUDIO }
        counts[SearchFilterCategory.FOLDERS] = allMatched.count { it.isDirectory }

        // Filter by currently selected category
        val filtered = when (category) {
            SearchFilterCategory.ALL -> allMatched
            SearchFilterCategory.DOCUMENTS -> allMatched.filter { it.category == FileCategory.DOCUMENT || it.category == FileCategory.SPREADSHEET }
            SearchFilterCategory.IMAGES -> allMatched.filter { it.category == FileCategory.IMAGE }
            SearchFilterCategory.AUDIO -> allMatched.filter { it.category == FileCategory.AUDIO }
            SearchFilterCategory.FOLDERS -> allMatched.filter { it.isDirectory }
        }

        _searchState.value = _searchState.value.copy(
            results = filtered,
            categoryCounts = counts,
            isSearching = false
        )
    }

    fun setFilterCategory(category: SearchFilterCategory) {
        val currentQuery = _searchState.value.query
        if (currentQuery.isBlank()) {
            _searchState.value = _searchState.value.copy(selectedCategory = category)
            return
        }

        val snapshot = cachedIndex
        val allMatched = filterIndex(currentQuery, snapshot)
        val filtered = when (category) {
            SearchFilterCategory.ALL -> allMatched
            SearchFilterCategory.DOCUMENTS -> allMatched.filter { it.category == FileCategory.DOCUMENT || it.category == FileCategory.SPREADSHEET }
            SearchFilterCategory.IMAGES -> allMatched.filter { it.category == FileCategory.IMAGE }
            SearchFilterCategory.AUDIO -> allMatched.filter { it.category == FileCategory.AUDIO }
            SearchFilterCategory.FOLDERS -> allMatched.filter { it.isDirectory }
        }

        _searchState.value = _searchState.value.copy(
            selectedCategory = category,
            results = filtered
        )
    }

    fun clearSearch() {
        _searchState.value = _searchState.value.copy(
            query = "",
            results = emptyList(),
            categoryCounts = emptyMap(),
            isSearching = false
        )
    }

    private suspend fun buildIndex(rootPath: String, showHidden: Boolean) {
        indexMutex.withLock {
            val now = System.currentTimeMillis()
            if (lastIndexedRoot == rootPath && now - lastIndexTimestamp <= 180_000L && cachedIndex.isNotEmpty()) {
                return
            }

            val root = File(rootPath)
            if (!root.exists() || !root.isDirectory) return

            val items = ArrayList<SearchResultItem>()
            val stack = ArrayDeque<File>()
            stack.add(root)

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            var count = 0

            while (stack.isNotEmpty() && count < 35_000) {
                val current = stack.removeFirst()
                val children = current.listFiles() ?: continue

                for (file in children) {
                    if (!showHidden && file.name.startsWith(".")) continue

                    val isDir = file.isDirectory
                    val sizeBytes = if (isDir) 0L else file.length()
                    val parentName = file.parentFile?.name ?: ""
                    val category = determineCategory(file)

                    items.add(
                        SearchResultItem(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = isDir,
                            sizeBytes = sizeBytes,
                            formattedSize = formatBytes(sizeBytes),
                            dateModifiedMs = file.lastModified(),
                            formattedDate = dateFormat.format(Date(file.lastModified())),
                            parentFolder = parentName,
                            mimeType = getMimeType(file),
                            category = category
                        )
                    )
                    count++

                    if (isDir) {
                        stack.add(file)
                    }
                }
            }

            cachedIndex = items.toList()
            lastIndexedRoot = rootPath
            lastIndexTimestamp = System.currentTimeMillis()
            android.util.Log.i("FileSearchEngine", "buildIndex finished: root='$rootPath' indexed ${items.size} items")
        }
    }

    private fun filterIndex(query: String, index: List<SearchResultItem>): List<SearchResultItem> {
        val lowerQuery = query.lowercase(Locale.ROOT)

        // Score results: exact match = 100, prefix match = 80, word boundary = 60, substring = 40
        return index.mapNotNull { item ->
            val lowerName = item.name.lowercase(Locale.ROOT)
            val score = when {
                lowerName == lowerQuery -> 100
                lowerName.startsWith(lowerQuery) -> 80
                lowerName.contains(" $lowerQuery") || lowerName.contains("_$lowerQuery") || lowerName.contains("-$lowerQuery") -> 60
                lowerName.contains(lowerQuery) -> 40
                else -> -1
            }

            if (score > 0) Pair(item, score) else null
        }
            .sortedWith(compareByDescending<Pair<SearchResultItem, Int>> { it.second }.thenByDescending { it.first.dateModifiedMs })
            .map { it.first }
    }

    private fun determineCategory(file: File): FileCategory {
        if (file.isDirectory) return FileCategory.GENERIC
        val ext = file.extension.lowercase(Locale.ROOT)
        return when (ext) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg", "heic", "dng" -> FileCategory.IMAGE
            "mp4", "mkv", "webm", "avi", "3gp", "mov", "flv" -> FileCategory.VIDEO
            "mp3", "wav", "flac", "aac", "m4a", "ogg", "opus" -> FileCategory.AUDIO
            "pdf", "doc", "docx", "txt", "rtf", "epub", "wps", "wpd", "md", "json", "xml", "log" -> FileCategory.DOCUMENT
            "xls", "xlsx", "csv", "ods" -> FileCategory.SPREADSHEET
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst", "iso" -> FileCategory.ARCHIVE
            else -> FileCategory.GENERIC
        }
    }

    private fun getMimeType(file: File): String {
        val ext = file.extension.lowercase(Locale.ROOT)
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
