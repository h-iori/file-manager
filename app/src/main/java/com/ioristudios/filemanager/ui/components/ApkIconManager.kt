package com.ioristudios.filemanager.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * High-performance, memory-efficient APK icon extractor and cache.
 * Dynamically resolves embedded application icons from standalone .apk files.
 */
object ApkIconManager {

    private val iconCache = LruCache<String, ImageBitmap>(150)

    fun getCached(path: String): ImageBitmap? = iconCache.get(path)

    suspend fun loadApkIcon(context: Context, path: String): ImageBitmap? {
        val cached = iconCache.get(path)
        if (cached != null) return cached

        return withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                if (!file.exists() || !file.canRead() || file.length() < 1000L) return@withContext null

                val pm = context.packageManager
                val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageArchiveInfo(path, 0)
                } ?: return@withContext null

                val appInfo = pkgInfo.applicationInfo ?: return@withContext null
                appInfo.sourceDir = path
                appInfo.publicSourceDir = path

                var drawable: Drawable? = null
                try {
                    if (appInfo.icon != 0) {
                        val res = pm.getResourcesForApplication(appInfo)
                        drawable = res.getDrawable(appInfo.icon, null)
                    }
                } catch (_: Throwable) {
                    // Fall back to loadIcon
                }

                if (drawable == null) {
                    drawable = appInfo.loadIcon(pm)
                }

                if (drawable == null) return@withContext null

                val bitmap = drawableToBitmap(drawable)
                val imageBitmap = bitmap.asImageBitmap()
                iconCache.put(path, imageBitmap)
                imageBitmap
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val bm = drawable.bitmap
            if (bm.width in 48..192 && bm.height in 48..192) {
                return bm
            }
        }
        val w = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth.coerceIn(48, 192) else 96
        val h = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight.coerceIn(48, 192) else 96
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
