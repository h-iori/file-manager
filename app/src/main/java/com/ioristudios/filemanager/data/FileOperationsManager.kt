package com.ioristudios.filemanager.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object FileOperationsManager {

    private const val BUFFER_SIZE = 128 * 1024 // 128 KB high-throughput buffer

    suspend fun executePaste(
        sourcePaths: List<String>,
        mode: ClipboardMode,
        targetDirectoryPath: String,
        conflictStrategy: ConflictResolution = ConflictResolution.AUTO_RENAME
    ): PasteResult = withContext(Dispatchers.IO) {
        val targetDir = File(targetDirectoryPath)
        if (!targetDir.exists() || !targetDir.isDirectory) {
            return@withContext PasteResult(0, 0, sourcePaths.size, "Target directory does not exist")
        }

        var success = 0
        var skipped = 0
        var errors = 0

        for (path in sourcePaths) {
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

            val ok = if (mode == ClipboardMode.CUT) {
                moveItem(src, targetFile)
            } else {
                copyItem(src, targetFile)
            }

            if (ok) success++ else errors++
        }

        val modeVerb = if (mode == ClipboardMode.CUT) "Moved" else "Copied"
        val message = when {
            errors > 0 -> "$modeVerb $success items, $errors failed, $skipped skipped"
            skipped > 0 -> "$modeVerb $success items, $skipped skipped"
            else -> "$modeVerb $success items successfully"
        }

        PasteResult(success, skipped, errors, message)
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

    suspend fun deleteItems(files: List<File>): Result<Int> = withContext(Dispatchers.IO) {
        var count = 0
        for (f in files) {
            if (deleteRecursively(f)) {
                count++
            }
        }
        Result.success(count)
    }

    private fun resolveDestinationFile(src: File, targetDir: File, strategy: ConflictResolution): File? {
        val initialTarget = File(targetDir, src.name)
        if (!initialTarget.exists()) {
            return initialTarget
        }

        return when (strategy) {
            ConflictResolution.SKIP -> null
            ConflictResolution.OVERWRITE -> {
                deleteRecursively(initialTarget)
                initialTarget
            }
            ConflictResolution.AUTO_RENAME -> {
                val baseName = src.nameWithoutExtension
                val ext = src.extension
                var counter = 1
                var candidate: File
                do {
                    val candidateName = if (ext.isNotEmpty()) "$baseName ($counter).$ext" else "$baseName ($counter)"
                    candidate = File(targetDir, candidateName)
                    counter++
                } while (candidate.exists())
                candidate
            }
        }
    }

    private fun copyItem(src: File, dest: File): Boolean {
        return try {
            if (src.isDirectory) {
                if (!dest.exists()) dest.mkdirs()
                val children = src.listFiles() ?: return true
                var allOk = true
                for (child in children) {
                    val childDest = File(dest, child.name)
                    if (!copyItem(child, childDest)) {
                        allOk = false
                    }
                }
                allOk
            } else {
                FileInputStream(src).use { input ->
                    FileOutputStream(dest).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                        output.flush()
                    }
                }
                dest.setLastModified(src.lastModified())
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun moveItem(src: File, dest: File): Boolean {
        // Try atomic rename first
        if (src.renameTo(dest)) {
            return true
        }

        // Fallback: Copy to destination, then delete original source
        val copied = copyItem(src, dest)
        if (copied) {
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
