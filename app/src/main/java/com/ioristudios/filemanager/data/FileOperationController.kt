package com.ioristudios.filemanager.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Global singleton controller managing active file operations, live throughput speed calculation,
 * pause, resume, and cancellation across both foreground Compose UI and background Android Services.
 */
object FileOperationController {

    private val _progressState = MutableStateFlow(FileOpProgress.IDLE)
    val progressState: StateFlow<FileOpProgress> = _progressState.asStateFlow()

    @Volatile
    private var activeToken: CancellablePauseableToken? = null

    // Speed calculation window variables
    private var speedWindowStartTime = 0L
    private var speedWindowStartBytes = 0L
    private var currentSpeed = 0L

    // Service callback hook for ongoing notifications
    private var serviceNotifier: ((FileOpProgress) -> Unit)? = null

    fun setServiceNotifier(notifier: ((FileOpProgress) -> Unit)?) {
        serviceNotifier = notifier
        // Push current state if active
        val current = _progressState.value
        if (current.isActive) {
            notifier?.invoke(current)
        }
    }

    fun startOperation(
        type: FileOpType,
        title: String,
        totalFiles: Int = 0,
        totalBytes: Long = 0L
    ): CancellablePauseableToken {
        val token = CancellablePauseableToken()
        activeToken = token

        val now = System.currentTimeMillis()
        speedWindowStartTime = now
        speedWindowStartBytes = 0L
        currentSpeed = 0L

        val initialProgress = FileOpProgress(
            operationId = UUID.randomUUID().toString(),
            type = type,
            status = FileOpStatus.RUNNING,
            title = title,
            totalFiles = totalFiles,
            totalBytes = totalBytes,
            progressFraction = 0f,
            formattedTotalBytes = FileOpProgress.formatBytes(totalBytes),
            formattedProcessedBytes = "0 B",
            formattedSpeed = "0 KB/s"
        )
        emitProgress(initialProgress)
        return token
    }

    fun updateProgress(
        processedBytes: Long,
        totalBytes: Long,
        currentFileName: String,
        currentFileIndex: Int,
        totalFiles: Int
    ) {
        val current = _progressState.value
        if (!current.isActive) return

        val now = System.currentTimeMillis()
        val elapsedMillis = now - speedWindowStartTime

        // Recalculate throughput speed every 350ms for smooth, responsive UI
        if (elapsedMillis >= 350L) {
            val bytesInWindow = (processedBytes - speedWindowStartBytes).coerceAtLeast(0L)
            currentSpeed = (bytesInWindow * 1000L) / elapsedMillis.coerceAtLeast(1L)
            speedWindowStartTime = now
            speedWindowStartBytes = processedBytes
        }

        val fraction = when {
            totalBytes > 0L -> (processedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            totalFiles > 0 -> (currentFileIndex.toFloat() / totalFiles.toFloat()).coerceIn(0f, 1f)
            else -> 0f
        }

        val etaSeconds = if (currentSpeed > 0L && totalBytes > processedBytes) {
            (totalBytes - processedBytes) / currentSpeed
        } else {
            -1L
        }

        val updated = current.copy(
            status = if (activeToken?.isPaused == true) FileOpStatus.PAUSED else FileOpStatus.RUNNING,
            currentFileName = currentFileName,
            currentFileIndex = currentFileIndex,
            totalFiles = totalFiles,
            processedBytes = processedBytes,
            totalBytes = totalBytes,
            progressFraction = fraction,
            bytesPerSecond = currentSpeed,
            formattedSpeed = FileOpProgress.formatSpeed(currentSpeed),
            formattedProcessedBytes = FileOpProgress.formatBytes(processedBytes),
            formattedTotalBytes = FileOpProgress.formatBytes(totalBytes),
            etaSeconds = etaSeconds,
            formattedEta = FileOpProgress.formatEta(etaSeconds)
        )
        emitProgress(updated)
    }

    fun pause() {
        val token = activeToken ?: return
        token.pause()
        val current = _progressState.value
        if (current.isActive) {
            emitProgress(current.copy(status = FileOpStatus.PAUSED, formattedSpeed = "Paused"))
        }
    }

    fun resume() {
        val token = activeToken ?: return
        token.resume()
        val current = _progressState.value
        if (current.status == FileOpStatus.PAUSED) {
            speedWindowStartTime = System.currentTimeMillis()
            speedWindowStartBytes = current.processedBytes
            emitProgress(current.copy(status = FileOpStatus.RUNNING))
        }
    }

    fun cancel() {
        val token = activeToken ?: return
        token.cancel()
        val current = _progressState.value
        emitProgress(current.copy(status = FileOpStatus.CANCELLED, formattedSpeed = "Cancelled"))
    }

    fun completeSuccess() {
        val current = _progressState.value
        emitProgress(
            current.copy(
                status = FileOpStatus.COMPLETED,
                progressFraction = 1f,
                formattedSpeed = "Complete",
                processedBytes = current.totalBytes
            )
        )
        activeToken = null
    }

    fun completeFailure(errorMessage: String) {
        val current = _progressState.value
        emitProgress(
            current.copy(
                status = FileOpStatus.FAILED,
                errorMessage = errorMessage,
                formattedSpeed = "Failed"
            )
        )
        activeToken = null
    }

    fun reset() {
        activeToken = null
        emitProgress(FileOpProgress.IDLE)
    }

    private fun emitProgress(progress: FileOpProgress) {
        _progressState.value = progress
        serviceNotifier?.invoke(progress)
    }
}
