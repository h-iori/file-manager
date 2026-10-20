
package com.ioristudios.filemanager.data

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.ArrayDeque

data class CleanupItem(
    val path: String,
    val name: String,
    val bytes: Long,
    val reason: String,
    val selectedByDefault: Boolean
)

data class CleanupCategory(
    val title: String,
    val items: List<CleanupItem>
) {
    val totalBytes: Long get() = items.sumOf { it.bytes }
}

data class CleanupScanResult(val categories: List<CleanupCategory>) {
    val totalBytes: Long get() = categories.sumOf { it.totalBytes }
}

data class CleanupScanProgress(val phase: String = "Preparing cleanup scan", val scannedItems: Int = 0)

/**
 * Conservative cleanup scanner. It never deletes anything until the user confirms exact paths.
 * Only safe-to-regenerate categories are selected initially; old personal files require opt-in.
 */
class CleanupEngine(private val context: Context) {
    private val _progress = MutableStateFlow(CleanupScanProgress())
    val progress: StateFlow<CleanupScanProgress> = _progress.asStateFlow()

    /**
     * Estimates the bytes available from cleanup items that are selected by default.
     * Personal/old large files are intentionally excluded because they require explicit opt-in.
     * This lightweight estimate avoids the full deep scan used by the cleanup review screen.
     */
    suspend fun estimateDefaultReclaimableBytes(): Long = withContext(Dispatchers.IO) {
        val storage = Environment.getExternalStorageDirectory()
        val cacheTargets = listOfNotNull(context.externalCacheDir, context.cacheDir)
            .distinctBy { it.absolutePath }
            .filter { it.exists() && it.canWrite() }

        val thumbnailTargets = listOf(
            File(storage, "DCIM/.thumbnails"),
            File(storage, "Pictures/.thumbnails")
        ).filter { it.exists() && it.canWrite() }

        (cacheTargets + thumbnailTargets).sumOf { directorySize(it).coerceAtLeast(0L) }
    }

    suspend fun scan(): CleanupScanResult = withContext(Dispatchers.IO) {
        val storage = Environment.getExternalStorageDirectory()
        val publicRoots = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        ).filter { it.exists() && it.canRead() }

        val categories = mutableListOf<CleanupCategory>()
        _progress.value = CleanupScanProgress("Finding app cache", 0)
        val caches = listOfNotNull(context.externalCacheDir, context.cacheDir)
            .distinctBy { it.absolutePath }
            .filter { it.exists() && it.canWrite() }
            .map { cache -> CleanupItem(cache.absolutePath, cache.name, directorySize(cache), "Regenerable app cache", true) }
        categories += CleanupCategory("App cache", caches)

        _progress.value = CleanupScanProgress("Finding thumbnail caches", caches.size)
        val thumbnails = listOf(
            File(storage, "DCIM/.thumbnails"),
            File(storage, "Pictures/.thumbnails")
        ).filter { it.exists() && it.canWrite() }
            .map { dir -> CleanupItem(dir.absolutePath, dir.name, directorySize(dir), "Regenerable thumbnails", true) }
        categories += CleanupCategory("Thumbnail cache", thumbnails)

        _progress.value = CleanupScanProgress("Finding empty folders", thumbnails.size)
        val emptyFolders = findEmptyFolders(publicRoots)
        categories += CleanupCategory("Empty folders", emptyFolders)

        _progress.value = CleanupScanProgress("Finding old large files", emptyFolders.size)
        val oldLargeFiles = findOldLargeFiles(publicRoots)
        categories += CleanupCategory("Large & old files", oldLargeFiles)

        _progress.value = CleanupScanProgress("Scan complete", caches.size + thumbnails.size + emptyFolders.size + oldLargeFiles.size)
        CleanupScanResult(categories.filter { it.items.isNotEmpty() })
    }

    suspend fun clean(paths: Set<String>): Long = withContext(Dispatchers.IO) {
        var released = 0L
        paths.forEach { path ->
            val target = File(path)
            if (!target.exists()) return@forEach
            val size = directorySize(target)
            val deleted = if (target.isDirectory) target.deleteRecursively() else target.delete()
            if (deleted) released += size
        }
        released
    }

    private fun findEmptyFolders(roots: List<File>): List<CleanupItem> {
        val results = mutableListOf<CleanupItem>()
        val queue = ArrayDeque<Pair<File, Int>>()
        // Never offer a public root (Downloads, DCIM, Pictures, Documents) itself for deletion.
        roots.forEach { root ->
            root.listFiles()?.filter { it.isDirectory }?.forEach { queue.addLast(it to 0) }
        }
        var scanned = 0
        while (queue.isNotEmpty() && scanned < 2_000) {
            val (directory, depth) = queue.removeFirst()
            if (depth > 8 || directory.name.startsWith('.') || File(directory, ".nomedia").exists()) continue
            val children = directory.listFiles() ?: continue
            scanned++
            if (children.isEmpty() && directory.canWrite()) {
                results += CleanupItem(directory.absolutePath, directory.name, 0L, "Empty folder", true)
            } else {
                children.filter { it.isDirectory }.forEach { queue.addLast(it to depth + 1) }
            }
        }
        return results
    }

    private fun findOldLargeFiles(roots: List<File>): List<CleanupItem> {
        val cutoff = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
        val results = mutableListOf<CleanupItem>()
        roots.forEach { root ->
            root.walkTopDown()
                .maxDepth(8)
                .filter { it.isFile && it.length() >= 50L * 1024 * 1024 && it.lastModified() in 1..cutoff }
                .take(250)
                .forEach { file ->
                    results += CleanupItem(file.absolutePath, file.name, file.length(), "Over 50 MB and unused for 90+ days", false)
                }
        }
        return results
    }

    private fun directorySize(file: File): Long {
        if (file.isFile) return file.length()
        return file.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
