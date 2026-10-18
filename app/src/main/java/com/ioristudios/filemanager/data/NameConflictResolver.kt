package com.ioristudios.filemanager.data

import java.io.File

/**
 * Enterprise-grade Name Conflict Resolver.
 * Handles intelligent auto-renaming, collision detection, and sequence numbering:
 * - Differentiates directories (which may contain dots) from files with extensions.
 * - Parses existing sequence numbers: "document (1).txt" -> "document (2).txt" (avoids "document (1) (1).txt").
 * - Provides non-colliding destination resolution for copy, move, archive extraction, and file/folder creation.
 */
object NameConflictResolver {

    private val DUPLICATE_PATTERN = Regex("""^(.*)\s\((\d+)\)$""")

    /**
     * Splits a file or folder name into its logical base name and extension.
     * For directories, the entire name is considered the base name (even if it contains dots).
     * For files, splits at the last dot (if any).
     */
    fun splitNameAndExtension(name: String, isDirectory: Boolean): Pair<String, String> {
        if (isDirectory) {
            return Pair(name, "")
        }
        val dotIndex = name.lastIndexOf('.')
        return if (dotIndex > 0 && dotIndex < name.length - 1) {
            Pair(name.substring(0, dotIndex), name.substring(dotIndex + 1))
        } else {
            Pair(name, "")
        }
    }

    /**
     * Resolves a non-colliding destination file in [targetDir].
     * If [desiredName] does not exist, returns File(targetDir, desiredName).
     * If it exists, intelligently increments:
     * - "file.txt" -> "file (1).txt"
     * - "file (1).txt" -> "file (2).txt"
     * - "folder.v1" (dir) -> "folder.v1 (1)"
     * - "folder (2)" (dir) -> "folder (3)"
     */
    fun resolveUniqueFile(targetDir: File, desiredName: String, isDirectory: Boolean): File {
        val initial = File(targetDir, desiredName)
        if (!initial.exists()) {
            return initial
        }

        val (rawBase, ext) = splitNameAndExtension(desiredName, isDirectory)
        val match = DUPLICATE_PATTERN.matchEntire(rawBase)
        val (cleanBase, startCounter) = if (match != null) {
            val prefix = match.groupValues[1]
            val currentNum = match.groupValues[2].toIntOrNull() ?: 1
            Pair(prefix, currentNum + 1)
        } else {
            Pair(rawBase, 1)
        }

        var counter = startCounter
        while (true) {
            val candidateName = if (ext.isNotEmpty()) "$cleanBase ($counter).$ext" else "$cleanBase ($counter)"
            val candidate = File(targetDir, candidateName)
            if (!candidate.exists()) {
                return candidate
            }
            counter++
        }
    }

    /**
     * Suggests the next available name given a set of existing sibling names.
     */
    fun suggestAvailableName(baseName: String, isDirectory: Boolean, existingNames: Set<String>): String {
        val existsExact = existingNames.any { it.equals(baseName, ignoreCase = true) }
        if (!existsExact) return baseName

        val (rawBase, ext) = splitNameAndExtension(baseName, isDirectory)
        val match = DUPLICATE_PATTERN.matchEntire(rawBase)
        val (cleanBase, startCounter) = if (match != null) {
            val prefix = match.groupValues[1]
            val currentNum = match.groupValues[2].toIntOrNull() ?: 1
            Pair(prefix, currentNum + 1)
        } else {
            Pair(rawBase, 1)
        }

        var counter = startCounter
        while (true) {
            val candidateName = if (ext.isNotEmpty()) "$cleanBase ($counter).$ext" else "$cleanBase ($counter)"
            if (existingNames.none { it.equals(candidateName, ignoreCase = true) }) {
                return candidateName
            }
            counter++
        }
    }
}
