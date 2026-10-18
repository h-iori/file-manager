package com.ioristudios.filemanager.data

import androidx.compose.runtime.Immutable

enum class DirectoryVisualType {
    // App Special Folders
    FOLDER_WHATSAPP,
    FOLDER_TELEGRAM,
    FOLDER_INSTAGRAM,
    FOLDER_FACEBOOK,
    FOLDER_TWITTER,
    FOLDER_SNAPCHAT,
    FOLDER_TIKTOK,
    FOLDER_YOUTUBE,
    FOLDER_DISCORD,
    FOLDER_SPOTIFY,
    FOLDER_REDDIT,
    FOLDER_NETFLIX,
    FOLDER_GITHUB,
    FOLDER_GOOGLE_DRIVE,
    FOLDER_BLUETOOTH,
    FOLDER_SCREENSHOTS,
    FOLDER_CAMERA,
    FOLDER_DOCUMENTS,
    FOLDER_DOWNLOADS,
    FOLDER_MOVIES,
    FOLDER_MUSIC,
    FOLDER_PICTURES,
    FOLDER_PODCASTS,
    FOLDER_RECORDINGS,
    FOLDER_CODE,
    FOLDER_ARCHIVES,
    FOLDER_ANDROID,
    FOLDER_PROTECTED,
    FOLDER,

    // File Types
    FILE_PDF,
    FILE_WORD,
    FILE_EXCEL,
    FILE_POWERPOINT,
    FILE_TEXT,
    FILE_CODE,
    FILE_ARCHIVE,
    FILE_APK,
    FILE_IMAGE,
    FILE_VIDEO,
    FILE_AUDIO,
    FILE_DATABASE,
    FILE_FONT,
    FILE_TORRENT,
    FILE_SYSTEM,
    FILE_GENERIC,

    // Legacy backwards compatibility aliases
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
    ANDROID
}

enum class SortOption(val label: String) {
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    DATE_DESC("Date (Newest)"),
    DATE_ASC("Date (Oldest)"),
    SIZE_DESC("Size (Largest)"),
    SIZE_ASC("Size (Smallest)")
}

@Immutable
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
    val mimeType: String = "",
    val isProtected: Boolean = false,
    val protectionLevel: ProtectionLevel = ProtectionLevel.NONE,
    val protectionReason: String = ""
)

@Immutable
data class BreadcrumbSegment(
    val title: String,
    val fullPath: String
)

@Immutable
data class DirectoryScreenState(
    val currentPath: String = "",
    val rootPath: String = "",
    val items: List<DirectoryItem> = emptyList(),
    val breadcrumbs: List<BreadcrumbSegment> = emptyList(),
    val sortOption: SortOption = SortOption.NAME_ASC,
    val isLoading: Boolean = false,
    val protectionInfo: DirectoryProtectionInfo? = null
)
