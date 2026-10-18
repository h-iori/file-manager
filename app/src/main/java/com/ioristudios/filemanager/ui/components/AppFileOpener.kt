package com.ioristudios.filemanager.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

object AppFileOpener {

    const val ANYDOC_PACKAGE = "com.ioristudios.anydoc"

    fun openFile(
        context: Context,
        filePath: String,
        mimeType: String? = null,
        showMessage: (String) -> Unit
    ) {
        val file = File(filePath)
        if (!file.exists()) {
            showMessage("File does not exist")
            return
        }

        val uri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            showMessage("Cannot generate file URI: ${e.message}")
            return
        }

        val resolvedMime = if (!mimeType.isNullOrBlank() && mimeType != "*/*") {
            mimeType
        } else {
            getMimeType(file)
        }

        if (isDocument(file, resolvedMime)) {
            openDocument(context, uri, resolvedMime, showMessage)
        } else {
            openGeneralFile(context, uri, resolvedMime, showMessage)
        }
    }

    fun isDocument(file: File, mimeType: String): Boolean {
        val ext = file.extension.lowercase(Locale.ROOT)
        val docExtensions = setOf(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "rtf", "epub", "csv", "odt", "ods", "odp",
            "wps", "wpd", "md", "json", "xml", "log", "html", "htm",
            "c", "cpp", "java", "kt", "py", "sh", "yaml", "yml", "properties"
        )
        return ext in docExtensions ||
                mimeType.startsWith("text/") ||
                mimeType.contains("pdf") ||
                mimeType.contains("officedocument") ||
                mimeType.contains("msword") ||
                mimeType.contains("ms-excel") ||
                mimeType.contains("ms-powerpoint")
    }

    private fun openDocument(
        context: Context,
        uri: Uri,
        mimeType: String,
        showMessage: (String) -> Unit
    ) {
        val pm = context.packageManager
        val isAnyDocInstalled = try {
            pm.getPackageInfo(ANYDOC_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

        if (isAnyDocInstalled) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    setPackage(ANYDOC_PACKAGE)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                // If AnyDoc failed to start, proceed to fallback
            }
        }

        // Fallback: Open with system default document viewer or chooser
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(intent, "Open document with...")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
        } catch (e: Exception) {
            showMessage("No document viewer application available")
        }
    }

    private fun openGeneralFile(
        context: Context,
        uri: Uri,
        mimeType: String,
        showMessage: (String) -> Unit
    ) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(intent, "Open with...")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
        } catch (e: Exception) {
            showMessage("No application found to open this file: ${e.message}")
        }
    }

    fun getMimeType(file: File): String {
        val extension = file.extension.lowercase(Locale.ROOT)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"
    }
}
