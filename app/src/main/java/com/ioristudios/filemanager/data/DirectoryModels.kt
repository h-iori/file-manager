package com.ioristudios.filemanager.data

enum class DirectoryVisualType {
    CAMERA,
    DOWNLOAD,
    DOCUMENT,
    MOVIE,
    HEADPHONES,
    IMAGE,
    SEND,
    MIC,
    BACKUP,
    PODCASTS,
    NOTIFICATIONS,
    ANDROID,
    FOLDER,
    FILE_GENERIC
}

enum class SortOption(val label: String) {
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    DATE_DESC("Date (Newest)"),
    DATE_ASC("Date (Oldest)"),
    SIZE_DESC("Size (Largest)"),
    SIZE_ASC("Size (Smallest)")
}

data class DirectoryItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val formattedSize: String,
    val childCount: Int,
    val fileCountLabel: String,
    val dateModifiedMs: Long,
    val formattedDate: String,
    val visualType: DirectoryVisualType,
    val mimeType: String = ""
)

data class BreadcrumbSegment(
    val title: String,
    val fullPath: String
)

data class DirectoryScreenState(
    val currentPath: String = "",
    val rootPath: String = "",
    val items: List<DirectoryItem> = emptyList(),
    val breadcrumbs: List<BreadcrumbSegment> = emptyList(),
    val sortOption: SortOption = SortOption.NAME_ASC,
    val isLoading: Boolean = false
)
