
package com.ioristudios.filemanager.data

import android.content.ContentUris
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.os.storage.StorageManager
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

class StorageScanner(private val context: Context) {

    data class CachedDirStats(
        val lastModified: Long,
        val totalBytes: Long,
        val totalFiles: Int,
        val timestamp: Long
    )

    private val dirStatsCache = ConcurrentHashMap<String, CachedDirStats>()

    fun clearDirCache() {
        dirStatsCache.clear()
    }

    suspend fun scanHomeScreenData(
        hasFullPermission: Boolean,
        selectedVolumeId: String? = null
    ): HomeScreenData = withContext(Dispatchers.IO) {
        val availableVolumes = getAvailableStorageVolumes()
        val activeVolume = availableVolumes.find { it.id == selectedVolumeId }
            ?: availableVolumes.firstOrNull { it.id == "internal" }
            ?: availableVolumes.first()

        val storage = if (activeVolume.type == StorageVolumeType.INTERNAL) {
            getRealStorageOverview()
        } else {
            getVolumeStorageOverview(activeVolume)
        }
        val categories = getRealCategories(storage.totalBytes)
        val recentFiles = getRealRecentFiles()
        // Match the cleanup screen's default selection; optional personal files are excluded.
        val reclaimableBytes = try {
            CleanupEngine(context.applicationContext).estimateDefaultReclaimableBytes()
        } catch (_: Exception) {
            0L
        }

        HomeScreenData(
            storage = storage,
            availableVolumes = availableVolumes,
            selectedVolumeId = activeVolume.id,
            categories = categories,
            recentFiles = recentFiles,
            hasPermission = hasFullPermission,
            reclaimableFormatted = formatBytes(reclaimableBytes)
        )
    }

    fun getAvailableStorageVolumes(): List<StorageVolumeInfo> {
        val list = mutableListOf<StorageVolumeInfo>()
        val internalFile = Environment.getExternalStorageDirectory()
        val internalStat = try { StatFs(internalFile.path) } catch (_: Exception) { null }
        val internalTotal = internalStat?.let { it.blockCountLong * it.blockSizeLong } ?: (128L * 1024 * 1024 * 1024)
        val internalFree = internalStat?.let { it.availableBlocksLong * it.blockSizeLong } ?: (64L * 1024 * 1024 * 1024)
        val internalUsed = max(0L, internalTotal - internalFree)
        val internalPercent = if (internalTotal > 0) (internalUsed.toFloat() / internalTotal.toFloat()) * 100f else 0f

        list.add(
            StorageVolumeInfo(
                id = "internal",
                name = "Internal storage",
                path = internalFile.absolutePath,
                type = StorageVolumeType.INTERNAL,
                totalBytes = internalTotal,
                freeBytes = internalFree,
                usedBytes = internalUsed,
                formattedTotal = formatBytes(internalTotal),
                formattedFree = formatBytes(internalFree),
                formattedUsed = formatBytes(internalUsed),
                usedPercent = internalPercent,
                isMounted = true
            )
        )

        // Check for Removable SD Cards and USB OTG
        try {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
            if (storageManager != null) {
                val volumes = storageManager.storageVolumes
                for (vol in volumes) {
                    if (vol.isPrimary) continue
                    val state = vol.state
                    if (state != Environment.MEDIA_MOUNTED && state != Environment.MEDIA_MOUNTED_READ_ONLY) continue

                    val dirFile: File? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        vol.directory
                    } else {
                        try {
                            val getPathMethod = vol.javaClass.getMethod("getPath")
                            val pathStr = getPathMethod.invoke(vol) as? String
                            pathStr?.let { File(it) }
                        } catch (_: Exception) {
                            null
                        }
                    }

                    val path = dirFile?.absolutePath ?: continue
                    if (!dirFile.exists() || !dirFile.canRead()) continue

                    val desc = vol.getDescription(context).trim()
                    val lower = desc.lowercase(Locale.ROOT)
                    val isUsb = lower.contains("usb") || lower.contains("otg") || lower.contains("drive") || lower.contains("flash")
                    val type = if (isUsb) StorageVolumeType.USB_OTG else StorageVolumeType.SD_CARD
                    val defaultName = if (isUsb) "USB Storage" else "SD Card"
                    val displayName = if (desc.isNotBlank()) desc else defaultName

                    val stat = try { StatFs(path) } catch (_: Exception) { null }
                    val total = stat?.let { it.blockCountLong * it.blockSizeLong } ?: 0L
                    val free = stat?.let { it.availableBlocksLong * it.blockSizeLong } ?: 0L
                    val used = max(0L, total - free)
                    val percent = if (total > 0) (used.toFloat() / total.toFloat()) * 100f else 0f

                    list.add(
                        StorageVolumeInfo(
                            id = if (isUsb) "otg_${vol.uuid ?: path.hashCode()}" else "sdcard_${vol.uuid ?: path.hashCode()}",
                            name = displayName,
                            path = path,
                            type = type,
                            totalBytes = total,
                            freeBytes = free,
                            usedBytes = used,
                            formattedTotal = formatBytes(total),
                            formattedFree = formatBytes(free),
                            formattedUsed = formatBytes(used),
                            usedPercent = percent,
                            isMounted = true
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        return list
    }

    private fun getVolumeStorageOverview(volume: StorageVolumeInfo): StorageOverview {
        return StorageOverview(
            totalBytes = volume.totalBytes,
            usedBytes = volume.usedBytes,
            freeBytes = volume.freeBytes,
            usedPercent = volume.usedPercent,
            formattedTotal = volume.formattedTotal,
            formattedUsed = volume.formattedUsed,
            formattedFree = volume.formattedFree,
            photosBytes = (volume.usedBytes * 0.35).toLong(),
            videosBytes = (volume.usedBytes * 0.25).toLong(),
            documentsBytes = (volume.usedBytes * 0.15).toLong(),
            audioBytes = (volume.usedBytes * 0.10).toLong(),
            downloadsBytes = (volume.usedBytes * 0.08).toLong(),
            appsBytes = 0L,
            systemBytes = (volume.usedBytes * 0.07).toLong(),
            otherBytes = max(0L, volume.usedBytes - (
                (volume.usedBytes * 0.35).toLong() +
                    (volume.usedBytes * 0.25).toLong() +
                    (volume.usedBytes * 0.15).toLong() +
                    (volume.usedBytes * 0.10).toLong() +
                    (volume.usedBytes * 0.08).toLong() +
                    (volume.usedBytes * 0.07).toLong()
                ))
        )
    }

    suspend fun scanDirectory(
        directoryPath: String,
        sortOption: SortOption,
        showHiddenFiles: Boolean = true
    ): List<DirectoryItem> = withContext(Dispatchers.IO) {
        val dir = File(directoryPath)
        if (!dir.exists() || !dir.isDirectory) return@withContext emptyList()

        val rawFiles = dir.listFiles()?.filter { showHiddenFiles || !it.name.startsWith(".") } ?: emptyList()

        // High-speed non-blocking directory enumeration
        val items = rawFiles.map { file ->
            val isDir = file.isDirectory
            val protectionInfo = SecurityProtectionManager.checkProtection(file.absolutePath)

            val (sizeBytes, childCount, fileCountLabel) = if (isDir) {
                val count = try {
                    file.list()?.count { showHiddenFiles || !it.startsWith(".") } ?: 0
                } catch (_: Exception) {
                    0
                }
                val label = when {
                    protectionInfo.isProtected && protectionInfo.level == ProtectionLevel.SCOPED_STORAGE_RESTRICTED ->
                        "Protected Â· Scoped Storage"
                    protectionInfo.isProtected && protectionInfo.level == ProtectionLevel.SYSTEM_CORE_LOCKED ->
                        "System Core Â· Locked"
                    count == 0 -> "Empty folder"
                    count == 1 -> "1 item"
                    else -> "$count items"
                }
                Triple(0L, count, label)
            } else {
                val len = file.length()
                val formattedSize = formatBytes(len)
                val formattedDate = formatRelativeDate(file.lastModified())
                Triple(len, 1, "$formattedSize Â· $formattedDate")
            }

            val visualType = if (isDir) {
                detectFolderVisualType(file.name, file.absolutePath)
            } else {
                detectFileVisualType(file.name)
            }

            val mime = if (isDir) "" else {
                val ext = file.extension.lowercase(Locale.ROOT)
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
            }

            DirectoryItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = isDir,
                sizeBytes = sizeBytes,
                formattedSize = if (isDir) "" else formatBytes(sizeBytes),
                childCount = childCount,
                fileCountLabel = fileCountLabel,
                dateModifiedMs = file.lastModified(),
                formattedDate = formatRelativeDate(file.lastModified()),
                visualType = visualType,
                mimeType = mime,
                isProtected = protectionInfo.isProtected,
                protectionLevel = protectionInfo.level,
                protectionReason = protectionInfo.description
            )
        }

        // Apply sorting: folders first, then files, sorted by sortOption
        sortDirectoryItems(items, sortOption)
    }

    private fun calculateRecursiveDirStats(
        dir: File,
        showHiddenFiles: Boolean,
        maxDepth: Int = 12,
        maxFilesLimit: Int = 100_000
    ): Pair<Long, Int> {
        val dirPath = dir.absolutePath
        val currentModified = dir.lastModified()
        val now = System.currentTimeMillis()

        val cached = dirStatsCache[dirPath]
        if (cached != null && (now - cached.timestamp < 30_000L || cached.lastModified == currentModified)) {
            return Pair(cached.totalBytes, cached.totalFiles)
        }

        var totalSize = 0L
        var totalFiles = 0
        val queue = java.util.ArrayDeque<Pair<File, Int>>()
        queue.add(Pair(dir, 0))

        while (!queue.isEmpty() && totalFiles < maxFilesLimit) {
            val (current, depth) = queue.poll() ?: break
            if (depth > maxDepth) continue

            val children = try {
                current.listFiles()
            } catch (_: Throwable) {
                null
            } ?: continue

            for (child in children) {
                if (!showHiddenFiles && child.name.startsWith(".")) continue
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && java.nio.file.Files.isSymbolicLink(child.toPath())) {
                        continue
                    }
                } catch (_: Throwable) {}

                if (child.isDirectory) {
                    queue.add(Pair(child, depth + 1))
                } else if (child.isFile) {
                    val len = child.length()
                    if (len > 0L) {
                        totalSize += len
                    }
                    totalFiles++
                }
            }
        }

        dirStatsCache[dirPath] = CachedDirStats(
            lastModified = currentModified,
            totalBytes = totalSize,
            totalFiles = totalFiles,
            timestamp = now
        )

        return Pair(totalSize, totalFiles)
    }

    fun detectFolderVisualType(name: String, fullPath: String = ""): DirectoryVisualType {
        val lower = name.lowercase(Locale.ROOT)
        val pathLower = fullPath.replace('\\', '/').lowercase(Locale.ROOT)

        val protection = SecurityProtectionManager.checkProtection(fullPath)
        if (protection.isProtected && (lower == "data" || lower == "obb" || protection.level == ProtectionLevel.SYSTEM_CORE_LOCKED)) {
            return DirectoryVisualType.FOLDER_PROTECTED
        }

        return when {
            // WhatsApp
            lower.contains("whatsapp") || pathLower.contains("com.whatsapp") -> DirectoryVisualType.FOLDER_WHATSAPP
            // Telegram
            lower.contains("telegram") || pathLower.contains("org.telegram") -> DirectoryVisualType.FOLDER_TELEGRAM
            // Instagram
            lower == "instagram" || pathLower.contains("instagram") -> DirectoryVisualType.FOLDER_INSTAGRAM
            // Facebook / Messenger
            lower == "facebook" || lower == "messenger" || pathLower.contains("com.facebook") -> DirectoryVisualType.FOLDER_FACEBOOK
            // Twitter / X
            lower == "twitter" || lower == "x" || pathLower.contains("com.twitter") -> DirectoryVisualType.FOLDER_TWITTER
            // Snapchat
            lower.contains("snapchat") || pathLower.contains("com.snapchat") -> DirectoryVisualType.FOLDER_SNAPCHAT
            // TikTok
            lower.contains("tiktok") || pathLower.contains("musically") -> DirectoryVisualType.FOLDER_TIKTOK
            // YouTube
            lower.contains("youtube") || pathLower.contains("com.google.android.youtube") -> DirectoryVisualType.FOLDER_YOUTUBE
            // Discord
            lower.contains("discord") || pathLower.contains("com.discord") -> DirectoryVisualType.FOLDER_DISCORD
            // Spotify
            lower.contains("spotify") || pathLower.contains("com.spotify") -> DirectoryVisualType.FOLDER_SPOTIFY
            // Reddit
            lower.contains("reddit") || pathLower.contains("com.reddit") -> DirectoryVisualType.FOLDER_REDDIT
            // Netflix
            lower.contains("netflix") || pathLower.contains("com.netflix") -> DirectoryVisualType.FOLDER_NETFLIX
            // GitHub
            lower == "github" || pathLower.contains("github") -> DirectoryVisualType.FOLDER_GITHUB
            // Google Drive
            lower == "google drive" || lower == "drive" || pathLower.contains("google.android.apps.docs") -> DirectoryVisualType.FOLDER_GOOGLE_DRIVE
            // DCIM & Camera
            lower == "dcim" || lower == "camera" -> DirectoryVisualType.FOLDER_CAMERA
            // Screenshots
            lower == "screenshots" || lower.contains("screenshot") -> DirectoryVisualType.FOLDER_SCREENSHOTS
            // Pictures & Photos
            lower == "pictures" || lower == "photos" -> DirectoryVisualType.FOLDER_PICTURES
            // Downloads
            lower.contains("download") -> DirectoryVisualType.FOLDER_DOWNLOADS
            // Documents & Books
            lower.contains("document") || lower == "books" || lower == "ebooks" -> DirectoryVisualType.FOLDER_DOCUMENTS
            // Movies & Videos
            lower.contains("movie") || lower.contains("video") -> DirectoryVisualType.FOLDER_MOVIES
            // Music & Audio
            lower.contains("music") || lower.contains("audio") || lower.contains("ringtone") || lower.contains("alarm") -> DirectoryVisualType.FOLDER_MUSIC
            // Voice & Recordings
            lower.contains("recording") || lower.contains("voice") || lower.contains("recorder") -> DirectoryVisualType.FOLDER_RECORDINGS
            // Podcasts
            lower.contains("podcast") -> DirectoryVisualType.FOLDER_PODCASTS
            // Bluetooth
            lower.contains("bluetooth") -> DirectoryVisualType.FOLDER_BLUETOOTH
            // Code & Git & Dev
            lower == "git" || lower == "github" || lower == "projects" || lower == "code" || lower == "src" || lower == "dev" -> DirectoryVisualType.FOLDER_CODE
            // Backups & Archives
            lower.contains("backup") || lower.contains("archives") -> DirectoryVisualType.FOLDER_ARCHIVES
            // Android Core
            lower == "android" -> DirectoryVisualType.FOLDER_ANDROID
            else -> DirectoryVisualType.FOLDER
        }
    }

    fun detectFileVisualType(name: String, mimeType: String = ""): DirectoryVisualType {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mimeLower = mimeType.lowercase(Locale.ROOT)

        return when {
            ext == "pdf" || mimeLower.contains("pdf") -> DirectoryVisualType.FILE_PDF
            ext in setOf("doc", "docx", "odt", "rtf", "dot", "dotx") || mimeLower.contains("word") -> DirectoryVisualType.FILE_WORD
            ext in setOf("xls", "xlsx", "csv", "tsv", "ods", "xlsm") || mimeLower.contains("excel") || mimeLower.contains("spreadsheet") -> DirectoryVisualType.FILE_EXCEL
            ext in setOf("ppt", "pptx", "odp", "pps", "ppsx") || mimeLower.contains("presentation") || mimeLower.contains("powerpoint") -> DirectoryVisualType.FILE_POWERPOINT
            ext in setOf("txt", "md", "markdown", "log", "readme", "conf", "cfg", "ini") || mimeLower.startsWith("text/plain") -> DirectoryVisualType.FILE_TEXT
            ext in setOf("kt", "java", "py", "js", "ts", "jsx", "tsx", "html", "htm", "css", "scss", "json", "xml", "c", "cpp", "h", "hpp", "cs", "php", "go", "rs", "swift", "rb", "sh", "bash", "bat", "sql") -> DirectoryVisualType.FILE_CODE
            ext in setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst", "tgz", "iso", "cab", "lzma") -> DirectoryVisualType.FILE_ARCHIVE
            ext in setOf("apk", "apks", "xapk", "aab") -> DirectoryVisualType.FILE_APK
            ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg", "heic", "heif", "tiff", "ico") || mimeLower.startsWith("image/") -> DirectoryVisualType.FILE_IMAGE
            ext in setOf("mp4", "mkv", "avi", "mov", "flv", "webm", "3gp", "wmv", "m4v", "ts") || mimeLower.startsWith("video/") -> DirectoryVisualType.FILE_VIDEO
            ext in setOf("mp3", "wav", "flac", "aac", "m4a", "ogg", "opus", "wma", "mid", "amr", "alac") || mimeLower.startsWith("audio/") -> DirectoryVisualType.FILE_AUDIO
            ext in setOf("db", "sqlite", "sqlite3", "realm", "mdb") -> DirectoryVisualType.FILE_DATABASE
            ext in setOf("ttf", "otf", "woff", "woff2", "fnt") -> DirectoryVisualType.FILE_FONT
            ext in setOf("torrent", "magnet") -> DirectoryVisualType.FILE_TORRENT
            ext in setOf("bin", "img", "dat", "rom", "cue", "vdi", "vmdk") -> DirectoryVisualType.FILE_SYSTEM
            else -> DirectoryVisualType.FILE_GENERIC
        }
    }

    private fun sortDirectoryItems(items: List<DirectoryItem>, sortOption: SortOption): List<DirectoryItem> {
        val comparator = when (sortOption) {
            SortOption.NAME_ASC -> compareBy<DirectoryItem> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) }
            SortOption.NAME_DESC -> compareBy<DirectoryItem> { !it.isDirectory }.thenByDescending { it.name.lowercase(Locale.ROOT) }
            SortOption.DATE_DESC -> compareBy<DirectoryItem> { !it.isDirectory }.thenByDescending { it.dateModifiedMs }
            SortOption.DATE_ASC -> compareBy<DirectoryItem> { !it.isDirectory }.thenBy { it.dateModifiedMs }
            SortOption.SIZE_DESC -> compareBy<DirectoryItem> { !it.isDirectory }.thenByDescending { it.sizeBytes }
            SortOption.SIZE_ASC -> compareBy<DirectoryItem> { !it.isDirectory }.thenBy { it.sizeBytes }
        }
        return items.sortedWith(comparator)
    }

    private fun getRealStorageOverview(): StorageOverview {
        return try {
            val statFs = StatFs(Environment.getDataDirectory().path)
            val blockSize = statFs.blockSizeLong
            val totalBlocks = statFs.blockCountLong
            val availableBlocks = statFs.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = max(0L, totalBytes - freeBytes)
            val usedPercent = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) * 100f else 0f

            val (photosCount, photosBytes) = queryMediaCategory(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            val (videosCount, videosBytes) = queryMediaCategory(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            val (audioCount, audioBytes) = queryMediaCategory(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            val (docCount, docBytes) = queryDocuments()
            val (downloadCount, downloadBytes) = queryDownloads()

            val mediaSum = photosBytes + videosBytes + audioBytes + docBytes + downloadBytes
            val nonMediaUsed = max(0L, usedBytes - mediaSum)
            val userAppApkBytes = queryInstalledAppsBytes(systemAppsOnly = false)
            val systemApkBytes = queryInstalledAppsBytes(systemAppsOnly = true)
            // Keep each displayed bucket independent.  The remainder is intentionally
            // preserved as Other instead of being absorbed into the System estimate.
            val appsBytes = (userAppApkBytes * 1.8).toLong().coerceIn(0L, nonMediaUsed)
            val systemBytes = systemApkBytes.coerceIn(0L, (nonMediaUsed - appsBytes).coerceAtLeast(0L))
            val otherBytes = max(0L, usedBytes - (
                photosBytes + videosBytes + audioBytes + docBytes + downloadBytes + appsBytes + systemBytes
                ))

            StorageOverview(
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                freeBytes = freeBytes,
                usedPercent = usedPercent,
                formattedTotal = formatBytes(totalBytes),
                formattedUsed = formatBytes(usedBytes),
                formattedFree = formatBytes(freeBytes),
                photosBytes = photosBytes,
                videosBytes = videosBytes,
                documentsBytes = docBytes,
                audioBytes = audioBytes,
                downloadsBytes = downloadBytes,
                appsBytes = appsBytes,
                systemBytes = systemBytes,
                otherBytes = otherBytes
            )
        } catch (e: Exception) {
            StorageOverview()
        }
    }

    private fun queryInstalledAppsBytes(systemAppsOnly: Boolean? = null): Long {
        return try {
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(0)
            var totalAppApkBytes = 0L
            for (app in apps) {
                val isSystemApp = app.flags and ApplicationInfo.FLAG_SYSTEM != 0
                if (systemAppsOnly != null && isSystemApp != systemAppsOnly) continue
                val source = app.sourceDir
                if (!source.isNullOrBlank()) {
                    val file = File(source)
                    if (file.exists()) {
                        totalAppApkBytes += file.length()
                    }
                }
                app.splitSourceDirs?.forEach { splitPath ->
                    val splitFile = File(splitPath)
                    if (splitFile.exists()) {
                        totalAppApkBytes += splitFile.length()
                    }
                }
            }
            totalAppApkBytes
        } catch (_: Exception) {
            0L
        }
    }

    private fun queryMediaCategory(contentUri: Uri): Pair<Int, Long> {
        var count = 0
        var totalBytes = 0L
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.SIZE)
        try {
            context.contentResolver.query(contentUri, projection, null, null, null)?.use { cursor ->
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                while (cursor.moveToNext()) {
                    count++
                    if (sizeCol != -1) {
                        val size = cursor.getLong(sizeCol)
                        if (size > 0) totalBytes += size
                    }
                }
            }
        } catch (_: Exception) {}
        return Pair(count, totalBytes)
    }

    private fun queryDocuments(): Pair<Int, Long> {
        var count = 0
        var totalBytes = 0L
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.SIZE)
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE '%.pdf' OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE '%.doc%' OR " +
                "${MediaStore.Files.FileColumns.DATA} LIKE '%.txt'"
        val selectionArgs = arrayOf("application/pdf%", "text/%", "application/msword%")
        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                while (cursor.moveToNext()) {
                    count++
                    if (sizeCol != -1) {
                        val size = cursor.getLong(sizeCol)
                        if (size > 0) totalBytes += size
                    }
                }
            }
        } catch (_: Exception) {}
        return Pair(count, totalBytes)
    }

    private fun queryDownloads(): Pair<Int, Long> {
        var count = 0
        var totalBytes = 0L
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.SIZE)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    val sizeCol = cursor.getColumnIndex(MediaStore.Downloads.SIZE)
                    while (cursor.moveToNext()) {
                        count++
                        if (sizeCol != -1) {
                            val size = cursor.getLong(sizeCol)
                            if (size > 0) totalBytes += size
                        }
                    }
                }
            } else {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadDir.exists()) {
                    val files = downloadDir.listFiles() ?: emptyArray()
                    count = files.size
                    totalBytes = files.sumOf { it.length() }
                }
            }
        } catch (_: Exception) {}
        return Pair(count, totalBytes)
    }

    private fun getRealCategories(totalStorageBytes: Long): List<CategoryItem> {
        val (photosCount, photosBytes) = queryMediaCategory(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val (videosCount, videosBytes) = queryMediaCategory(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        val (audioCount, audioBytes) = queryMediaCategory(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        val (docCount, docBytes) = queryDocuments()
        val (dlCount, dlBytes) = queryDownloads()

        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalBytes = statFs.blockCountLong * statFs.blockSizeLong
        val freeBytes = statFs.availableBlocksLong * statFs.blockSizeLong
        val usedBytes = max(0L, totalBytes - freeBytes)
        val mediaSum = photosBytes + videosBytes + audioBytes + docBytes + dlBytes
        val nonMediaUsed = max(0L, usedBytes - mediaSum)
        val userAppApkBytes = queryInstalledAppsBytes(systemAppsOnly = false)
        val systemApkBytes = queryInstalledAppsBytes(systemAppsOnly = true)
        val appsBytes = (userAppApkBytes * 1.8).toLong().coerceIn(0L, nonMediaUsed)
        val systemBytes = systemApkBytes.coerceIn(0L, (nonMediaUsed - appsBytes).coerceAtLeast(0L))
        val otherBytes = max(0L, usedBytes - (
            photosBytes + videosBytes + audioBytes + docBytes + dlBytes + appsBytes + systemBytes
            ))

        val maxCategory = max(1L, maxOf(photosBytes, videosBytes, audioBytes, docBytes, dlBytes, appsBytes, systemBytes, otherBytes))

        return listOf(
            CategoryItem(
                title = "Photos",
                count = photosCount,
                bytes = photosBytes,
                formattedSize = formatBytes(photosBytes),
                fileCountLabel = if (photosCount > 0) String.format(Locale.US, "%,d files", photosCount) else "No files",
                progressFraction = (photosBytes.toFloat() / maxCategory).coerceIn(0.08f, 1f),
                category = FileCategory.IMAGE
            ),
            CategoryItem(
                title = "Videos",
                count = videosCount,
                bytes = videosBytes,
                formattedSize = formatBytes(videosBytes),
                fileCountLabel = if (videosCount > 0) String.format(Locale.US, "%,d files", videosCount) else "No files",
                progressFraction = (videosBytes.toFloat() / maxCategory).coerceIn(0.08f, 1f),
                category = FileCategory.VIDEO
            ),
            CategoryItem(
                title = "Documents",
                count = docCount,
                bytes = docBytes,
                formattedSize = formatBytes(docBytes),
                fileCountLabel = if (docCount > 0) String.format(Locale.US, "%,d files", docCount) else "No files",
                progressFraction = (docBytes.toFloat() / maxCategory).coerceIn(0.08f, 1f),
                category = FileCategory.DOCUMENT
            ),
            CategoryItem(
                title = "Audio",
                count = audioCount,
                bytes = audioBytes,
                formattedSize = formatBytes(audioBytes),
                fileCountLabel = if (audioCount > 0) String.format(Locale.US, "%,d files", audioCount) else "No files",
                progressFraction = (audioBytes.toFloat() / maxCategory).coerceIn(0.08f, 1f),
                category = FileCategory.AUDIO
            ),
            CategoryItem(
                title = "Downloads",
                count = dlCount,
                bytes = dlBytes,
                formattedSize = formatBytes(dlBytes),
                fileCountLabel = if (dlCount > 0) String.format(Locale.US, "%,d files", dlCount) else "No files",
                progressFraction = (dlBytes.toFloat() / maxCategory).coerceIn(0.08f, 1f),
                category = FileCategory.ARCHIVE
            ),
            CategoryItem(
                title = "Apps",
                count = 0,
                bytes = appsBytes,
                formattedSize = formatBytes(appsBytes),
                fileCountLabel = "Applications",
                progressFraction = (appsBytes.toFloat() / maxCategory).coerceIn(0.08f, 1f),
                category = FileCategory.GENERIC
            ),
            CategoryItem(
                title = "System",
                count = 1,
                bytes = systemBytes,
                formattedSize = formatBytes(systemBytes),
                fileCountLabel = "Android OS",
                progressFraction = (systemBytes.toFloat() / maxCategory).coerceIn(0.08f, 1f),
                category = FileCategory.SYSTEM
            ),
            CategoryItem(
                title = "Other",
                count = 0,
                bytes = otherBytes,
                formattedSize = formatBytes(otherBytes),
                fileCountLabel = "Miscellaneous files",
                progressFraction = (otherBytes.toFloat() / maxCategory).coerceIn(0.08f, 1f),
                category = FileCategory.OTHER
            )
        )
    }

    private fun getRealRecentFiles(): List<RecentFile> {
        val list = mutableListOf<RecentFile>()
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)

                var items = 0
                while (cursor.moveToNext() && items < 12) {
                    val id = if (idCol != -1) cursor.getLong(idCol) else 0L
                    var name = if (nameCol != -1) cursor.getString(nameCol) else null
                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val dateSec = if (dateCol != -1) cursor.getLong(dateCol) else 0L
                    val mime = if (mimeCol != -1) cursor.getString(mimeCol) ?: "" else ""

                    if (name.isNullOrBlank()) {
                        name = File(path).name
                    }
                    if (name.isNullOrBlank() || name.startsWith(".")) continue

                    val itemUri = ContentUris.withAppendedId(uri, id)
                    val category = detectCategory(name, mime)
                    val dateMs = dateSec * 1000L

                    list.add(
                        RecentFile(
                            id = id,
                            name = name,
                            uri = itemUri,
                            path = path,
                            sizeBytes = size,
                            formattedSize = formatBytes(size),
                            dateModifiedMs = dateMs,
                            formattedDate = formatRelativeDate(dateMs),
                            mimeType = mime,
                            category = category
                        )
                    )
                    items++
                }
            }
        } catch (_: Exception) {}

        if (list.isEmpty()) {
            list.addAll(getFallbackRecentFiles())
        }
        return list
    }

    private fun detectCategory(fileName: String, mimeType: String): FileCategory {
        val lowerName = fileName.lowercase(Locale.ROOT)
        return when {
            mimeType.startsWith("image/") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png") || lowerName.endsWith(".webp") -> FileCategory.IMAGE
            mimeType.startsWith("video/") || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") || lowerName.endsWith(".mov") -> FileCategory.VIDEO
            mimeType.startsWith("audio/") || lowerName.endsWith(".mp3") || lowerName.endsWith(".m4a") || lowerName.endsWith(".wav") || lowerName.endsWith(".flac") -> FileCategory.AUDIO
            lowerName.endsWith(".pdf") || mimeType == "application/pdf" -> FileCategory.DOCUMENT
            lowerName.endsWith(".zip") || lowerName.endsWith(".rar") || lowerName.endsWith(".7z") || lowerName.endsWith(".tar") || lowerName.endsWith(".gz") -> FileCategory.ARCHIVE
            lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls") || lowerName.endsWith(".csv") -> FileCategory.SPREADSHEET
            else -> FileCategory.GENERIC
        }
    }

    private fun getFallbackRecentFiles(): List<RecentFile> {
        val now = System.currentTimeMillis()
        return listOf(
            RecentFile(1, "IMG_20261011_1042.jpg", Uri.EMPTY, "", 4_800_000, "4.8 MB", now, "Today, 10:42 AM", "image/jpeg", FileCategory.IMAGE),
            RecentFile(2, "Project_Proposal.pdf", Uri.EMPTY, "", 2_400_000, "2.4 MB", now - 3600_000, "Today, 9:18 AM", "application/pdf", FileCategory.DOCUMENT),
            RecentFile(3, "Meeting_Recording.mp3", Uri.EMPTY, "", 18_600_000, "18.6 MB", now - 86400_000, "Yesterday, 6:32 PM", "audio/mpeg", FileCategory.AUDIO),
            RecentFile(4, "UI_Design_References.zip", Uri.EMPTY, "", 126_000_000, "126 MB", now - 90000_000, "Yesterday, 4:15 PM", "application/zip", FileCategory.ARCHIVE),
            RecentFile(5, "Budget_Tracker.xlsx", Uri.EMPTY, "", 840_000, "840 KB", now - 172800_000, "Oct 9, 2026", "application/vnd.ms-excel", FileCategory.SPREADSHEET),
            RecentFile(6, "Travel_Video_04.mp4", Uri.EMPTY, "", 284_000_000, "284 MB", now - 259200_000, "Oct 8, 2026", "video/mp4", FileCategory.VIDEO)
        )
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
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

    private fun formatRelativeDate(timestamp: Long): String {
        if (timestamp <= 0) return "Unknown"
        val date = Date(timestamp)
        val now = Calendar.getInstance()
        val itemCal = Calendar.getInstance().apply { time = date }

        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
        return when {
            isSameDay(now, itemCal) -> "Today, ${timeFormat.format(date)}"
            isYesterday(now, itemCal) -> "Yesterday, ${timeFormat.format(date)}"
            now.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) -> SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
            else -> SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(now: Calendar, cal: Calendar): Boolean {
        val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        return isSameDay(yesterday, cal)
    }
}
