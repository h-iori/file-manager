package com.ioristudios.filemanager.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

data class InstalledAppItem(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val sourceApkPath: String,
    val isSystemApp: Boolean
)

object AppManager {

    suspend fun getInstalledApps(
        context: Context,
        includeSystem: Boolean = false
    ): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<InstalledAppItem>()
        val pm = context.packageManager

        try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PackageManager.ApplicationInfoFlags.of(0)
            } else 0

            val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(0)
            }

            for (app in apps) {
                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (!includeSystem && isSystem) continue

                val appName = pm.getApplicationLabel(app).toString()
                val pkgName = app.packageName
                val apkPath = app.sourceDir ?: ""
                val apkFile = File(apkPath)
                val sizeBytes = if (apkFile.exists()) apkFile.length() else 0L

                val versionName = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(0)).versionName ?: "1.0"
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageInfo(pkgName, 0).versionName ?: "1.0"
                    }
                } catch (_: Exception) {
                    "1.0"
                }

                list.add(
                    InstalledAppItem(
                        appName = appName,
                        packageName = pkgName,
                        versionName = versionName,
                        sizeBytes = sizeBytes,
                        formattedSize = formatBytes(sizeBytes),
                        sourceApkPath = apkPath,
                        isSystemApp = isSystem
                    )
                )
            }
        } catch (_: Exception) {}

        return@withContext list.sortedBy { it.appName.lowercase(Locale.ROOT) }
    }

    suspend fun extractApk(
        app: InstalledAppItem,
        targetDirectory: File = File(Environment.getExternalStorageDirectory(), "ExtractedAPKs")
    ): Result<File> = withContext(Dispatchers.IO) {
        val srcFile = File(app.sourceApkPath)
        if (!srcFile.exists()) {
            return@withContext Result.failure(IllegalStateException("Source APK file not found at ${app.sourceApkPath}"))
        }

        return@withContext try {
            if (!targetDirectory.exists()) targetDirectory.mkdirs()

            val sanitizedName = app.appName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val sanitizedVersion = app.versionName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val destFileName = "${sanitizedName}_v${sanitizedVersion}.apk"
            val destFile = File(targetDirectory, destFileName)

            val buffer = ByteArray(128 * 1024)
            FileInputStream(srcFile).use { inStream ->
                FileOutputStream(destFile).use { outStream ->
                    var read: Int
                    while (inStream.read(buffer).also { read = it } != -1) {
                        outStream.write(buffer, 0, read)
                    }
                }
            }
            Result.success(destFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
