package com.ioristudios.filemanager.sharing.resilience

import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Transfer action control commands for pause, continue/resume, cancel, and retry.
 */
enum class TransferControlAction {
    PAUSE,
    RESUME,
    CANCEL,
    RETRY
}

/**
 * Discrete lifecycle states for an active P2P transfer session.
 */
enum class TransferLifecycleStatus {
    IDLE,
    DISCOVERING,
    CONNECTING,
    HANDSHAKING,
    WAITING_APPROVAL,
    TRANSFERRING,
    PAUSED,
    FAILED,
    COMPLETED,
    CANCELLED
}

/**
 * Snapshot of the live transfer progress and metrics.
 */
data class TransferProgressSnapshot(
    val status: TransferLifecycleStatus = TransferLifecycleStatus.IDLE,
    val currentFileName: String = "",
    val fileIndex: Int = 0,
    val totalFiles: Int = 0,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val speedFormatted: String = "0 B/s",
    val etaSeconds: Long = 0L,
    val etaFormatted: String = "--",
    val percent: Float = 0f,
    val errorMessage: String? = null,
    val canResume: Boolean = false
)

/**
 * Manages transfer resilience: tracking byte offsets, handling pause/resume,
 * progressive SHA-256 verification, and cancellation.
 */
class TransferSessionManager {

    private val _progress = MutableStateFlow(TransferProgressSnapshot())
    val progress: StateFlow<TransferProgressSnapshot> = _progress.asStateFlow()

    @Volatile
    var isPaused: Boolean = false
        private set

    @Volatile
    var isCancelled: Boolean = false
        private set

    private var lastSpeedCalcTime = 0L
    private var lastBytesForSpeed = 0L

    fun reset() {
        isPaused = false
        isCancelled = false
        lastSpeedCalcTime = 0L
        lastBytesForSpeed = 0L
        _progress.value = TransferProgressSnapshot()
    }

    fun updateStatus(status: TransferLifecycleStatus, errorMessage: String? = null, canResume: Boolean = false) {
        _progress.value = _progress.value.copy(
            status = status,
            errorMessage = errorMessage,
            canResume = canResume
        )
    }

    fun pauseTransfer() {
        if (_progress.value.status == TransferLifecycleStatus.TRANSFERRING) {
            isPaused = true
            updateStatus(TransferLifecycleStatus.PAUSED, canResume = true)
        }
    }

    fun resumeTransfer() {
        if (_progress.value.status == TransferLifecycleStatus.PAUSED || _progress.value.canResume) {
            isPaused = false
            updateStatus(TransferLifecycleStatus.TRANSFERRING)
        }
    }

    fun cancelTransfer() {
        isCancelled = true
        isPaused = false
        updateStatus(TransferLifecycleStatus.CANCELLED)
    }

    fun markFailed(error: String, canResume: Boolean = true) {
        isPaused = false
        updateStatus(TransferLifecycleStatus.FAILED, errorMessage = error, canResume = canResume)
    }

    fun markCompleted() {
        isPaused = false
        isCancelled = false
        updateStatus(TransferLifecycleStatus.COMPLETED)
    }

    fun updateMetrics(
        fileName: String,
        fileIndex: Int,
        totalFiles: Int,
        transferredBytes: Long,
        totalBytes: Long
    ) {
        val now = System.currentTimeMillis()
        var speedBytes = _progress.value.speedBytesPerSec
        var eta = _progress.value.etaSeconds

        if (lastSpeedCalcTime == 0L) {
            lastSpeedCalcTime = now
            lastBytesForSpeed = transferredBytes
        } else {
            val deltaMs = now - lastSpeedCalcTime
            if (deltaMs >= 500) { // update speed every 500ms
                val deltaBytes = transferredBytes - lastBytesForSpeed
                if (deltaBytes >= 0) {
                    speedBytes = (deltaBytes * 1000L) / deltaMs
                    if (speedBytes > 0 && totalBytes > transferredBytes) {
                        eta = (totalBytes - transferredBytes) / speedBytes
                    }
                }
                lastSpeedCalcTime = now
                lastBytesForSpeed = transferredBytes
            }
        }

        val percent = if (totalBytes > 0) {
            (transferredBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else 0f

        _progress.value = _progress.value.copy(
            currentFileName = fileName,
            fileIndex = fileIndex,
            totalFiles = totalFiles,
            bytesTransferred = transferredBytes,
            totalBytes = totalBytes,
            speedBytesPerSec = speedBytes,
            speedFormatted = formatSpeed(speedBytes),
            etaSeconds = eta,
            etaFormatted = formatEta(eta),
            percent = percent
        )
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
            bytesPerSec >= 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
            else -> "$bytesPerSec B/s"
        }
    }

    private fun formatEta(seconds: Long): String {
        return when {
            seconds <= 0 -> "--"
            seconds < 60 -> "${seconds}s left"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s left"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m left"
        }
    }
}

/**
 * Checksum verifier and temp file resume manager.
 */
object DataIntegrityVerifier {

    fun computeSha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            var read: Int
            while (input.read(buf).also { read = it } != -1) {
                md.update(buf, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    fun getTempResumeFile(cacheDir: File, payloadId: Long): File {
        return File(cacheDir, ".share_tmp_${payloadId}.part")
    }

    fun getResumeOffset(tempFile: File): Long {
        return if (tempFile.exists()) tempFile.length() else 0L
    }
}

