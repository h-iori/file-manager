package com.ioristudios.filemanager.ui.directory

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCameraBack
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.ioristudios.filemanager.ui.components.GlassContextMenu
import com.ioristudios.filemanager.ui.components.shareFileToSystem
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ioristudios.filemanager.data.DirectoryItem
import com.ioristudios.filemanager.data.DirectoryScreenState
import com.ioristudios.filemanager.data.DirectoryVisualType
import com.ioristudios.filemanager.data.SortOption
import com.ioristudios.filemanager.theme.BgDeepDark
import com.ioristudios.filemanager.theme.BorderSubtle
import com.ioristudios.filemanager.theme.CardBorderPurple
import com.ioristudios.filemanager.theme.ElectricCoral
import com.ioristudios.filemanager.theme.ElectricCyan
import com.ioristudios.filemanager.theme.ElectricLavender
import com.ioristudios.filemanager.theme.ElectricMagenta
import com.ioristudios.filemanager.theme.ElectricPurple
import com.ioristudios.filemanager.theme.ElectricTeal
import com.ioristudios.filemanager.theme.OutfitFontFamily
import com.ioristudios.filemanager.theme.PurpleContainer
import com.ioristudios.filemanager.theme.SurfaceContainer
import com.ioristudios.filemanager.theme.SurfaceContainerHigh
import com.ioristudios.filemanager.theme.OutfitFontFamily
import com.ioristudios.filemanager.theme.TextMuted
import com.ioristudios.filemanager.theme.TextPrimary
import com.ioristudios.filemanager.theme.TextSecondary
import com.ioristudios.filemanager.ui.main.AppHeader
import com.ioristudios.filemanager.ui.main.MainScreenViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DirectoryScreen(
    viewModel: MainScreenViewModel,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.directoryUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedItemForDetails by remember { mutableStateOf<DirectoryItem?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDeepDark)
    ) {
        // Shared Top Brand Header (Identical to Stitch Screen 5 & Screen 6)
        AppHeader(
            onSearchClick = { showMessage("Directory search coming soon") },
            onMenuClick = {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                try {
                    context.startActivity(intent)
                } catch (_: Exception) {
                    showMessage("Settings")
                }
            }
        )

        // Breadcrumb Trail (Interactive drill-down navigation)
        if (state.breadcrumbs.isNotEmpty()) {
            BreadcrumbsBar(
                state = state,
                onBreadcrumbClick = { path -> viewModel.openDirectory(path) },
                onBackClick = { viewModel.navigateUpDirectory() }
            )
        }

        // Section Header with Sort Options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Directories",
                    fontFamily = OutfitFontFamily,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "· ${state.items.size} items",
                    fontFamily = OutfitFontFamily,
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            // Sort Dropdown Button
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A1233).copy(alpha = 0.7f))
                        .border(1.dp, BorderSubtle.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable { sortMenuExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SortByAlpha,
                            contentDescription = "Sort",
                            tint = ElectricPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = getShortSortLabel(state.sortOption),
                            color = ElectricPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    modifier = Modifier.background(SurfaceContainerHigh)
                ) {
                    SortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (state.sortOption == option) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = ElectricPurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = option.label,
                                        color = if (state.sortOption == option) ElectricPurple else TextPrimary,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            onClick = {
                                viewModel.setSortOption(option)
                                sortMenuExpanded = false
                            },
                            colors = MenuDefaults.itemColors()
                        )
                    }
                }
            }
        }

        // Directory Items List
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = ElectricPurple,
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
            }
        } else if (state.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "This folder is empty",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(2.dp)) }

                items(state.items, key = { it.path }) { item ->
                    DirectoryItemRow(
                        item = item,
                        onClick = {
                            if (item.isDirectory) {
                                viewModel.openDirectory(item.path)
                            } else {
                                openDirectoryFile(context, item, showMessage)
                            }
                        },
                        onOpen = {
                            if (item.isDirectory) {
                                viewModel.openDirectory(item.path)
                            } else {
                                openDirectoryFile(context, item, showMessage)
                            }
                        },
                        onShare = {
                            shareFileToSystem(context, item.path, item.mimeType, showMessage)
                        },
                        onDelete = { showMessage("Deleting ${item.name} is restricted in preview") },
                        onDetails = { selectedItemForDetails = item }
                    )
                }

                item { Spacer(modifier = Modifier.height(96.dp)) }
            }
        }
    }

    // File Details Popup Modal (Fully functional)
    selectedItemForDetails?.let { item ->
        FileDetailsDialog(
            item = item,
            onDismiss = { selectedItemForDetails = null }
        )
    }
}

@Composable
fun BreadcrumbsBar(
    state: DirectoryScreenState,
    onBreadcrumbClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.breadcrumbs.size > 1) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ElectricPurple,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            state.breadcrumbs.forEachIndexed { index, segment ->
                val isLast = index == state.breadcrumbs.size - 1
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isLast) Color(0xFF20173D) else Color.Transparent
                        )
                        .clickable(enabled = !isLast) { onBreadcrumbClick(segment.fullPath) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = segment.title,
                        color = if (isLast) ElectricPurple else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                if (!isLast) {
                    Text(
                        text = "/",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DirectoryItemRow(
    item: DirectoryItem,
    onClick: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val (icon, iconTint, iconBg, iconBorder) = when (item.visualType) {
        DirectoryVisualType.CAMERA -> ItemVisuals(
            Icons.Default.PhotoCameraBack,
            ElectricPurple,
            Color(0xFF9333EA).copy(alpha = 0.2f),
            Color(0xFFA855F7).copy(alpha = 0.3f)
        )
        DirectoryVisualType.DOWNLOAD -> ItemVisuals(
            Icons.Outlined.Download,
            ElectricCoral,
            Color(0xFFE11D48).copy(alpha = 0.2f),
            ElectricCoral.copy(alpha = 0.3f)
        )
        DirectoryVisualType.DOCUMENT -> ItemVisuals(
            Icons.Default.Description,
            ElectricMagenta,
            Color(0xFFC026D3).copy(alpha = 0.2f),
            Color(0xFFD946EF).copy(alpha = 0.3f)
        )
        DirectoryVisualType.MOVIE -> ItemVisuals(
            Icons.Default.Movie,
            ElectricCyan,
            Color(0xFF0284C7).copy(alpha = 0.2f),
            ElectricCyan.copy(alpha = 0.3f)
        )
        DirectoryVisualType.HEADPHONES -> ItemVisuals(
            Icons.Default.Headphones,
            ElectricTeal,
            Color(0xFF0F766E).copy(alpha = 0.25f),
            ElectricTeal.copy(alpha = 0.3f)
        )
        DirectoryVisualType.IMAGE -> ItemVisuals(
            Icons.Default.Image,
            ElectricPurple,
            Color(0xFF20173D),
            CardBorderPurple.copy(alpha = 0.5f)
        )
        DirectoryVisualType.SEND -> ItemVisuals(
            Icons.AutoMirrored.Filled.Send,
            ElectricCyan,
            Color(0xFF0284C7).copy(alpha = 0.2f),
            ElectricCyan.copy(alpha = 0.3f)
        )
        DirectoryVisualType.MIC -> ItemVisuals(
            Icons.Default.Mic,
            ElectricMagenta,
            Color(0xFFC026D3).copy(alpha = 0.2f),
            ElectricMagenta.copy(alpha = 0.3f)
        )
        DirectoryVisualType.BACKUP -> ItemVisuals(
            Icons.Default.Backup,
            ElectricLavender,
            Color(0xFF4F46E5).copy(alpha = 0.2f),
            ElectricLavender.copy(alpha = 0.3f)
        )
        DirectoryVisualType.PODCASTS -> ItemVisuals(
            Icons.Default.Podcasts,
            ElectricPurple,
            Color(0xFF20173D),
            CardBorderPurple.copy(alpha = 0.5f)
        )
        DirectoryVisualType.NOTIFICATIONS -> ItemVisuals(
            Icons.Default.NotificationsActive,
            TextMuted,
            Color(0xFF20173D),
            CardBorderPurple.copy(alpha = 0.5f)
        )
        DirectoryVisualType.ANDROID -> ItemVisuals(
            Icons.Default.Folder,
            ElectricPurple,
            Color(0xFF20173D),
            CardBorderPurple.copy(alpha = 0.5f)
        )
        DirectoryVisualType.FOLDER -> ItemVisuals(
            Icons.Default.Folder,
            ElectricPurple,
            Color(0xFF20173D),
            CardBorderPurple.copy(alpha = 0.5f)
        )
        DirectoryVisualType.FILE_GENERIC -> ItemVisuals(
            Icons.AutoMirrored.Filled.InsertDriveFile,
            TextSecondary,
            Color(0xFF20173D),
            BorderSubtle.copy(alpha = 0.5f)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF140E26))
            .border(1.dp, BorderSubtle.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Folder / File Icon Container
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg)
                        .border(1.dp, iconBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.fileCountLabel,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // More Options Menu Button
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Options",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                GlassContextMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    onOpen = onOpen,
                    onDetails = onDetails,
                    onShare = onShare,
                    onDelete = onDelete
                )
            }
        }
    }
}

data class ItemVisuals(
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val iconBorder: Color
)

@Composable
fun FileDetailsDialog(
    item: DirectoryItem,
    onDismiss: () -> Unit
) {
    val fileObj = File(item.path)
    val exactBytes = if (item.isDirectory) item.sizeBytes else fileObj.length()
    val lastModifiedFormatted = SimpleDateFormat("MMM d, yyyy · h:mm:ss a", Locale.US).format(Date(item.dateModifiedMs))

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF140E26),
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
                        .border(1.dp, ElectricPurple.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Info,
                        contentDescription = null,
                        tint = ElectricPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = if (item.isDirectory) "Directory Details" else "File Details",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailRow(label = "Name", value = item.name)
                DetailRow(
                    label = "Type",
                    value = if (item.isDirectory) "Directory (${item.childCount} items)" else "File"
                )
                DetailRow(label = "Location", value = item.path)
                DetailRow(
                    label = "Size",
                    value = "${item.formattedSize} (${String.format(Locale.US, "%,d bytes", exactBytes)})"
                )
                DetailRow(label = "Modified", value = lastModifiedFormatted)
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF9333EA), PurpleContainer))
                    )
                    .border(1.dp, ElectricPurple.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Close",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 17.sp
        )
    }
}

private fun getShortSortLabel(sort: SortOption): String {
    return when (sort) {
        SortOption.NAME_ASC -> "Name (A-Z)"
        SortOption.NAME_DESC -> "Name (Z-A)"
        SortOption.DATE_DESC -> "Date (Newest)"
        SortOption.DATE_ASC -> "Date (Oldest)"
        SortOption.SIZE_DESC -> "Size (Largest)"
        SortOption.SIZE_ASC -> "Size (Smallest)"
    }
}

private fun openDirectoryFile(context: Context, item: DirectoryItem, showMessage: (String) -> Unit) {
    try {
        val file = File(item.path)
        if (!file.exists()) {
            showMessage("File not found")
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            setDataAndType(uri, "*/*")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        showMessage("Cannot open ${item.name}: ${e.message}")
    }
}
