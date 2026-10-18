package com.ioristudios.filemanager.data

import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

/**
 * Thread-safe token for cooperative pausing, resumption, cancellation and transactional rollback
 * across file I/O operations (Copy, Move, Compress, Extract).
 */
class CancellablePauseableToken {

    private val _isPaused = AtomicBoolean(false)
    private val _isCancelled = AtomicBoolean(false)
    private val pauseLock = java.lang.Object()

    // Tracks newly created destination files/dirs for transactional rollback on cancel
    private val createdItems = ConcurrentLinkedQueue<File>()

    val isPaused: Boolean
        get() = _isPaused.get()

    val isCancelled: Boolean
        get() = _isCancelled.get()

    fun pause() {
        _isPaused.set(true)
    }

    fun resume() {
        _isPaused.set(false)
        synchronized(pauseLock) {
            pauseLock.notifyAll()
        }
    }

    fun cancel() {
        _isCancelled.set(true)
        _isPaused.set(false)
        synchronized(pauseLock) {
            pauseLock.notifyAll()
        }
    }

    /**
     * Checks if the operation was cancelled or paused.
     * If paused, halts the calling thread until resume() or cancel() is called.
     * Throws [CancellationException] if cancelled.
     */
    fun checkPauseAndCancel() {
        if (_isCancelled.get()) {
            throw CancellationException("Operation cancelled by user")
        }
        if (_isPaused.get()) {
            synchronized(pauseLock) {
                while (_isPaused.get() && !_isCancelled.get()) {
                    try {
                        pauseLock.wait(250)
                    } catch (e: InterruptedException) {
                        if (_isCancelled.get()) {
                            throw CancellationException("Operation cancelled by user")
                        }
                    }
                }
            }
            if (_isCancelled.get()) {
                throw CancellationException("Operation cancelled by user")
            }
        }
    }

    fun trackCreatedItem(file: File) {
        createdItems.add(file)
    }

    /**
     * Rolls back all partially created files and directories in reverse creation order.
     */
    fun rollback() {
        val itemsList = createdItems.toList().reversed()
        for (item in itemsList) {
            try {
                if (item.isDirectory) {
                    item.deleteRecursively()
                } else if (item.exists()) {
                    item.delete()
                }
            } catch (_: Exception) {}
        }
        createdItems.clear()
    }
}
