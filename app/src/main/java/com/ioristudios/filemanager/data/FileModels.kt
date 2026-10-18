package com.ioristudios.filemanager.data

import android.net.Uri

enum class FileCategory {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    ARCHIVE,
    SPREADSHEET,
    GENERIC,
    SYSTEM
}

enum class StorageVolumeType {
    INTERNAL,
    SD_CARD,
    USB_OTG
}

data class StorageVolumeInfo(
    val id: String,
    val name: String,
    val path: String,
    val type: StorageVolumeType,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val formattedTotal: String,
    val formattedFree: String,
    val formattedUsed: String,
    val usedPercent: Float,
    val isMounted: Boolean = true
)

data class StorageOverview(
    val totalBytes: Long = 128L * 1024 * 1024 * 1024,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedPercent: Float = 0f,
    val formattedTotal: String = "128 GB",
    val formattedUsed: String = "0 GB",
    val formattedFree: String = "128 GB",
    val photosBytes: Long = 0L,
    val videosBytes: Long = 0L,
    val documentsBytes: Long = 0L,
    val audioBytes: Long = 0L,
    val downloadsBytes: Long = 0L,
    val appsBytes: Long = 0L,
    val systemBytes: Long = 0L
)

data class CategoryItem(
    val title: String,
    val count: Int,
    val bytes: Long,
    val formattedSize: String,
    val fileCountLabel: String,
    val progressFraction: Float,
    val category: FileCategory
)

data class RecentFile(
    val id: Long,
    val name: String,
    val uri: Uri,
    val path: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val dateModifiedMs: Long,
    val formattedDate: String,
    val mimeType: String,
    val category: FileCategory
)

data class HomeScreenData(
    val storage: StorageOverview = StorageOverview(),
    val availableVolumes: List<StorageVolumeInfo> = emptyList(),
    val selectedVolumeId: String = "internal",
    val categories: List<CategoryItem> = emptyList(),
    val recentFiles: List<RecentFile> = emptyList(),
    val hasPermission: Boolean = false,
    val reclaimableFormatted: String = "2.4 GB"
)

enum class ClipboardMode {
    COPY,
    CUT
}

data class ClipboardState(
    val items: List<String> = emptyList(),
    val mode: ClipboardMode = ClipboardMode.COPY,
    val sourceDirectory: String = "",
    val isActive: Boolean = false
)

enum class ConflictResolution {
    SKIP,
    OVERWRITE,
    AUTO_RENAME
}

data class PasteResult(
    val successCount: Int = 0,
    val skippedCount: Int = 0,
    val errorCount: Int = 0,
    val message: String = ""
)

enum class SearchFilterCategory(val label: String) {
    ALL("All"),
    DOCUMENTS("Documents"),
    IMAGES("Images"),
    AUDIO("Audio"),
    FOLDERS("Folders")
}

data class SearchResultItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val formattedSize: String,
    val dateModifiedMs: Long,
    val formattedDate: String,
    val parentFolder: String,
    val mimeType: String,
    val category: FileCategory
)

data class SearchUiState(
    val query: String = "",
    val selectedCategory: SearchFilterCategory = SearchFilterCategory.ALL,
    val results: List<SearchResultItem> = emptyList(),
    val categoryCounts: Map<SearchFilterCategory, Int> = emptyMap(),
    val isSearching: Boolean = false,
    val recentQueries: List<String> = emptyList()
)
