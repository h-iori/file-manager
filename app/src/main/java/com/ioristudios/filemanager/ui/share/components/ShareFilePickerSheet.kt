package com.ioristudios.filemanager.ui.share.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ioristudios.filemanager.data.DirectoryItem
import com.ioristudios.filemanager.haptics.HapticType
import com.ioristudios.filemanager.haptics.LocalAppHaptics
import com.ioristudios.filemanager.theme.BgDeepDark
import com.ioristudios.filemanager.theme.CardBorderPurple
import com.ioristudios.filemanager.theme.CardGradientEnd
import com.ioristudios.filemanager.theme.CardGradientStart
import com.ioristudios.filemanager.theme.ChartAvailableEmerald
import com.ioristudios.filemanager.theme.ElectricCyan
import com.ioristudios.filemanager.theme.ElectricPurple
import com.ioristudios.filemanager.theme.OrbitronFontFamily
import com.ioristudios.filemanager.theme.OutfitFontFamily
import com.ioristudios.filemanager.theme.SurfaceContainer
import com.ioristudios.filemanager.theme.TextMuted
import com.ioristudios.filemanager.theme.TextPrimary
import com.ioristudios.filemanager.theme.TextSecondary
import com.ioristudios.filemanager.ui.main.MainScreenViewModel
import java.io.File

/**
 * Enterprise-grade in-app file manager picker sheet for wireless P2P sharing.
 * Allows navigating directories, viewing folders/files, and multi-selecting files.
 */
@Composable
fun ShareFilePickerSheet(
    viewModel: MainScreenViewModel,
    initiallySelectedFiles: List<File>,
    onDismiss: () -> Unit,
    onConfirmSelection: (List<File>) -> Unit
) {
    val haptics = LocalAppHaptics.current
    val dirState by viewModel.directoryUiState.collectAsStateWithLifecycle()
    val homeState by viewModel.homeUiState.collectAsStateWithLifecycle()

    val selectedFiles = remember {
        mutableStateListOf<File>().apply { addAll(initiallySelectedFiles) }
    }

    var selectedCategoryFilter by remember { mutableStateOf("All") }
    val categories = listOf("All", "Downloads", "Documents", "Pictures", "DCIM", "Music")

    // Ensure directory is loaded when picker opens
    LaunchedEffect(Unit) {
        if (dirState.items.isEmpty()) {
            val root = android.os.Environment.getExternalStorageDirectory()
            viewModel.openDirectory(root.absolutePath)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeepDark)
            .padding(top = 16.dp, bottom = 4.dp, start = 16.dp, end = 16.dp)
    ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FILE MANAGER PICKER",
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Select files to share with nearby devices",
                            fontFamily = OutfitFontFamily,
                            fontSize = 12.sp,
                            color = ElectricCyan
                        )
                    }

                    // Close Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1736))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Category Filters
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) ElectricCyan.copy(alpha = 0.2f)
                                    else Color(0xFF16102A)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) ElectricCyan else CardBorderPurple.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedCategoryFilter = cat
                                    if (cat != "All") {
                                        val root = android.os.Environment.getExternalStorageDirectory()
                                        val target = File(root, cat)
                                        if (target.exists()) {
                                            viewModel.openDirectory(target.absolutePath)
                                        }
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontFamily = OutfitFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isSelected) ElectricCyan else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Breadcrumbs & Up Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF110C24))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Navigate Up Button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1F163D))
                            .clickable { viewModel.navigateUpDirectory() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Up",
                            tint = ElectricCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dirState.breadcrumbs.forEachIndexed { index, crumb ->
                            Text(
                                text = crumb.title,
                                fontFamily = OutfitFontFamily,
                                fontSize = 12.sp,
                                color = if (index == dirState.breadcrumbs.lastIndex) TextPrimary else TextMuted,
                                modifier = Modifier.clickable {
                                    viewModel.openDirectory(crumb.fullPath)
                                }
                            )
                            if (index < dirState.breadcrumbs.lastIndex) {
                                Text(
                                    text = " / ",
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Directory Items List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(dirState.items) { item ->
                        val itemFile = File(item.path)
                        val isSelected = selectedFiles.any { it.absolutePath == item.path }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) ElectricCyan.copy(alpha = 0.12f)
                                    else Color(0xFF140D28)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) ElectricCyan.copy(alpha = 0.6f)
                                    else CardBorderPurple.copy(alpha = 0.25f),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    if (item.isDirectory) {
                                        viewModel.openDirectory(item.path)
                                    } else {
                                        if (isSelected) {
                                            selectedFiles.removeAll { it.absolutePath == item.path }
                                        } else {
                                            selectedFiles.add(itemFile)
                                        }
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Item Icon
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (item.isDirectory) ElectricPurple.copy(alpha = 0.15f)
                                            else ElectricCyan.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                                        contentDescription = item.name,
                                        tint = if (item.isDirectory) ElectricPurple else ElectricCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Item Info
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (item.isDirectory) item.fileCountLabel else item.formattedSize,
                                        fontFamily = OutfitFontFamily,
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                // Checkbox for files
                                if (!item.isDirectory) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) selectedFiles.add(itemFile)
                                            else selectedFiles.removeAll { it.absolutePath == item.path }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = ElectricCyan,
                                            uncheckedColor = CardBorderPurple,
                                            checkmarkColor = Color(0xFF07040D)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Docked Glass Confirmation Bar
            val canSend = selectedFiles.isNotEmpty()
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xF0181135), Color(0xF50C071E))
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                if (canSend) ElectricCyan.copy(alpha = 0.6f) else CardBorderPurple.copy(alpha = 0.35f),
                                if (canSend) ElectricPurple.copy(alpha = 0.5f) else Color(0x22FFFFFF),
                                CardBorderPurple.copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: File Count and Size with Glowing Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (canSend) ElectricCyan.copy(alpha = 0.16f) else Color(0xFF1E1736))
                                .border(1.dp, if (canSend) ElectricCyan.copy(alpha = 0.5f) else Color(0x33FFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = null,
                                tint = if (canSend) ElectricCyan else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            val count = selectedFiles.size
                            Text(
                                text = if (count == 0) "NO FILES SELECTED" else if (count == 1) "1 FILE SELECTED" else "$count FILES SELECTED",
                                fontFamily = OrbitronFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 0.8.sp,
                                color = if (canSend) ElectricCyan else TextMuted
                            )
                            val totalBytes = selectedFiles.sumOf { it.length() }
                            Text(
                                text = if (count == 0) "Tap files above to select" else formatBytes(totalBytes),
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right: Prominent Send Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (canSend) Brush.horizontalGradient(
                                    listOf(ElectricCyan, ElectricPurple)
                                ) else Brush.horizontalGradient(
                                    listOf(Color(0xFF261D3D), Color(0xFF1B142E))
                                )
                            )
                            .clickable(enabled = canSend) {
                                haptics.trigger(HapticType.SUCCESS_CONFIRM)
                                onConfirmSelection(selectedFiles.toList())
                            }
                            .padding(horizontal = 20.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (canSend) Color(0xFF07040D) else TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "SEND",
                                fontFamily = OrbitronFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp,
                                color = if (canSend) Color(0xFF07040D) else TextMuted
                            )
                        }
                    }
                }
            }
        }
    }

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024L * 1024L -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

