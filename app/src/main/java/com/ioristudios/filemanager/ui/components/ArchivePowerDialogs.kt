package com.ioristudios.filemanager.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import com.ioristudios.filemanager.theme.LocalMotionSensitivity
import com.ioristudios.filemanager.theme.MotionTokens
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Unarchive
import com.ioristudios.filemanager.data.NameConflictResolver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.filemanager.data.ArchiveType
import com.ioristudios.filemanager.data.CompressionLevel
import com.ioristudios.filemanager.data.DirectoryItem
import com.ioristudios.filemanager.data.InstalledAppItem
import com.ioristudios.filemanager.haptics.HapticType
import com.ioristudios.filemanager.haptics.LocalAppHaptics
import com.ioristudios.filemanager.theme.ElectricCyan
import com.ioristudios.filemanager.theme.ElectricLavender
import com.ioristudios.filemanager.theme.ElectricMagenta
import com.ioristudios.filemanager.theme.ElectricPurple
import com.ioristudios.filemanager.theme.ElectricTeal
import com.ioristudios.filemanager.theme.FilesTheme
import com.ioristudios.filemanager.theme.OutfitFontFamily
import com.ioristudios.filemanager.ui.directory.GlassDialogTextField
import com.ioristudios.filemanager.ui.main.MainScreenViewModel
import java.io.File

/**
 * Dialog presented when tapping an archive (.zip, .apk, .tar, .jar, etc.)
 * Offers in-place browsing, extraction options, or integrity testing.
 */
@Composable
fun ArchiveOptionsDialog(
    name: String,
    path: String,
    onBrowseInPlace: () -> Unit,
    onExtractHere: () -> Unit,
    onExtractToFolder: () -> Unit,
    onTestIntegrity: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalAppHaptics.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FilesTheme.colors.dialogBg,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF9333EA).copy(alpha = 0.25f))
                        .border(1.dp, FilesTheme.colors.primaryAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        tint = FilesTheme.colors.primaryAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Archive Actions",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = FilesTheme.colors.textPrimary
                    )
                    Text(
                        text = name,
                        fontFamily = OutfitFontFamily,
                        fontSize = 12.sp,
                        color = FilesTheme.colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ArchiveOptionTile(
                    title = "Browse Archive",
                    subtitle = "Explore inside archive without extracting",
                    icon = Icons.Default.OpenInBrowser,
                    iconTint = ElectricCyan,
                    onClick = {
                        haptics.trigger(HapticType.CLICK)
                        onBrowseInPlace()
                    }
                )

                ArchiveOptionTile(
                    title = "Extract Here",
                    subtitle = "Decompress files into current directory",
                    icon = Icons.Default.Unarchive,
                    iconTint = ElectricPurple,
                    onClick = {
                        haptics.trigger(HapticType.CLICK)
                        onExtractHere()
                    }
                )

                val folderTargetName = name.substringBeforeLast('.')
                ArchiveOptionTile(
                    title = "Extract to /$folderTargetName/",
                    subtitle = "Extract into a dedicated new subfolder",
                    icon = Icons.Default.FolderOpen,
                    iconTint = ElectricTeal,
                    onClick = {
                        haptics.trigger(HapticType.CLICK)
                        onExtractToFolder()
                    }
                )

                ArchiveOptionTile(
                    title = "Test Archive Integrity",
                    subtitle = "Verify archive headers & integrity without unpacking",
                    icon = Icons.Default.Security,
                    iconTint = ElectricMagenta,
                    onClick = {
                        haptics.trigger(HapticType.CLICK)
                        onTestIntegrity()
                    }
                )
            }
        },
        confirmButton = {
            Text(
                text = "Cancel",
                color = FilesTheme.colors.textSecondary,
                fontSize = 14.sp,
                fontFamily = OutfitFontFamily,
                modifier = Modifier
                    .elasticBounce(targetScale = 0.92f, onClick = onDismiss)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    )
}

@Composable
fun ArchiveOptionsDialog(
    item: DirectoryItem,
    onBrowseInPlace: () -> Unit,
    onExtractHere: () -> Unit,
    onExtractToFolder: () -> Unit,
    onTestIntegrity: () -> Unit,
    onDismiss: () -> Unit
) = ArchiveOptionsDialog(
    name = item.name,
    path = item.path,
    onBrowseInPlace = onBrowseInPlace,
    onExtractHere = onExtractHere,
    onExtractToFolder = onExtractToFolder,
    onTestIntegrity = onTestIntegrity,
    onDismiss = onDismiss
)

@Composable
fun ArchiveOptionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .elasticBounce(targetScale = 0.96f, hapticType = HapticType.CLICK, onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(if (FilesTheme.isDark) Color(0x18FFFFFF) else Color(0xFFF1F5F9))
            .border(1.dp, iconTint.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = FilesTheme.colors.textPrimary
            )
            Text(
                text = subtitle,
                fontFamily = OutfitFontFamily,
                fontSize = 11.5.sp,
                color = FilesTheme.colors.textMuted
            )
        }
    }
}

/**
 * Compression Dialog for creating new ZIP or TAR archives.
 */
@Composable
fun CompressDialog(
    initialName: String,
    existingNames: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onCompress: (String, ArchiveType, CompressionLevel) -> Unit
) {
    val rawDefault = if (initialName.endsWith(".zip")) initialName else "$initialName.zip"
    val suggestedInitial = remember(existingNames) {
        NameConflictResolver.suggestAvailableName(rawDefault, isDirectory = false, existingNames = existingNames)
    }
    var archiveName by remember(suggestedInitial) { mutableStateOf(suggestedInitial) }
    var selectedFormat by remember { mutableStateOf(ArchiveType.ZIP) }
    var selectedLevel by remember { mutableStateOf(CompressionLevel.NORMAL) }
    val haptics = LocalAppHaptics.current
    val trimmed = archiveName.trim()
    val isDuplicate = existingNames.any { it.equals(trimmed, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FilesTheme.colors.dialogBg,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF9333EA).copy(alpha = 0.25f))
                        .border(1.dp, FilesTheme.colors.primaryAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        tint = FilesTheme.colors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Create Archive",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FilesTheme.colors.textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Archive Name:",
                    color = FilesTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontFamily = OutfitFontFamily
                )

                GlassDialogTextField(
                    value = archiveName,
                    onValueChange = { archiveName = it },
                    placeholder = "archive_name.zip"
                )

                if (isDuplicate) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Archive exists. Will save as \"${NameConflictResolver.suggestAvailableName(trimmed, false, existingNames)}\".",
                            color = Color(0xFFF59E0B),
                            fontSize = 11.5.sp,
                            fontFamily = OutfitFontFamily
                        )
                    }
                }

                // Format Selector (ZIP vs TAR)
                Text(
                    text = "Archive Format:",
                    color = FilesTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = OutfitFontFamily
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair("ZIP (.zip)", ArchiveType.ZIP),
                        Pair("TAR (.tar)", ArchiveType.TAR)
                    ).forEach { (label, type) ->
                        val isSelected = selectedFormat == type
                        val targetBg = if (isSelected) FilesTheme.colors.primaryAccent.copy(alpha = 0.25f) else Color(0x11FFFFFF)
                        val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = tween(MotionTokens.DurationFast), label = "FormatBg")
                        val targetBorder = if (isSelected) FilesTheme.colors.primaryAccent else Color(0x22FFFFFF)
                        val animatedBorder by animateColorAsState(targetValue = targetBorder, animationSpec = tween(MotionTokens.DurationFast), label = "FormatBorder")

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .elasticBounce(
                                    targetScale = 0.94f,
                                    hapticType = HapticType.CLICK,
                                    onClick = {
                                        selectedFormat = type
                                        val base = archiveName.substringBeforeLast('.')
                                        archiveName = if (type == ArchiveType.TAR) "$base.tar" else "$base.zip"
                                    }
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .background(animatedBg)
                                .border(1.dp, animatedBorder, RoundedCornerShape(10.dp))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = OutfitFontFamily,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) FilesTheme.colors.primaryAccent else FilesTheme.colors.textSecondary
                            )
                        }
                    }
                }

                // Compression Level Selector
                Text(
                    text = "Compression Level:",
                    color = FilesTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = OutfitFontFamily
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Pair("Store", CompressionLevel.STORE),
                        Pair("Fast", CompressionLevel.FAST),
                        Pair("Normal", CompressionLevel.NORMAL),
                        Pair("Max", CompressionLevel.MAXIMUM)
                    ).forEach { (label, level) ->
                        val isSelected = selectedLevel == level
                        val targetBg = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.22f) else Color(0x11FFFFFF)
                        val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = tween(MotionTokens.DurationFast), label = "LevelBg")
                        val targetBorder = if (isSelected) ElectricCyan else Color(0x22FFFFFF)
                        val animatedBorder by animateColorAsState(targetValue = targetBorder, animationSpec = tween(MotionTokens.DurationFast), label = "LevelBorder")

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .elasticBounce(
                                    targetScale = 0.93f,
                                    hapticType = HapticType.CLICK,
                                    onClick = { selectedLevel = level }
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(animatedBg)
                                .border(1.dp, animatedBorder, RoundedCornerShape(8.dp))
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = OutfitFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) ElectricCyan else FilesTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Text(
                text = "Compress",
                color = FilesTheme.colors.primaryAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = OutfitFontFamily,
                modifier = Modifier
                    .elasticBounce(targetScale = 0.92f) {
                        if (trimmed.isNotEmpty()) {
                            val resolved = if (isDuplicate) {
                                NameConflictResolver.suggestAvailableName(trimmed, false, existingNames)
                            } else {
                                trimmed
                            }
                            onCompress(resolved, selectedFormat, selectedLevel)
                        }
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        },
        dismissButton = {
            Text(
                text = "Cancel",
                color = FilesTheme.colors.textSecondary,
                fontSize = 14.sp,
                fontFamily = OutfitFontFamily,
                modifier = Modifier
                    .elasticBounce(targetScale = 0.92f, onClick = onDismiss)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    )
}

/**
 * App Manager & APK Extractor Dialog
 * Lists installed applications and extracts their APKs to /sdcard/ExtractedAPKs/ in 1 tap.
 */
@Composable
fun AppManagerDialog(
    viewModel: MainScreenViewModel,
    showMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = LocalAppHaptics.current
    var apps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var extractingAppPkg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadInstalledApps(includeSystem = false) { list ->
            apps = list
            isLoading = false
        }
    }

    val filtered = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FilesTheme.colors.dialogBg,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.25f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "App Manager & APK Extractor",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = FilesTheme.colors.textPrimary
                    )
                    Text(
                        text = "${apps.size} installed user apps",
                        fontFamily = OutfitFontFamily,
                        fontSize = 12.sp,
                        color = FilesTheme.colors.textMuted
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search field for filtering apps
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (FilesTheme.isDark) Color(0x18FFFFFF) else Color(0xFFF1F5F9))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = FilesTheme.colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(
                            color = FilesTheme.colors.textPrimary,
                            fontSize = 13.5.sp,
                            fontFamily = OutfitFontFamily
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(FilesTheme.colors.primaryAccent),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search installed apps...",
                                    color = FilesTheme.colors.textMuted,
                                    fontSize = 13.sp,
                                    fontFamily = OutfitFontFamily
                                )
                            }
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = FilesTheme.colors.textMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ElectricPurple,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    }
                } else if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No apps found",
                            color = FilesTheme.colors.textSecondary,
                            fontSize = 13.sp,
                            fontFamily = OutfitFontFamily
                        )
                    }
                } else {
                    val motion = LocalMotionSensitivity.current
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(filtered, key = { _, it -> it.packageName }) { index, app ->
                            val staggerDelay = if (motion.isReducedMotion) 0 else (index * 15).coerceAtMost(150)
                            var isVisible by remember { mutableStateOf(motion.isReducedMotion) }
                            LaunchedEffect(Unit) {
                                if (!motion.isReducedMotion) {
                                    kotlinx.coroutines.delay(staggerDelay.toLong())
                                    isVisible = true
                                }
                            }
                            val alpha by animateFloatAsState(
                                targetValue = if (isVisible) 1f else 0f,
                                animationSpec = MotionTokens.SpringFloat,
                                label = "AppRowAlpha"
                            )
                            val translationY by animateFloatAsState(
                                targetValue = if (isVisible) 0f else 18f,
                                animationSpec = MotionTokens.SpringFloat,
                                label = "AppRowY"
                            )
                            Box(
                                modifier = Modifier
                                    .animateItem(placementSpec = MotionTokens.SpringItemReorder)
                                    .graphicsLayer {
                                        this.alpha = alpha
                                        this.translationY = translationY
                                    }
                            ) {
                                AppItemRow(
                                    app = app,
                                    isExtracting = extractingAppPkg == app.packageName,
                                    onExtract = {
                                        haptics.trigger(HapticType.CLICK)
                                        extractingAppPkg = app.packageName
                                        viewModel.extractAppApk(app) { res ->
                                            extractingAppPkg = null
                                            res.onSuccess { destFile ->
                                                showMessage("Extracted to ExtractedAPKs/${destFile.name}")
                                            }.onFailure { err ->
                                                showMessage("Failed: ${err.message}")
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Text(
                text = "Close",
                color = FilesTheme.colors.primaryAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = OutfitFontFamily,
                modifier = Modifier
                    .elasticBounce(targetScale = 0.92f, onClick = onDismiss)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    )
}

@Composable
fun AppItemRow(
    app: InstalledAppItem,
    isExtracting: Boolean,
    onExtract: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (FilesTheme.isDark) Color(0x14FFFFFF) else Color(0xFFF8FAFC))
            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = FilesTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "v${app.versionName} · ${app.formattedSize}",
                fontFamily = OutfitFontFamily,
                fontSize = 11.5.sp,
                color = FilesTheme.colors.textMuted
            )
        }

        // Extract Pill Button
        Box(
            modifier = Modifier
                .elasticBounce(targetScale = 0.90f, hapticType = HapticType.CLICK, enabled = !isExtracting, onClick = onExtract)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFF9333EA), Color(0xFFC084FC)))
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isExtracting) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Extract",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Extract",
                        color = Color.White,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}
