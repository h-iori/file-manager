package com.ioristudios.filemanager.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
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

    // Global and scoped index caches
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
        val effectiveRoot = _searchState.value.scopeDirectory ?: rootPath

        // Ensure index for effectiveRoot is fresh (cached up to 3 minutes)
        val now = System.currentTimeMillis()
        if (lastIndexedRoot != effectiveRoot || now - lastIndexTimestamp > 180_000L || cachedIndex.isEmpty()) {
            buildIndex(effectiveRoot, showHidden)
        }

        val snapshot = cachedIndex

        if (trimmed.isEmpty()) {
            if (category == SearchFilterCategory.ALL) {
                _searchState.value = _searchState.value.copy(
                    query = "",
                    selectedCategory = SearchFilterCategory.ALL,
                    results = emptyList(),
                    categoryCounts = emptyMap(),
                    isSearching = false
                )
                return@withContext
            } else {
                // When query is blank but a category is selected (e.g. from Home category clicks)
                val filtered = filterByCategory(snapshot, category)
                val counts = calculateCategoryCounts(snapshot)
                _searchState.value = _searchState.value.copy(
                    query = "",
                    selectedCategory = category,
                    results = filtered,
                    categoryCounts = counts,
                    isSearching = false
                )
                return@withContext
            }
        }

        _searchState.value = _searchState.value.copy(
            query = trimmed,
            selectedCategory = category,
            isSearching = true
        )

        android.util.Log.i("FileSearchEngine", "search: query='$trimmed' root='$effectiveRoot' cat=$category")

        val allMatched = filterIndex(trimmed, snapshot)
        val counts = calculateCategoryCounts(allMatched)
        val filtered = filterByCategory(allMatched, category)

        _searchState.value = _searchState.value.copy(
            results = filtered,
            categoryCounts = counts,
            isSearching = false
        )
    }

    fun setFilterCategory(category: SearchFilterCategory) {
        val currentQuery = _searchState.value.query.trim()
        val snapshot = cachedIndex

        if (currentQuery.isBlank()) {
            if (category == SearchFilterCategory.ALL) {
                _searchState.value = _searchState.value.copy(
                    selectedCategory = category,
                    results = emptyList()
                )
            } else {
                val filtered = filterByCategory(snapshot, category)
                val counts = calculateCategoryCounts(snapshot)
                _searchState.value = _searchState.value.copy(
                    selectedCategory = category,
                    results = filtered,
                    categoryCounts = counts
                )
            }
            return
        }

        val allMatched = filterIndex(currentQuery, snapshot)
        val filtered = filterByCategory(allMatched, category)
        val counts = calculateCategoryCounts(allMatched)

        _searchState.value = _searchState.value.copy(
            selectedCategory = category,
            results = filtered,
            categoryCounts = counts
        )
    }

    fun setScope(scopePath: String?, scopeDisplayName: String?) {
        _searchState.value = _searchState.value.copy(
            scopeDirectory = scopePath,
            scopeDisplayName = scopeDisplayName
        )
    }

    fun clearScope() {
        _searchState.value = _searchState.value.copy(
            scopeDirectory = null,
            scopeDisplayName = null
        )
    }

    fun clearSearch() {
        _searchState.value = _searchState.value.copy(
            query = "",
            selectedCategory = SearchFilterCategory.ALL,
            results = emptyList(),
            categoryCounts = emptyMap(),
            isSearching = false
        )
    }

    private fun filterByCategory(
        items: List<SearchResultItem>,
        category: SearchFilterCategory
    ): List<SearchResultItem> {
        return when (category) {
            SearchFilterCategory.ALL -> items
            SearchFilterCategory.DOCUMENTS -> items.filter {
                !it.isDirectory && (it.category == FileCategory.DOCUMENT || it.category == FileCategory.SPREADSHEET)
            }
            SearchFilterCategory.IMAGES -> items.filter {
                !it.isDirectory && it.category == FileCategory.IMAGE
            }
            SearchFilterCategory.VIDEOS -> items.filter {
                !it.isDirectory && it.category == FileCategory.VIDEO
            }
            SearchFilterCategory.AUDIO -> items.filter {
                !it.isDirectory && it.category == FileCategory.AUDIO
            }
            SearchFilterCategory.ARCHIVES -> items.filter {
                !it.isDirectory && it.category == FileCategory.ARCHIVE
            }
            SearchFilterCategory.APKS -> items.filter {
                !it.isDirectory && (it.name.endsWith(".apk", ignoreCase = true) ||
                        it.name.endsWith(".xapk", ignoreCase = true) ||
                        it.name.endsWith(".apks", ignoreCase = true))
            }
            SearchFilterCategory.FOLDERS -> items.filter { it.isDirectory }
        }
    }

    private fun calculateCategoryCounts(items: List<SearchResultItem>): Map<SearchFilterCategory, Int> {
        val counts = mutableMapOf<SearchFilterCategory, Int>()
        counts[SearchFilterCategory.ALL] = items.size
        counts[SearchFilterCategory.DOCUMENTS] = items.count {
            !it.isDirectory && (it.category == FileCategory.DOCUMENT || it.category == FileCategory.SPREADSHEET)
        }
        counts[SearchFilterCategory.IMAGES] = items.count {
            !it.isDirectory && it.category == FileCategory.IMAGE
        }
        counts[SearchFilterCategory.VIDEOS] = items.count {
            !it.isDirectory && it.category == FileCategory.VIDEO
        }
        counts[SearchFilterCategory.AUDIO] = items.count {
            !it.isDirectory && it.category == FileCategory.AUDIO
        }
        counts[SearchFilterCategory.ARCHIVES] = items.count {
            !it.isDirectory && it.category == FileCategory.ARCHIVE
        }
        counts[SearchFilterCategory.APKS] = items.count {
            !it.isDirectory && (it.name.endsWith(".apk", ignoreCase = true) ||
                    it.name.endsWith(".xapk", ignoreCase = true) ||
                    it.name.endsWith(".apks", ignoreCase = true))
        }
        counts[SearchFilterCategory.FOLDERS] = items.count { it.isDirectory }
        return counts
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
        val lowerQuery = query.trim().lowercase(Locale.ROOT)
        if (lowerQuery.isEmpty()) return emptyList()

        val isExplicitExtension = lowerQuery.startsWith(".") || lowerQuery.startsWith("*.")
        val targetExtension = when {
            lowerQuery.startsWith("*.") -> lowerQuery.removePrefix("*.")
            lowerQuery.startsWith(".") -> lowerQuery.removePrefix(".")
            else -> lowerQuery
        }

        val isKnownExtension = targetExtension in setOf(
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso", "zst",
            "pdf", "doc", "docx", "txt", "rtf", "epub", "md", "json", "xml", "log", "csv", "xlsx", "xls",
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg", "heic", "dng",
            "mp4", "mkv", "webm", "avi", "3gp", "mov", "flv", "wmv",
            "mp3", "wav", "flac", "aac", "m4a", "ogg", "opus",
            "apk", "xapk", "apks"
        )

        return index.mapNotNull { item ->
            val lowerName = item.name.lowercase(Locale.ROOT)
            val itemExt = if (item.isDirectory) "" else File(item.name).extension.lowercase(Locale.ROOT)

            var score = -1

            // 1. Explicit extension match: e.g. user typed ".zip" or "*.zip"
            if (isExplicitExtension && targetExtension.isNotEmpty()) {
                if (itemExt == targetExtension) {
                    score = 98
                } else if (lowerName.endsWith(".$targetExtension")) {
                    score = 95
                }
            } else if (isKnownExtension && itemExt == targetExtension) {
                // User typed e.g. "zip" or "pdf" without dot -> all matching extensions get high score
                score = 92
            }

            // 2. Exact match
            if (lowerName == lowerQuery) {
                score = maxOf(score, 100)
            }
            // 3. Exact name without extension
            val nameWithoutExt = lowerName.substringBeforeLast('.', lowerName)
            if (nameWithoutExt == lowerQuery) {
                score = maxOf(score, 99)
            }
            // 4. Prefix match
            if (lowerName.startsWith(lowerQuery)) {
                score = maxOf(score, 85)
            }
            // 5. Word boundary match
            if (lowerName.contains(" $lowerQuery") || lowerName.contains("_$lowerQuery") || lowerName.contains("-$lowerQuery")) {
                score = maxOf(score, 70)
            }
            // 6. General substring match
            if (lowerName.contains(lowerQuery)) {
                score = maxOf(score, 50)
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
            "mp4", "mkv", "webm", "avi", "3gp", "mov", "flv", "wmv" -> FileCategory.VIDEO
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
