package com.ioristudios.filemanager.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

enum class ArchiveType {
    ZIP,
    TAR,
    TAR_GZ,
    TAR_BZ2,
    SEVEN_Z,
    UNKNOWN
}

enum class CompressionLevel(val level: Int, val label: String) {
    STORE(0, "Store (No compression)"),
    FAST(1, "Fast"),
    NORMAL(6, "Normal (Balanced)"),
    MAXIMUM(9, "Maximum (Ultra)")
}

object ArchiveManager {

    fun isArchive(file: File): Boolean {
        if (file.isDirectory) return false
        val name = file.name.lowercase(Locale.ROOT)
        return name.endsWith(".zip") ||
                name.endsWith(".jar") ||
                name.endsWith(".apk") ||
                name.endsWith(".xapk") ||
                name.endsWith(".apks") ||
                name.endsWith(".tar") ||
                name.endsWith(".tar.gz") ||
                name.endsWith(".tgz") ||
                name.endsWith(".tar.bz2") ||
                name.endsWith(".tbz2") ||
                name.endsWith(".7z") ||
                name.endsWith(".gz")
    }

    fun detectArchiveType(file: File): ArchiveType {
        val name = file.name.lowercase(Locale.ROOT)
        return when {
            name.endsWith(".zip") || name.endsWith(".jar") || name.endsWith(".apk") || name.endsWith(".xapk") || name.endsWith(".apks") -> ArchiveType.ZIP
            name.endsWith(".tar.gz") || name.endsWith(".tgz") -> ArchiveType.TAR_GZ
            name.endsWith(".tar.bz2") || name.endsWith(".tbz2") -> ArchiveType.TAR_BZ2
            name.endsWith(".tar") -> ArchiveType.TAR
            name.endsWith(".7z") -> ArchiveType.SEVEN_Z
            else -> ArchiveType.UNKNOWN
        }
    }

    /**
     * Lists virtual entries inside an archive at [innerPath].
     * Virtual folders are synthesized for nested directory paths so users can browse in-place.
     */
    suspend fun listArchiveEntries(
        archiveFile: File,
        innerPath: String = ""
    ): List<DirectoryItem> = withContext(Dispatchers.IO) {
        val cleanInner = innerPath.trim('/').let { if (it.isEmpty()) "" else "$it/" }
        val type = detectArchiveType(archiveFile)

        return@withContext when (type) {
            ArchiveType.ZIP -> listZipEntries(archiveFile, cleanInner)
            ArchiveType.TAR, ArchiveType.TAR_GZ, ArchiveType.TAR_BZ2 -> listTarEntries(archiveFile, type, cleanInner)
            else -> emptyList()
        }
    }

    private fun listZipEntries(archiveFile: File, prefix: String): List<DirectoryItem> {
        val directItems = mutableMapOf<String, DirectoryItem>()

        try {
            ZipFile(archiveFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val entryName = entry.name.trimStart('/')

                    if (prefix.isNotEmpty() && !entryName.startsWith(prefix)) {
                        continue
                    }

                    val relative = if (prefix.isEmpty()) entryName else entryName.removePrefix(prefix)
                    if (relative.isEmpty()) continue

                    val slashIndex = relative.indexOf('/')
                    if (slashIndex != -1) {
                        // Subdirectory item
                        val dirName = relative.substring(0, slashIndex)
                        if (!directItems.containsKey(dirName)) {
                            directItems[dirName] = DirectoryItem(
                                name = dirName,
                                path = "${archiveFile.absolutePath}::$prefix$dirName",
                                isDirectory = true,
                                sizeBytes = 0L,
                                formattedSize = "Folder",
                                childCount = 1,
                                fileCountLabel = "Folder",
                                dateModifiedMs = entry.time.takeIf { it > 0 } ?: archiveFile.lastModified(),
                                formattedDate = "Archive Folder",
                                visualType = DirectoryVisualType.FOLDER
                            )
                        } else {
                            val existing = directItems[dirName]!!
                            directItems[dirName] = existing.copy(childCount = existing.childCount + 1)
                        }
                    } else {
                        // Direct file item
                        val isDir = entry.isDirectory
                        val size = if (isDir) 0L else entry.size.coerceAtLeast(0L)
                        val formattedSize = formatBytes(size)
                        directItems[relative] = DirectoryItem(
                            name = relative,
                            path = "${archiveFile.absolutePath}::$prefix$relative",
                            isDirectory = isDir,
                            sizeBytes = size,
                            formattedSize = formattedSize,
                            childCount = 0,
                            fileCountLabel = if (isDir) "Folder" else formattedSize,
                            dateModifiedMs = entry.time.takeIf { it > 0 } ?: archiveFile.lastModified(),
                            formattedDate = "In Archive",
                            visualType = if (isDir) DirectoryVisualType.FOLDER else DirectoryVisualType.FILE_GENERIC
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        return directItems.values.sortedWith(
            compareBy<DirectoryItem> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) }
        )
    }

    private fun listTarEntries(archiveFile: File, type: ArchiveType, prefix: String): List<DirectoryItem> {
        val directItems = mutableMapOf<String, DirectoryItem>()

        try {
            openTarInputStream(archiveFile, type).use { tarIn ->
                var entry: TarArchiveEntry? = tarIn.nextEntry
                while (entry != null) {
                    val entryName = entry.name.trimStart('/')
                    if (prefix.isEmpty() || entryName.startsWith(prefix)) {
                        val relative = if (prefix.isEmpty()) entryName else entryName.removePrefix(prefix)
                        if (relative.isNotEmpty()) {
                            val slashIndex = relative.indexOf('/')
                            if (slashIndex != -1) {
                                val dirName = relative.substring(0, slashIndex)
                                if (!directItems.containsKey(dirName)) {
                                    directItems[dirName] = DirectoryItem(
                                        name = dirName,
                                        path = "${archiveFile.absolutePath}::$prefix$dirName",
                                        isDirectory = true,
                                        sizeBytes = 0L,
                                        formattedSize = "Folder",
                                        childCount = 1,
                                        fileCountLabel = "Folder",
                                        dateModifiedMs = entry.modTime?.time ?: archiveFile.lastModified(),
                                        formattedDate = "Archive Folder",
                                        visualType = DirectoryVisualType.FOLDER
                                    )
                                }
                            } else {
                                val isDir = entry.isDirectory
                                val size = if (isDir) 0L else entry.size.coerceAtLeast(0L)
                                val formattedSize = formatBytes(size)
                                directItems[relative] = DirectoryItem(
                                    name = relative,
                                    path = "${archiveFile.absolutePath}::$prefix$relative",
                                    isDirectory = isDir,
                                    sizeBytes = size,
                                    formattedSize = formattedSize,
                                    childCount = 0,
                                    fileCountLabel = if (isDir) "Folder" else formattedSize,
                                    dateModifiedMs = entry.modTime?.time ?: archiveFile.lastModified(),
                                    formattedDate = "In Archive",
                                    visualType = if (isDir) DirectoryVisualType.FOLDER else DirectoryVisualType.FILE_GENERIC
                                )
                            }
                        }
                    }
                    entry = tarIn.nextEntry
                }
            }
        } catch (_: Exception) {}

        return directItems.values.sortedWith(
            compareBy<DirectoryItem> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) }
        )
    }

    private fun openTarInputStream(archiveFile: File, type: ArchiveType): TarArchiveInputStream {
        val rawIn = BufferedInputStream(FileInputStream(archiveFile))
        val decompressedIn: InputStream = when (type) {
            ArchiveType.TAR_GZ -> GzipCompressorInputStream(rawIn)
            ArchiveType.TAR_BZ2 -> BZip2CompressorInputStream(rawIn)
            else -> rawIn
        }
        return TarArchiveInputStream(decompressedIn)
    }

    /**
     * Extracts an entire archive to [targetDir].
     */
    suspend fun extractAll(
        archiveFile: File,
        targetDir: File,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!targetDir.exists()) targetDir.mkdirs()
        val type = detectArchiveType(archiveFile)

        return@withContext try {
            when (type) {
                ArchiveType.ZIP -> extractZip(archiveFile, targetDir, onProgress)
                ArchiveType.TAR, ArchiveType.TAR_GZ, ArchiveType.TAR_BZ2 -> extractTar(archiveFile, type, targetDir, onProgress)
                else -> Result.failure(IllegalArgumentException("Unsupported archive type"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractZip(
        archiveFile: File,
        targetDir: File,
        onProgress: (Int, Int, String) -> Unit
    ): Result<Int> {
        var count = 0
        ZipFile(archiveFile).use { zip ->
            val total = zip.size()
            val entries = zip.entries()
            val buffer = ByteArray(64 * 1024)

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val outFile = File(targetDir, entry.name)

                // Guard against Zip Slip vulnerability
                if (!outFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    continue
                }

                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { inStream ->
                        FileOutputStream(outFile).use { outStream ->
                            var read: Int
                            while (inStream.read(buffer).also { read = it } != -1) {
                                outStream.write(buffer, 0, read)
                            }
                        }
                    }
                }
                count++
                onProgress(count, total, entry.name)
            }
        }
        return Result.success(count)
    }

    private fun extractTar(
        archiveFile: File,
        type: ArchiveType,
        targetDir: File,
        onProgress: (Int, Int, String) -> Unit
    ): Result<Int> {
        var count = 0
        val buffer = ByteArray(64 * 1024)

        openTarInputStream(archiveFile, type).use { tarIn ->
            var entry: TarArchiveEntry? = tarIn.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (!outFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    entry = tarIn.nextEntry
                    continue
                }

                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { outStream ->
                        var read: Int
                        while (tarIn.read(buffer).also { read = it } != -1) {
                            outStream.write(buffer, 0, read)
                        }
                    }
                }
                count++
                onProgress(count, -1, entry.name)
                entry = tarIn.nextEntry
            }
        }
        return Result.success(count)
    }

    /**
     * Extracts a single item from an archive to a temporary file for instant viewing.
     */
    suspend fun extractSingleFile(
        archiveFile: File,
        entryPath: String,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        val type = detectArchiveType(archiveFile)
        val cleanEntry = entryPath.trimStart('/')

        return@withContext try {
            outputFile.parentFile?.mkdirs()
            if (type == ArchiveType.ZIP) {
                ZipFile(archiveFile).use { zip ->
                    val entry = zip.getEntry(cleanEntry)
                        ?: return@withContext Result.failure(NoSuchElementException("Entry $cleanEntry not found"))
                    zip.getInputStream(entry).use { inStream ->
                        FileOutputStream(outputFile).use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                }
                Result.success(outputFile)
            } else {
                Result.failure(UnsupportedOperationException("Single extract not supported for this format"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compresses the selected files/directories into a new ZIP archive.
     */
    suspend fun createZipArchive(
        sourceFiles: List<File>,
        destinationZip: File,
        level: CompressionLevel = CompressionLevel.NORMAL,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        return@withContext try {
            destinationZip.parentFile?.mkdirs()
            val allFiles = mutableListOf<Pair<File, String>>()

            for (src in sourceFiles) {
                if (src.isDirectory) {
                    collectDirFiles(src, src.name, allFiles)
                } else {
                    allFiles.add(Pair(src, src.name))
                }
            }

            val total = allFiles.size
            var current = 0
            val buffer = ByteArray(64 * 1024)

            ZipOutputStream(BufferedOutputStream(FileOutputStream(destinationZip))).use { zipOut ->
                zipOut.setLevel(level.level)
                for ((file, pathInZip) in allFiles) {
                    val entry = ZipEntry(pathInZip)
                    entry.time = file.lastModified()
                    zipOut.putNextEntry(entry)

                    FileInputStream(file).use { fileIn ->
                        var read: Int
                        while (fileIn.read(buffer).also { read = it } != -1) {
                            zipOut.write(buffer, 0, read)
                        }
                    }
                    zipOut.closeEntry()
                    current++
                    onProgress(current, total, file.name)
                }
            }
            Result.success(destinationZip)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compresses the selected files/directories into a TAR archive.
     */
    suspend fun createTarArchive(
        sourceFiles: List<File>,
        destinationTar: File,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        return@withContext try {
            destinationTar.parentFile?.mkdirs()
            val allFiles = mutableListOf<Pair<File, String>>()

            for (src in sourceFiles) {
                if (src.isDirectory) {
                    collectDirFiles(src, src.name, allFiles)
                } else {
                    allFiles.add(Pair(src, src.name))
                }
            }

            val total = allFiles.size
            var current = 0
            val buffer = ByteArray(64 * 1024)

            TarArchiveOutputStream(BufferedOutputStream(FileOutputStream(destinationTar))).use { tarOut ->
                for ((file, pathInTar) in allFiles) {
                    val entry = TarArchiveEntry(file, pathInTar)
                    tarOut.putArchiveEntry(entry)

                    FileInputStream(file).use { fileIn ->
                        var read: Int
                        while (fileIn.read(buffer).also { read = it } != -1) {
                            tarOut.write(buffer, 0, read)
                        }
                    }
                    tarOut.closeArchiveEntry()
                    current++
                    onProgress(current, total, file.name)
                }
            }
            Result.success(destinationTar)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun collectDirFiles(dir: File, currentPrefix: String, outList: MutableList<Pair<File, String>>) {
        val children = dir.listFiles() ?: return
        for (child in children) {
            val childPath = "$currentPrefix/${child.name}"
            if (child.isDirectory) {
                collectDirFiles(child, childPath, outList)
            } else {
                outList.add(Pair(child, childPath))
            }
        }
    }

    /**
     * Tests archive integrity by verifying headers and calculating CRC32 without extracting files.
     */
    suspend fun testArchiveIntegrity(archiveFile: File): Result<String> = withContext(Dispatchers.IO) {
        val type = detectArchiveType(archiveFile)
        if (type != ArchiveType.ZIP) {
            return@withContext Result.failure(UnsupportedOperationException("Testing integrity currently supported for ZIP/APK/JAR archives"))
        }

        return@withContext try {
            var testedCount = 0
            val buffer = ByteArray(64 * 1024)

            ZipFile(archiveFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory) {
                        val crc = CRC32()
                        zip.getInputStream(entry).use { inStream ->
                            var read: Int
                            while (inStream.read(buffer).also { read = it } != -1) {
                                crc.update(buffer, 0, read)
                            }
                        }
                        if (entry.crc != -1L && crc.value != entry.crc) {
                            return@withContext Result.failure(
                                IllegalStateException("CRC mismatch in entry '${entry.name}': expected ${entry.crc}, got ${crc.value}")
                            )
                        }
                        testedCount++
                    }
                }
            }
            Result.success("Integrity verified: $testedCount files checked without errors.")
        } catch (e: Exception) {
            Result.failure(e)
        }
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
