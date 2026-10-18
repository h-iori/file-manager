package com.ioristudios.filemanager.data

import android.content.Context
import android.media.MediaScannerConnection
import java.io.File

/**
 * Utility to immediately index new, copied, moved or deleted media files with Android's MediaStore.
 */
object MediaStoreNotifier {

    fun scanFiles(context: Context, paths: List<String>) {
        if (paths.isEmpty()) return
        val validPaths = paths.filter { it.isNotBlank() && File(it).exists() }.toTypedArray()
        if (validPaths.isNotEmpty()) {
            try {
                MediaScannerConnection.scanFile(context.applicationContext, validPaths, null, null)
            } catch (_: Exception) {}
        }
    }

    fun scanFile(context: Context, path: String) {
        if (path.isNotBlank() && File(path).exists()) {
            try {
                MediaScannerConnection.scanFile(context.applicationContext, arrayOf(path), null, null)
            } catch (_: Exception) {}
        }
    }
}
