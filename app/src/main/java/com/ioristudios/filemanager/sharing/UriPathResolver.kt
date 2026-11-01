package com.ioristudios.filemanager.sharing

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object UriPathResolver {

    fun resolveUriToPath(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.path
        }

        if (uri.scheme == "content") {
            if (uri.authority == "com.android.externalstorage.documents") {
                try {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":")
                    if (split.size == 2) {
                        val type = split[0]
                        val relativePath = split[1]
                        if ("primary".equals(type, ignoreCase = true)) {
                            val f = File(Environment.getExternalStorageDirectory(), relativePath)
                            if (f.exists()) return f.absolutePath
                        } else {
                            val rawPath = "/storage/$type/$relativePath"
                            val f = File(rawPath)
                            if (f.exists()) return f.absolutePath
                        }
                    }
                } catch (_: Exception) {}
            }

            // Cache fallback
            try {
                var displayName: String? = null
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex)
                        }
                    }
                }

                val finalName = displayName ?: "share_file_${System.currentTimeMillis()}"
                val tempFile = File(context.cacheDir, finalName)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (tempFile.exists()) {
                    return tempFile.absolutePath
                }
            } catch (_: Exception) {}
        }

        return null
    }
}

