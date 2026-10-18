package com.ioristudios.filemanager.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoCameraBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.filemanager.data.DirectoryItem
import com.ioristudios.filemanager.data.DirectoryVisualType
import com.ioristudios.filemanager.data.ProtectionLevel
import com.ioristudios.filemanager.theme.FilesTheme
import java.util.Locale

data class ItemVisuals(
    val icon: ImageVector,
    val iconTint: Color,
    val bgBrush: Brush,
    val borderColor: Color,
    val badgeText: String? = null,
    val badgeBgColor: Color = Color.Transparent,
    val isProtected: Boolean = false,
    val protectionLevel: ProtectionLevel = ProtectionLevel.NONE
)

object FileVisualsManager {

    fun getItemVisuals(item: DirectoryItem, isDark: Boolean): ItemVisuals {
        val fileName = item.name.lowercase(Locale.ROOT)
        val ext = item.name.substringAfterLast('.', "").uppercase(Locale.ROOT)

        // Special override for direct APK files
        if (fileName.endsWith(".apk") || item.visualType == DirectoryVisualType.FILE_APK) {
            val tint = Color(0xFF10B981)
            return ItemVisuals(
                icon = Icons.Default.Android,
                iconTint = tint,
                bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.08f))),
                borderColor = tint.copy(alpha = 0.40f),
                badgeText = "APK",
                badgeBgColor = tint
            )
        }

        // Special override for direct archives
        if (item.visualType == DirectoryVisualType.FILE_ARCHIVE || item.visualType == DirectoryVisualType.FOLDER_ARCHIVES) {
            val tint = Color(0xFFF59E0B)
            return ItemVisuals(
                icon = Icons.Default.Archive,
                iconTint = tint,
                bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.08f))),
                borderColor = tint.copy(alpha = 0.40f),
                badgeText = if (item.isDirectory) null else if (ext.length in 2..4) ext else "ZIP",
                badgeBgColor = tint
            )
        }

        return when (item.visualType) {
            // App Specific Folders
            DirectoryVisualType.FOLDER_WHATSAPP -> {
                val tint = Color(0xFF25D366) // Official WhatsApp Green
                ItemVisuals(
                    icon = BrandIcons.WhatsApp,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(Color(0xFF075E54).copy(alpha = 0.35f), tint.copy(alpha = 0.12f))),
                    borderColor = tint.copy(alpha = 0.50f)
                )
            }
            DirectoryVisualType.FOLDER_TELEGRAM, DirectoryVisualType.SEND -> {
                val tint = Color(0xFF26A5E4) // Official Telegram Sky Blue
                ItemVisuals(
                    icon = BrandIcons.Telegram,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(Color(0xFF0284C7).copy(alpha = 0.35f), tint.copy(alpha = 0.12f))),
                    borderColor = tint.copy(alpha = 0.50f)
                )
            }
            DirectoryVisualType.FOLDER_INSTAGRAM -> {
                val tint = Color(0xFFE4405F) // Official Instagram Magenta
                ItemVisuals(
                    icon = BrandIcons.Instagram,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(Color(0xFF833AB4).copy(alpha = 0.35f), Color(0xFFFD1D1D).copy(alpha = 0.20f))),
                    borderColor = tint.copy(alpha = 0.50f)
                )
            }
            DirectoryVisualType.FOLDER_FACEBOOK -> {
                val tint = Color(0xFF1877F2) // Official Facebook Blue
                ItemVisuals(
                    icon = BrandIcons.Facebook,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.30f), tint.copy(alpha = 0.10f))),
                    borderColor = tint.copy(alpha = 0.45f)
                )
            }
            DirectoryVisualType.FOLDER_TWITTER -> {
                val tint = Color(0xFFF4F4F5)
                ItemVisuals(
                    icon = BrandIcons.X,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(Color(0xFF27272A).copy(alpha = 0.50f), Color(0xFF18181B).copy(alpha = 0.30f))),
                    borderColor = Color(0xFF71717A).copy(alpha = 0.50f)
                )
            }
            DirectoryVisualType.FOLDER_SNAPCHAT -> {
                val tint = Color(0xFFFFFC00) // Official Snapchat Yellow
                ItemVisuals(
                    icon = BrandIcons.Snapchat,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(Color(0xFF78350F).copy(alpha = 0.35f), Color(0xFFF59E0B).copy(alpha = 0.12f))),
                    borderColor = tint.copy(alpha = 0.50f)
                )
            }
            DirectoryVisualType.FOLDER_TIKTOK -> {
                val tint = Color(0xFF00F2FE)
                ItemVisuals(
                    icon = BrandIcons.TikTok,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(Color(0xFFFE2C55).copy(alpha = 0.25f), tint.copy(alpha = 0.15f))),
                    borderColor = tint.copy(alpha = 0.45f)
                )
            }
            DirectoryVisualType.FOLDER_YOUTUBE -> {
                val tint = Color(0xFFFF0000) // Official YouTube Red
                ItemVisuals(
                    icon = BrandIcons.YouTube,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(Color(0xFF991B1B).copy(alpha = 0.35f), tint.copy(alpha = 0.12f))),
                    borderColor = tint.copy(alpha = 0.50f)
                )
            }
            DirectoryVisualType.FOLDER_DISCORD -> {
                val tint = Color(0xFF5865F2) // Official Discord Blurple
                ItemVisuals(
                    icon = BrandIcons.Discord,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.35f), tint.copy(alpha = 0.10f))),
                    borderColor = tint.copy(alpha = 0.50f)
                )
            }
            DirectoryVisualType.FOLDER_SPOTIFY -> {
                val tint = Color(0xFF1DB954) // Official Spotify Green
                ItemVisuals(
                    icon = BrandIcons.Spotify,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(Color(0xFF065F46).copy(alpha = 0.35f), tint.copy(alpha = 0.12f))),
                    borderColor = tint.copy(alpha = 0.50f)
                )
            }
            DirectoryVisualType.FOLDER_REDDIT -> {
                val tint = Color(0xFFFF4500) // Official Reddit Orange
                ItemVisuals(
                    icon = BrandIcons.Reddit,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.35f), tint.copy(alpha = 0.10f))),
                    borderColor = tint.copy(alpha = 0.50f)
                )
            }
            DirectoryVisualType.FOLDER_NETFLIX -> {
                val tint = Color(0xFFE50914) // Official Netflix Red
                ItemVisuals(
                    icon = BrandIcons.Netflix,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.35f), Color(0xFF18181B).copy(alpha = 0.25f))),
                    borderColor = tint.copy(alpha = 0.50f)
                )
            }
            DirectoryVisualType.FOLDER_GITHUB -> {
                val tint = Color.White
                ItemVisuals(
                    icon = BrandIcons.GitHub,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(Color(0xFF27272A).copy(alpha = 0.50f), Color(0xFF09090B).copy(alpha = 0.40f))),
                    borderColor = Color(0xFF71717A).copy(alpha = 0.50f)
                )
            }
            DirectoryVisualType.FOLDER_GOOGLE_DRIVE -> {
                val tint = Color(0xFF34A853) // Google Drive Green/Blue
                ItemVisuals(
                    icon = BrandIcons.GoogleDrive,
                    iconTint = Color(0xFF4ADE80),
                    bgBrush = Brush.linearGradient(listOf(Color(0xFF1E3A8A).copy(alpha = 0.30f), tint.copy(alpha = 0.15f))),
                    borderColor = tint.copy(alpha = 0.50f)
                )
            }

            // System Protected Folders
            DirectoryVisualType.FOLDER_PROTECTED -> {
                val tint = if (item.protectionLevel == ProtectionLevel.SYSTEM_CORE_LOCKED) Color(0xFFEF4444) else Color(0xFFF59E0B)
                ItemVisuals(
                    icon = Icons.Default.Shield,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.30f), tint.copy(alpha = 0.10f))),
                    borderColor = tint.copy(alpha = 0.55f),
                    isProtected = true,
                    protectionLevel = item.protectionLevel
                )
            }
            DirectoryVisualType.FOLDER_ANDROID, DirectoryVisualType.ANDROID -> {
                val tint = Color(0xFF10B981)
                ItemVisuals(
                    icon = Icons.Default.Android,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.45f),
                    isProtected = item.isProtected,
                    protectionLevel = item.protectionLevel
                )
            }

            // Standard Categories
            DirectoryVisualType.FOLDER_CAMERA, DirectoryVisualType.CAMERA -> {
                val tint = Color(0xFFA855F7)
                ItemVisuals(
                    icon = Icons.Default.PhotoCameraBack,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f)
                )
            }
            DirectoryVisualType.FOLDER_SCREENSHOTS -> {
                val tint = Color(0xFFC084FC)
                ItemVisuals(
                    icon = Icons.Default.Screenshot,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f)
                )
            }
            DirectoryVisualType.FOLDER_PICTURES, DirectoryVisualType.IMAGE -> {
                val tint = Color(0xFF06B6D4)
                ItemVisuals(
                    icon = Icons.Default.Image,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f)
                )
            }
            DirectoryVisualType.FOLDER_DOWNLOADS, DirectoryVisualType.DOWNLOAD -> {
                val tint = Color(0xFFF97316)
                ItemVisuals(
                    icon = Icons.Outlined.Download,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f)
                )
            }
            DirectoryVisualType.FOLDER_DOCUMENTS, DirectoryVisualType.DOCUMENT -> {
                val tint = Color(0xFF3B82F6)
                ItemVisuals(
                    icon = Icons.Default.Description,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f)
                )
            }
            DirectoryVisualType.FOLDER_MOVIES, DirectoryVisualType.MOVIE -> {
                val tint = Color(0xFF8B5CF6)
                ItemVisuals(
                    icon = Icons.Default.Movie,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f)
                )
            }
            DirectoryVisualType.FOLDER_MUSIC, DirectoryVisualType.HEADPHONES -> {
                val tint = Color(0xFF14B8A6)
                ItemVisuals(
                    icon = Icons.Default.Headphones,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f)
                )
            }
            DirectoryVisualType.FOLDER_RECORDINGS, DirectoryVisualType.MIC -> {
                val tint = Color(0xFFE11D48)
                ItemVisuals(
                    icon = Icons.Default.Mic,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f)
                )
            }
            DirectoryVisualType.FOLDER_PODCASTS, DirectoryVisualType.PODCASTS -> {
                val tint = Color(0xFF9333EA)
                ItemVisuals(
                    icon = Icons.Default.Podcasts,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f)
                )
            }
            DirectoryVisualType.FOLDER_BLUETOOTH -> {
                val tint = Color(0xFF4F46E5)
                ItemVisuals(
                    icon = Icons.Default.Bluetooth,
                    iconTint = Color(0xFF818CF8),
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f)
                )
            }
            DirectoryVisualType.FOLDER_CODE -> {
                val tint = Color(0xFF10B981)
                ItemVisuals(
                    icon = Icons.Default.Code,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f)
                )
            }
            DirectoryVisualType.NOTIFICATIONS -> {
                val tint = Color(0xFF64748B)
                ItemVisuals(
                    icon = Icons.Default.NotificationsActive,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f)
                )
            }
            DirectoryVisualType.BACKUP -> {
                val tint = Color(0xFF6366F1)
                ItemVisuals(
                    icon = Icons.Default.Backup,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f)
                )
            }
            DirectoryVisualType.FOLDER -> {
                val tint = if (isDark) Color(0xFFA855F7) else Color(0xFFF59E0B)
                ItemVisuals(
                    icon = Icons.Default.Folder,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.20f), tint.copy(alpha = 0.06f))),
                    borderColor = tint.copy(alpha = 0.35f),
                    isProtected = item.isProtected,
                    protectionLevel = item.protectionLevel
                )
            }

            // File Types Library
            DirectoryVisualType.FILE_PDF -> {
                val tint = Color(0xFFEF4444)
                ItemVisuals(
                    icon = Icons.Default.PictureAsPdf,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f),
                    badgeText = "PDF",
                    badgeBgColor = tint
                )
            }
            DirectoryVisualType.FILE_WORD -> {
                val tint = Color(0xFF2563EB)
                ItemVisuals(
                    icon = Icons.Default.Description,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f),
                    badgeText = "DOC",
                    badgeBgColor = tint
                )
            }
            DirectoryVisualType.FILE_EXCEL -> {
                val tint = Color(0xFF16A34A)
                ItemVisuals(
                    icon = Icons.Default.TableChart,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f),
                    badgeText = "XLS",
                    badgeBgColor = tint
                )
            }
            DirectoryVisualType.FILE_POWERPOINT -> {
                val tint = Color(0xFFEA580C)
                ItemVisuals(
                    icon = Icons.Default.Slideshow,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f),
                    badgeText = "PPT",
                    badgeBgColor = tint
                )
            }
            DirectoryVisualType.FILE_CODE -> {
                val tint = Color(0xFF06B6D4)
                ItemVisuals(
                    icon = Icons.Default.Code,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f),
                    badgeText = if (ext.length in 1..4) ext else "DEV",
                    badgeBgColor = tint
                )
            }
            DirectoryVisualType.FILE_IMAGE -> {
                val tint = Color(0xFFEC4899)
                ItemVisuals(
                    icon = Icons.Default.Image,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f)
                )
            }
            DirectoryVisualType.FILE_VIDEO -> {
                val tint = Color(0xFF8B5CF6)
                ItemVisuals(
                    icon = Icons.Default.Movie,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f)
                )
            }
            DirectoryVisualType.FILE_AUDIO -> {
                val tint = Color(0xFF14B8A6)
                ItemVisuals(
                    icon = Icons.Default.Headphones,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f)
                )
            }
            DirectoryVisualType.FILE_DATABASE -> {
                val tint = Color(0xFF6366F1)
                ItemVisuals(
                    icon = Icons.Default.Storage,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f),
                    badgeText = "DB",
                    badgeBgColor = tint
                )
            }
            DirectoryVisualType.FILE_FONT -> {
                val tint = Color(0xFFFBBF24)
                ItemVisuals(
                    icon = Icons.Default.FontDownload,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f),
                    badgeText = "FONT",
                    badgeBgColor = tint
                )
            }
            DirectoryVisualType.FILE_TORRENT -> {
                val tint = Color(0xFF0284C7)
                ItemVisuals(
                    icon = Icons.Outlined.Download,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.08f))),
                    borderColor = tint.copy(alpha = 0.40f),
                    badgeText = "TOR",
                    badgeBgColor = tint
                )
            }
            DirectoryVisualType.FILE_SYSTEM -> {
                val tint = Color(0xFF64748B)
                ItemVisuals(
                    icon = Icons.Default.Settings,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.20f), tint.copy(alpha = 0.06f))),
                    borderColor = tint.copy(alpha = 0.35f),
                    badgeText = if (ext.length in 2..4) ext else "BIN",
                    badgeBgColor = tint
                )
            }
            DirectoryVisualType.FILE_TEXT -> {
                val tint = Color(0xFF94A3B8)
                ItemVisuals(
                    icon = Icons.Default.Description,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.20f), tint.copy(alpha = 0.06f))),
                    borderColor = tint.copy(alpha = 0.35f),
                    badgeText = if (ext.length in 2..4) ext else "TXT",
                    badgeBgColor = tint
                )
            }
            DirectoryVisualType.FILE_GENERIC -> {
                val tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                ItemVisuals(
                    icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                    iconTint = tint,
                    bgBrush = Brush.linearGradient(listOf(tint.copy(alpha = 0.18f), tint.copy(alpha = 0.05f))),
                    borderColor = tint.copy(alpha = 0.30f),
                    badgeText = if (ext.isNotEmpty() && ext.length in 1..4) ext else null,
                    badgeBgColor = tint
                )
            }
        }
    }
}

@Composable
fun FileIconSquircle(
    item: DirectoryItem,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp
) {
    val isDark = FilesTheme.isDark
    val visuals = FileVisualsManager.getItemVisuals(item, isDark)
    val context = LocalContext.current

    val isApk = !item.isDirectory && (item.visualType == DirectoryVisualType.FILE_APK || item.name.endsWith(".apk", ignoreCase = true))
    var apkBitmap by remember(item.path) {
        mutableStateOf(if (isApk) ApkIconManager.getCached(item.path) else null)
    }

    if (isApk && apkBitmap == null) {
        LaunchedEffect(item.path) {
            apkBitmap = ApkIconManager.loadApkIcon(context, item.path)
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Main Squircle Body
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(13.dp))
                .background(visuals.bgBrush)
                .border(1.dp, visuals.borderColor, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (apkBitmap != null) {
                Image(
                    bitmap = apkBitmap!!,
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size((size.value * 0.70f).dp)
                        .clip(RoundedCornerShape(7.dp))
                )
            } else {
                Icon(
                    imageVector = visuals.icon,
                    contentDescription = null,
                    tint = visuals.iconTint,
                    modifier = Modifier.size((size.value * 0.52f).dp)
                )
            }

            // Bottom-right file extension badge (PDF, XLS, DOC, ZIP, APK, etc.)
            if (visuals.badgeText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(visuals.badgeBgColor)
                        .padding(horizontal = 3.5.dp, vertical = 0.5.dp)
                ) {
                    Text(
                        text = visuals.badgeText,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 10.sp
                    )
                }
            }
        }

        // Top-right Security Shield / Lock badge for sensitive core directories
        if (visuals.isProtected || item.isProtected) {
            val shieldColor = if (visuals.protectionLevel == ProtectionLevel.SYSTEM_CORE_LOCKED) {
                Color(0xFFEF4444) // Crimson for root/system core
            } else {
                Color(0xFFF59E0B) // Amber for Scoped Storage (Android/data, obb)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 3.dp, y = (-3).dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F0B1A))
                    .border(1.dp, shieldColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Protected Directory",
                    tint = shieldColor,
                    modifier = Modifier.size(9.dp)
                )
            }
        }
    }
}
