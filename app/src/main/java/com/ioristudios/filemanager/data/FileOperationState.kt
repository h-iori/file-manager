package com.ioristudios.filemanager.data

import androidx.compose.runtime.Immutable
import java.util.Locale

enum class FileOpType(val verb: String) {
    COPY("Copying"),
    MOVE("Moving"),
    DELETE("Deleting"),
    COMPRESS("Compressing"),
    EXTRACT("Extracting")
}

enum class FileOpStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    CANCELLED,
    FAILED
}

@Immutable
data class FileOpProgress(
    val operationId: String = "",
    val type: FileOpType = FileOpType.COPY,
    val status: FileOpStatus = FileOpStatus.IDLE,
    val title: String = "",
    val currentFileName: String = "",
    val currentFileIndex: Int = 0,
    val totalFiles: Int = 0,
    val processedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val progressFraction: Float = 0f,
    val bytesPerSecond: Long = 0L,
    val formattedSpeed: String = "0 KB/s",
    val formattedProcessedBytes: String = "0 B",
    val formattedTotalBytes: String = "0 B",
    val etaSeconds: Long = -1L,
    val formattedEta: String = "",
    val errorMessage: String? = null
) {
    val isActive: Boolean
        get() = status == FileOpStatus.RUNNING || status == FileOpStatus.PAUSED

    val isIndeterminate: Boolean
        get() = totalBytes <= 0L && totalFiles <= 0

    companion object {
        val IDLE = FileOpProgress()

        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var value = bytes.toDouble()
            var unitIndex = 0
            while (value >= 1024.0 && unitIndex < units.lastIndex) {
                value /= 1024.0
                unitIndex++
            }
            return if (unitIndex == 0) {
                "${value.toLong()} B"
            } else {
                String.format(Locale.US, "%.1f %s", value, units[unitIndex])
            }
        }

        fun formatSpeed(bytesPerSec: Long): String {
            if (bytesPerSec <= 0L) return "0 KB/s"
            val kb = bytesPerSec / 1024.0
            return if (kb < 1024.0) {
                String.format(Locale.US, "%.1f KB/s", kb)
            } else {
                val mb = kb / 1024.0
                String.format(Locale.US, "%.1f MB/s", mb)
            }
        }

        fun formatEta(etaSeconds: Long): String {
            if (etaSeconds < 0L || etaSeconds > 86400L) return ""
            return when {
                etaSeconds < 60L -> "${etaSeconds}s remaining"
                etaSeconds < 3600L -> "${etaSeconds / 60}m ${etaSeconds % 60}s remaining"
                else -> "${etaSeconds / 3600}h ${(etaSeconds % 3600) / 60}m remaining"
            }
        }
    }
}
