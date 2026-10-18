package com.ioristudios.filemanager.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

object FileOperationsManager {

    private const val BUFFER_SIZE = 256 * 1024 // 256 KB high-throughput buffer

    /**
     * Traverses all files and directories recursively to compute accurate byte totals and file counts.
     */
    fun calculateTotalBytesAndCount(paths: List<String>): Pair<Long, Int> {
        var totalBytes = 0L
        var totalFiles = 0

        fun walk(f: File) {
            if (f.isDirectory) {
                totalFiles++
                val children = f.listFiles() ?: return
                for (c in children) {
                    walk(c)
                }
            } else if (f.isFile) {
                totalFiles++
                totalBytes += f.length()
            }
        }

        for (path in paths) {
            val f = File(path)
            if (f.exists()) {
                walk(f)
            }
        }
        return Pair(totalBytes, totalFiles)
    }

    suspend fun executePaste(
        context: Context,
        sourcePaths: List<String>,
        mode: ClipboardMode,
        targetDirectoryPath: String,
        conflictStrategy: ConflictResolution = ConflictResolution.AUTO_RENAME,
        token: CancellablePauseableToken
    ): PasteResult = withContext(Dispatchers.IO) {
        val targetDir = File(targetDirectoryPath)
        if (!targetDir.exists() || !targetDir.isDirectory) {
            FileOperationController.completeFailure("Target directory does not exist")
            return@withContext PasteResult(0, 0, sourcePaths.size, "Target directory does not exist")
        }

        val (totalBytes, totalFiles) = calculateTotalBytesAndCount(sourcePaths)
        var processedBytes = 0L
        var processedFiles = 0

        var success = 0
        var skipped = 0
        var errors = 0
        val movedPaths = mutableListOf<String>()
        val scannedPaths = mutableListOf<String>()

        try {
            for (path in sourcePaths) {
                token.checkPauseAndCancel()

                val src = File(path)
                if (!src.exists()) {
                    errors++
                    continue
                }

                // Prevent pasting a directory into itself or its child
                if (src.isDirectory && targetDir.absolutePath.startsWith(src.absolutePath)) {
                    errors++
                    continue
                }

                val targetFile = resolveDestinationFile(src, targetDir, conflictStrategy)
                if (targetFile == null) {
                    skipped++
                    continue
                }

                // If moving and source is already in the target directory with the same name
                if (mode == ClipboardMode.CUT && src.canonicalPath == targetFile.canonicalPath) {
                    skipped++
                    continue
                }

                val ok = if (mode == ClipboardMode.CUT) {
                    moveItem(src, targetFile, token) { bytesDelta ->
                        processedBytes += bytesDelta
                        FileOperationController.updateProgress(
                            processedBytes = processedBytes,
                            totalBytes = totalBytes,
                            currentFileName = src.name,
                            currentFileIndex = processedFiles + 1,
                            totalFiles = totalFiles
                        )
                    }
                } else {
                    copyItem(src, targetFile, token) { bytesDelta ->
                        processedBytes += bytesDelta
                        FileOperationController.updateProgress(
                            processedBytes = processedBytes,
                            totalBytes = totalBytes,
                            currentFileName = src.name,
                            currentFileIndex = processedFiles + 1,
                            totalFiles = totalFiles
                        )
                    }
                }

                if (ok) {
                    success++
                    processedFiles++
                    scannedPaths.add(targetFile.absolutePath)
                    if (mode == ClipboardMode.CUT) {
                        movedPaths.add(path)
                    }
                } else {
                    errors++
                }
            }

            // Sync with Android MediaStore
            MediaStoreNotifier.scanFiles(context, scannedPaths)

            val modeVerb = if (mode == ClipboardMode.CUT) "Moved" else "Copied"
            val message = when {
                errors > 0 -> "$modeVerb $success items, $errors failed, $skipped skipped"
                skipped > 0 -> "$modeVerb $success items, $skipped skipped"
                else -> "$modeVerb $success items successfully"
            }

            FileOperationController.completeSuccess()
            PasteResult(success, skipped, errors, message, movedPaths)
        } catch (e: CancellationException) {
            // Clean up partially written items
            token.rollback()
            FileOperationController.cancel()
            PasteResult(success, skipped, errors + 1, "Operation cancelled", movedPaths)
        } catch (e: Exception) {
            token.rollback()
            FileOperationController.completeFailure(e.message ?: "Operation failed")
            PasteResult(success, skipped, errors + 1, "Failed: ${e.message}", movedPaths)
        }
    }

    suspend fun renameItem(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed.contains("/") || trimmed.contains("\\")) {
            return@withContext Result.failure(IllegalArgumentException("Invalid file name"))
        }

        val target = File(file.parentFile, trimmed)
        if (target.exists()) {
            return@withContext Result.failure(FileAlreadyExistsException(target, null, "A file with this name already exists"))
        }

        val success = file.renameTo(target)
        if (success) {
            Result.success(target)
        } else {
            Result.failure(IOException("Failed to rename file"))
        }
    }

    suspend fun createFolder(parentDir: File, folderName: String): Result<File> = withContext(Dispatchers.IO) {
        val trimmed = folderName.trim()
        if (trimmed.isEmpty() || trimmed.contains("/") || trimmed.contains("\\")) {
            return@withContext Result.failure(IllegalArgumentException("Invalid folder name"))
        }

        val target = File(parentDir, trimmed)
        if (target.exists()) {
            return@withContext Result.failure(FileAlreadyExistsException(target, null, "Folder already exists"))
        }

        val created = target.mkdirs()
        if (created) {
            Result.success(target)
        } else {
            Result.failure(IOException("Failed to create folder"))
        }
    }

    suspend fun createBlankFile(parentDir: File, fileName: String): Result<File> = withContext(Dispatchers.IO) {
        val trimmed = fileName.trim()
        if (trimmed.isEmpty() || trimmed.contains("/") || trimmed.contains("\\")) {
            return@withContext Result.failure(IllegalArgumentException("Invalid file name"))
        }

        val target = File(parentDir, trimmed)
        if (target.exists()) {
            return@withContext Result.failure(FileAlreadyExistsException(target, null, "File already exists"))
        }

        val created = try {
            target.createNewFile()
        } catch (e: Exception) {
            false
        }

        if (created) {
            Result.success(target)
        } else {
            Result.failure(IOException("Failed to create file"))
        }
    }

    suspend fun deleteItems(
        context: Context,
        files: List<File>,
        token: CancellablePauseableToken? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        var count = 0
        val scannedPaths = mutableListOf<String>()
        for (f in files) {
            token?.checkPauseAndCancel()
            scannedPaths.add(f.absolutePath)
            if (deleteRecursively(f)) {
                count++
            }
        }
        MediaStoreNotifier.scanFiles(context, scannedPaths)
        Result.success(count)
    }

    private fun resolveDestinationFile(src: File, targetDir: File, strategy: ConflictResolution): File? {
        val initialTarget = File(targetDir, src.name)
        if (!initialTarget.exists()) {
            return initialTarget
        }

        // Prevent deleting source if it's the identical file
        if (initialTarget.canonicalPath == src.canonicalPath) {
            return when (strategy) {
                ConflictResolution.SKIP -> null
                ConflictResolution.OVERWRITE -> initialTarget
                ConflictResolution.AUTO_RENAME -> autoRenameCandidate(src, targetDir)
            }
        }

        return when (strategy) {
            ConflictResolution.SKIP -> null
            ConflictResolution.OVERWRITE -> {
                deleteRecursively(initialTarget)
                initialTarget
            }
            ConflictResolution.AUTO_RENAME -> autoRenameCandidate(src, targetDir)
        }
    }

    private fun autoRenameCandidate(src: File, targetDir: File): File {
        val baseName = src.nameWithoutExtension
        val ext = src.extension
        var counter = 1
        var candidate: File
        do {
            val candidateName = if (ext.isNotEmpty()) "$baseName ($counter).$ext" else "$baseName ($counter)"
            candidate = File(targetDir, candidateName)
            counter++
        } while (candidate.exists())
        return candidate
    }

    private fun copyItem(
        src: File,
        dest: File,
        token: CancellablePauseableToken,
        onBytesCopied: (Long) -> Unit
    ): Boolean {
        token.checkPauseAndCancel()

        return try {
            if (src.isDirectory) {
                if (!dest.exists()) {
                    dest.mkdirs()
                    token.trackCreatedItem(dest)
                }
                val children = src.listFiles() ?: return true
                var allOk = true
                for (child in children) {
                    token.checkPauseAndCancel()
                    val childDest = File(dest, child.name)
                    if (!copyItem(child, childDest, token, onBytesCopied)) {
                        allOk = false
                    }
                }
                allOk
            } else {
                token.trackCreatedItem(dest)
                FileInputStream(src).use { input ->
                    FileOutputStream(dest).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            token.checkPauseAndCancel()
                            output.write(buffer, 0, bytesRead)
                            onBytesCopied(bytesRead.toLong())
                        }
                        output.flush()
                    }
                }
                dest.setLastModified(src.lastModified())
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    private fun moveItem(
        src: File,
        dest: File,
        token: CancellablePauseableToken,
        onBytesCopied: (Long) -> Unit
    ): Boolean {
        token.checkPauseAndCancel()

        // Same source and destination file
        if (src.canonicalPath == dest.canonicalPath) {
            return true
        }

        // Try atomic rename first (efficient when on the same filesystem mount)
        if (src.renameTo(dest)) {
            val len = if (dest.isFile) dest.length() else 0L
            if (len > 0L) {
                onBytesCopied(len)
            }
            return true
        }

        // Fallback for cross-filesystem moves: Copy with byte progress then delete source
        val copied = copyItem(src, dest, token, onBytesCopied)
        if (copied) {
            // Verify byte parity on regular files before deleting source
            if (src.isFile && dest.isFile && src.length() != dest.length()) {
                dest.delete()
                return false
            }
            deleteRecursively(src)
            return true
        }
        return false
    }

    private fun deleteRecursively(file: File): Boolean {
        if (!file.exists()) return true
        if (file.isDirectory) {
            val children = file.listFiles() ?: emptyArray()
            for (child in children) {
                deleteRecursively(child)
            }
        }
        return file.delete()
    }
}
