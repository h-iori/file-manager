package com.ioristudios.filemanager.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
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
import kotlin.coroutines.cancellation.CancellationException

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

    private const val BUFFER_SIZE = 128 * 1024

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
                        val dirName = relative.substring(0, slashIndex)
                        if (!directItems.containsKey(dirName)) {
                            directItems[dirName] = DirectoryItem(
                                name = dirName,
                                path = "$prefix$dirName",
                                isDirectory = true,
                                sizeBytes = 0L,
                                formattedSize = "",
                                childCount = 0,
                                fileCountLabel = "Folder",
                                dateModifiedMs = entry.time.takeIf { it > 0 } ?: archiveFile.lastModified(),
                                formattedDate = "",
                                visualType = DirectoryVisualType.FOLDER
                            )
                        }
                    } else {
                        val isDir = entry.isDirectory
                        directItems[relative] = DirectoryItem(
                            name = relative,
                            path = "$prefix$relative",
                            isDirectory = isDir,
                            sizeBytes = if (isDir) 0L else entry.size.coerceAtLeast(0L),
                            formattedSize = if (isDir) "" else formatBytes(entry.size.coerceAtLeast(0L)),
                            childCount = 0,
                            fileCountLabel = if (isDir) "Folder" else formatBytes(entry.size.coerceAtLeast(0L)),
                            dateModifiedMs = entry.time.takeIf { it > 0 } ?: archiveFile.lastModified(),
                            formattedDate = "",
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

                    if (prefix.isNotEmpty() && !entryName.startsWith(prefix)) {
                        entry = tarIn.nextEntry
                        continue
                    }

                    val relative = if (prefix.isEmpty()) entryName else entryName.removePrefix(prefix)
                    if (relative.isEmpty()) {
                        entry = tarIn.nextEntry
                        continue
                    }

                    val slashIndex = relative.indexOf('/')
                    if (slashIndex != -1) {
                        val dirName = relative.substring(0, slashIndex)
                        if (!directItems.containsKey(dirName)) {
                            directItems[dirName] = DirectoryItem(
                                name = dirName,
                                path = "$prefix$dirName",
                                isDirectory = true,
                                sizeBytes = 0L,
                                formattedSize = "",
                                childCount = 0,
                                fileCountLabel = "Folder",
                                dateModifiedMs = entry.modTime.time.takeIf { it > 0 } ?: archiveFile.lastModified(),
                                formattedDate = "",
                                visualType = DirectoryVisualType.FOLDER
                            )
                        }
                    } else {
                        val isDir = entry.isDirectory
                        directItems[relative] = DirectoryItem(
                            name = relative,
                            path = "$prefix$relative",
                            isDirectory = isDir,
                            sizeBytes = if (isDir) 0L else entry.size.coerceAtLeast(0L),
                            formattedSize = if (isDir) "" else formatBytes(entry.size.coerceAtLeast(0L)),
                            childCount = 0,
                            fileCountLabel = if (isDir) "Folder" else formatBytes(entry.size.coerceAtLeast(0L)),
                            dateModifiedMs = entry.modTime.time.takeIf { it > 0 } ?: archiveFile.lastModified(),
                            formattedDate = "",
                            visualType = if (isDir) DirectoryVisualType.FOLDER else DirectoryVisualType.FILE_GENERIC
                        )
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
     * Extracts an entire archive to [targetDir] with live real-time byte progress, pause, resume, cancel, and rollback.
     */
    suspend fun extractAll(
        context: Context,
        archiveFile: File,
        targetDir: File,
        token: CancellablePauseableToken
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
            token.trackCreatedItem(targetDir)
        }
        val type = detectArchiveType(archiveFile)
        val extractedPaths = mutableListOf<String>()

        return@withContext try {
            val count = when (type) {
                ArchiveType.ZIP -> extractZip(context, archiveFile, targetDir, token, extractedPaths)
                ArchiveType.TAR, ArchiveType.TAR_GZ, ArchiveType.TAR_BZ2 -> extractTar(context, archiveFile, type, targetDir, token, extractedPaths)
                else -> throw IllegalArgumentException("Unsupported archive type")
            }
            MediaStoreNotifier.scanFiles(context, extractedPaths)
            FileOperationController.completeSuccess()
            Result.success(count)
        } catch (e: CancellationException) {
            token.rollback()
            FileOperationController.cancel()
            Result.failure(e)
        } catch (e: Exception) {
            token.rollback()
            FileOperationController.completeFailure(e.message ?: "Extraction failed")
            Result.failure(e)
        }
    }

    private fun extractZip(
        context: Context,
        archiveFile: File,
        targetDir: File,
        token: CancellablePauseableToken,
        extractedPaths: MutableList<String>
    ): Int {
        var count = 0
        ZipFile(archiveFile).use { zip ->
            val totalFiles = zip.size()
            var totalBytes = 0L

            // Calculate total uncompressed bytes
            val entriesEnum = zip.entries()
            while (entriesEnum.hasMoreElements()) {
                val e = entriesEnum.nextElement()
                if (!e.isDirectory && e.size > 0L) {
                    totalBytes += e.size
                }
            }
            if (totalBytes <= 0L) {
                totalBytes = archiveFile.length() * 2L // Estimate if compressed sizes not in header
            }

            var processedBytes = 0L
            val entries = zip.entries()
            val buffer = ByteArray(BUFFER_SIZE)

            while (entries.hasMoreElements()) {
                token.checkPauseAndCancel()
                val entry = entries.nextElement()
                val outFile = File(targetDir, entry.name)

                // Guard against Zip Slip vulnerability
                if (!outFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    continue
                }

                if (entry.isDirectory) {
                    if (!outFile.exists()) {
                        outFile.mkdirs()
                        token.trackCreatedItem(outFile)
                    }
                } else {
                    outFile.parentFile?.let { p ->
                        if (!p.exists()) {
                            p.mkdirs()
                            token.trackCreatedItem(p)
                        }
                    }
                    token.trackCreatedItem(outFile)

                    zip.getInputStream(entry).use { inStream ->
                        FileOutputStream(outFile).use { outStream ->
                            var read: Int
                            while (inStream.read(buffer).also { read = it } != -1) {
                                token.checkPauseAndCancel()
                                outStream.write(buffer, 0, read)
                                processedBytes += read
                                FileOperationController.updateProgress(
                                    processedBytes = processedBytes,
                                    totalBytes = totalBytes,
                                    currentFileName = entry.name.substringAfterLast('/'),
                                    currentFileIndex = count + 1,
                                    totalFiles = totalFiles
                                )
                            }
                        }
                    }
                    extractedPaths.add(outFile.absolutePath)
                }
                count++
            }
        }
        return count
    }

    private fun extractTar(
        context: Context,
        archiveFile: File,
        type: ArchiveType,
        targetDir: File,
        token: CancellablePauseableToken,
        extractedPaths: MutableList<String>
    ): Int {
        var count = 0
        val buffer = ByteArray(BUFFER_SIZE)
        val totalBytes = archiveFile.length() * 3L // Estimate uncompressed tar
        var processedBytes = 0L

        openTarInputStream(archiveFile, type).use { tarIn ->
            var entry: TarArchiveEntry? = tarIn.nextEntry
            while (entry != null) {
                token.checkPauseAndCancel()
                val outFile = File(targetDir, entry.name)

                // Guard against Zip Slip vulnerability
                if (!outFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    entry = tarIn.nextEntry
                    continue
                }

                if (entry.isDirectory) {
                    if (!outFile.exists()) {
                        outFile.mkdirs()
                        token.trackCreatedItem(outFile)
                    }
                } else {
                    outFile.parentFile?.let { p ->
                        if (!p.exists()) {
                            p.mkdirs()
                            token.trackCreatedItem(p)
                        }
                    }
                    token.trackCreatedItem(outFile)

                    FileOutputStream(outFile).use { outStream ->
                        var read: Int
                        while (tarIn.read(buffer).also { read = it } != -1) {
                            token.checkPauseAndCancel()
                            outStream.write(buffer, 0, read)
                            processedBytes += read
                            FileOperationController.updateProgress(
                                processedBytes = processedBytes,
                                totalBytes = totalBytes,
                                currentFileName = entry.name.substringAfterLast('/'),
                                currentFileIndex = count + 1,
                                totalFiles = -1
                            )
                        }
                    }
                    extractedPaths.add(outFile.absolutePath)
                }
                count++
                entry = tarIn.nextEntry
            }
        }
        return count
    }

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
     * Compresses the selected files/directories into a new ZIP or TAR archive with live byte throughput.
     */
    suspend fun createArchive(
        context: Context,
        sourceFiles: List<File>,
        destinationFile: File,
        format: ArchiveType = ArchiveType.ZIP,
        level: CompressionLevel = CompressionLevel.NORMAL,
        token: CancellablePauseableToken
    ): Result<File> = withContext(Dispatchers.IO) {
        return@withContext try {
            destinationFile.parentFile?.mkdirs()
            token.trackCreatedItem(destinationFile)

            val allFiles = mutableListOf<Pair<File, String>>()
            var totalBytes = 0L

            for (src in sourceFiles) {
                if (src.isDirectory) {
                    collectDirFiles(src, src.name, allFiles)
                } else {
                    allFiles.add(Pair(src, src.name))
                }
            }

            for ((f, _) in allFiles) {
                totalBytes += f.length()
            }

            val totalFiles = allFiles.size
            var currentFile = 0
            var processedBytes = 0L
            val buffer = ByteArray(BUFFER_SIZE)

            if (format == ArchiveType.ZIP) {
                ZipOutputStream(BufferedOutputStream(FileOutputStream(destinationFile))).use { zipOut ->
                    zipOut.setLevel(level.level)
                    for ((file, pathInZip) in allFiles) {
                        token.checkPauseAndCancel()
                        val entry = ZipEntry(pathInZip)
                        entry.time = file.lastModified()
                        zipOut.putNextEntry(entry)

                        FileInputStream(file).use { fileIn ->
                            var read: Int
                            while (fileIn.read(buffer).also { read = it } != -1) {
                                token.checkPauseAndCancel()
                                zipOut.write(buffer, 0, read)
                                processedBytes += read
                                FileOperationController.updateProgress(
                                    processedBytes = processedBytes,
                                    totalBytes = totalBytes,
                                    currentFileName = file.name,
                                    currentFileIndex = currentFile + 1,
                                    totalFiles = totalFiles
                                )
                            }
                        }
                        zipOut.closeEntry()
                        currentFile++
                    }
                }
            } else {
                TarArchiveOutputStream(BufferedOutputStream(FileOutputStream(destinationFile))).use { tarOut ->
                    for ((file, pathInTar) in allFiles) {
                        token.checkPauseAndCancel()
                        val entry = TarArchiveEntry(file, pathInTar)
                        tarOut.putArchiveEntry(entry)

                        FileInputStream(file).use { fileIn ->
                            var read: Int
                            while (fileIn.read(buffer).also { read = it } != -1) {
                                token.checkPauseAndCancel()
                                tarOut.write(buffer, 0, read)
                                processedBytes += read
                                FileOperationController.updateProgress(
                                    processedBytes = processedBytes,
                                    totalBytes = totalBytes,
                                    currentFileName = file.name,
                                    currentFileIndex = currentFile + 1,
                                    totalFiles = totalFiles
                                )
                            }
                        }
                        tarOut.closeArchiveEntry()
                        currentFile++
                    }
                }
            }

            MediaStoreNotifier.scanFile(context, destinationFile.absolutePath)
            FileOperationController.completeSuccess()
            Result.success(destinationFile)
        } catch (e: CancellationException) {
            token.rollback()
            FileOperationController.cancel()
            Result.failure(e)
        } catch (e: Exception) {
            token.rollback()
            FileOperationController.completeFailure(e.message ?: "Compression failed")
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

    suspend fun testArchiveIntegrity(archiveFile: File): Result<String> = withContext(Dispatchers.IO) {
        val type = detectArchiveType(archiveFile)
        if (type != ArchiveType.ZIP) {
            return@withContext Result.failure(UnsupportedOperationException("Testing integrity currently supported for ZIP/APK/JAR archives"))
        }

        return@withContext try {
            var testedCount = 0
            val buffer = ByteArray(BUFFER_SIZE)

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
        return FileOpProgress.formatBytes(bytes)
    }
}
