package com.ioristudios.filemanager.sharing

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Resolves URIs handed to us by other apps (ACTION_SEND / SEND_MULTIPLE) into local files.
 *
 * Everything in an incoming share intent is attacker-controlled, so this class defends against:
 * - "Dirty Stream" path traversal: a provider returning DISPLAY_NAME "../shared_prefs/x.xml"
 *   to make us overwrite our own private files. Names are reduced to a bare, safe file name and
 *   every copy lands in a fresh per-share folder whose canonical path is verified.
 * - Confused deputy: file:// URIs, crafted ExternalStorage document ids ("primary:../../data/..")
 *   or content:// URIs pointing at our own FileProvider, used to trick us into reading and
 *   sending this app's private data to another device.
 */
object UriPathResolver {

    private const val TAG = "UriPathResolver"
    private const val INCOMING_DIR = "incoming_share"
    private const val MAX_NAME_CHARS = 150

    fun resolveUriToPath(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            return File(path).takeIf { isSafeSourceFile(context, it) }?.absolutePath
        }

        if (uri.scheme == "content") {
            // Never let another app read our private files through our own FileProvider
            if (uri.authority == "${context.packageName}.fileprovider") {
                Log.w(TAG, "Refused share URI pointing at this app's own FileProvider")
                return null
            }

            if (uri.authority == "com.android.externalstorage.documents") {
                try {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":", limit = 2)
                    if (split.size == 2) {
                        val type = split[0]
                        val relativePath = split[1]
                        val root = if ("primary".equals(type, ignoreCase = true)) {
                            Environment.getExternalStorageDirectory()
                        } else {
                            File("/storage/$type")
                        }
                        val f = File(root, relativePath)
                        if (f.exists() && isInside(f, root) && isSafeSourceFile(context, f)) {
                            return f.absolutePath
                        }
                    }
                } catch (_: Exception) {}
            }

            // Cache fallback: copy the stream into a fresh, private per-share folder
            try {
                var displayName: String? = null
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            displayName = cursor.getString(nameIndex)
                        }
                    }
                }

                val incomingRoot = File(context.cacheDir, INCOMING_DIR)
                val shareDir = File(incomingRoot, UUID.randomUUID().toString())
                if (!shareDir.mkdirs() && !shareDir.isDirectory) return null

                val finalName = sanitizeDisplayName(displayName)
                val tempFile = File(shareDir, finalName)
                if (!isInside(tempFile, shareDir) || tempFile.canonicalFile.parentFile != shareDir.canonicalFile) {
                    Log.w(TAG, "Refused unsafe display name from share intent")
                    return null
                }

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                if (tempFile.exists()) {
                    return tempFile.absolutePath
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to import shared content: ${e.message}")
            }
        }

        return null
    }

    /** Reduces an untrusted DISPLAY_NAME to a bare file name with no path or hidden-file tricks. */
    internal fun sanitizeDisplayName(raw: String?): String {
        var name = (raw ?: "").substringAfterLast('/').substringAfterLast('\\')
        name = name.filter { ch ->
            !Character.isISOControl(ch) &&
                Character.getType(ch) != Character.FORMAT.toInt() &&
                ch !in "\\/:*?\"<>|"
        }.trim().trimStart('.').trim()
        if (name.isEmpty()) name = "share_file_${System.currentTimeMillis()}"
        if (name.length > MAX_NAME_CHARS) {
            val ext = name.substringAfterLast('.', "").take(16)
            val base = if (ext.isEmpty()) name else name.substringBeforeLast('.')
            val keep = MAX_NAME_CHARS - (if (ext.isEmpty()) 0 else ext.length + 1)
            name = base.take(keep) + (if (ext.isEmpty()) "" else ".$ext")
        }
        return name
    }

    /**
     * A file another app points us at may be sent to a remote device, so it must be a real,
     * readable regular file that is not inside this app's private storage (internal or the
     * app-specific external directory).
     */
    private fun isSafeSourceFile(context: Context, file: File): Boolean {
        val canonical = try { file.canonicalFile } catch (_: Exception) { return false }
        if (!canonical.isFile || !canonical.canRead()) return false

        val privateRoots = buildList {
            add(File(context.applicationInfo.dataDir))
            context.filesDir?.parentFile?.let { add(it) }
            context.getExternalFilesDir(null)?.parentFile?.let { add(it) } // Android/data/<pkg>
            context.externalCacheDir?.parentFile?.let { add(it) }
            add(File("/data/data/${context.packageName}"))
            add(File("/data/user/0/${context.packageName}"))
        }
        if (privateRoots.any { isInside(canonical, it) }) {
            Log.w(TAG, "Refused share of a file inside this app's private storage")
            return false
        }
        return true
    }

    /** True when [file] resolves (after following symlinks and "..") to a path inside [dir]. */
    private fun isInside(file: File, dir: File): Boolean = try {
        val f = file.canonicalPath
        val d = dir.canonicalPath
        f == d || f.startsWith(d.trimEnd('/') + "/")
    } catch (_: Exception) {
        false
    }
}
