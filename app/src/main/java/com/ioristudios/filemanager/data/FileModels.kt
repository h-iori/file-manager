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
    val categories: List<CategoryItem> = emptyList(),
    val recentFiles: List<RecentFile> = emptyList(),
    val hasPermission: Boolean = false,
    val reclaimableFormatted: String = "2.4 GB"
)
