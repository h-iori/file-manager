package com.ioristudios.filemanager.ui.directory

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
import com.ioristudios.filemanager.theme.LocalMotionSensitivity
import com.ioristudios.filemanager.theme.MotionTokens
import com.ioristudios.filemanager.ui.components.elasticBounce
import com.ioristudios.filemanager.ui.components.staggeredEntrance
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.DeleteForever
import com.ioristudios.filemanager.data.ClipboardMode
import com.ioristudios.filemanager.data.ClipboardState
import com.ioristudios.filemanager.ui.components.AppFileOpener
import com.ioristudios.filemanager.haptics.HapticType
import com.ioristudios.filemanager.haptics.LocalAppHaptics
import com.ioristudios.filemanager.ui.components.GlassContextMenu
import com.ioristudios.filemanager.ui.components.GlassContextMenuItem
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import com.ioristudios.filemanager.ui.components.shareFileToSystem
import com.ioristudios.filemanager.data.ArchiveManager
import com.ioristudios.filemanager.data.ArchiveType
import com.ioristudios.filemanager.data.CompressionLevel
import com.ioristudios.filemanager.ui.components.ArchiveOptionsDialog
import com.ioristudios.filemanager.ui.components.CompressDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Android
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ioristudios.filemanager.ui.components.SelectionTopBar
import com.ioristudios.filemanager.ui.components.SelectionCheckbox
import com.ioristudios.filemanager.ui.components.BatchDeleteItemInfo
import com.ioristudios.filemanager.ui.components.EnterpriseBatchDeleteDialog
import com.ioristudios.filemanager.ui.components.EnterpriseDeleteConfirmDialog
import com.ioristudios.filemanager.ui.components.EnterpriseRenameDialog
import com.ioristudios.filemanager.ui.components.shareMultipleFilesToSystem
import com.ioristudios.filemanager.ui.components.FileIconSquircle
import com.ioristudios.filemanager.ui.components.ProtectedDirectoryBanner
import com.ioristudios.filemanager.ui.components.ProtectedDirectoryInfoDialog
import com.ioristudios.filemanager.ui.components.ProtectedActionBlockedDialog
import com.ioristudios.filemanager.ui.components.ProtectedOperationDisclaimerDialog
import com.ioristudios.filemanager.ui.components.ScopedStorageRestrictedAccessDialog
import com.ioristudios.filemanager.data.ProtectionLevel
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
import com.ioristudios.filemanager.theme.*
import com.ioristudios.filemanager.theme.TextSecondary
import com.ioristudios.filemanager.ui.main.AppHeader
import com.ioristudios.filemanager.ui.main.MainScreenViewModel
import com.ioristudios.filemanager.ui.main.NavTab
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DirectoryScreen(
    viewModel: MainScreenViewModel,
    showMessage: (String) -> Unit,
    onSearchClick: () -> Unit = { viewModel.selectTab(NavTab.SEARCH) },
    modifier: Modifier = Modifier
) {
    val state by viewModel.directoryUiState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsUiState.collectAsStateWithLifecycle()
    val clipboardState by viewModel.clipboardState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalAppHaptics.current
    val motion = LocalMotionSensitivity.current

    var selectedItemForDetails by remember { mutableStateOf<DirectoryItem?>(null) }
    var itemToRename by remember { mutableStateOf<DirectoryItem?>(null) }
    var itemToDelete by remember { mutableStateOf<DirectoryItem?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var archiveItemForOptions by remember { mutableStateOf<DirectoryItem?>(null) }
    var itemToCompress by remember { mutableStateOf<DirectoryItem?>(null) }
    var showFolderCompressDialog by remember { mutableStateOf(false) }
    var showProtectionInfoDialog by remember { mutableStateOf(false) }
    var pendingDisclaimerAction by remember { mutableStateOf<Triple<String, DirectoryItem, () -> Unit>?>(null) }

    var selectedPaths by remember { mutableStateOf(emptySet<String>()) }
    val isSelectionMode = selectedPaths.isNotEmpty()
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showBatchCompressDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.currentPath) {
        selectedPaths = emptySet()
    }

    BackHandler(enabled = isSelectionMode) {
        selectedPaths = emptySet()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FilesTheme.colors.bg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Contextual Top Action Bar vs Standard Header with Smooth Vertical Slide
            AnimatedContent(
                targetState = isSelectionMode,
                transitionSpec = {
                    if (motion.isReducedMotion) {
                        fadeIn(tween(80)) togetherWith fadeOut(tween(80))
                    } else {
                        (slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = MotionTokens.SpringSpatialOffset
                        ) + fadeIn(tween(160))) togetherWith (slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = MotionTokens.SpringSpatialOffset
                        ) + fadeOut(tween(140)))
                    }
                },
                label = "SelectionHeaderTransition"
            ) { inSelection ->
                if (inSelection) {
                    SelectionTopBar(
                        selectedCount = selectedPaths.size,
                        totalCount = state.items.size,
                        onClose = { selectedPaths = emptySet() },
                        isAllSelected = selectedPaths.size == state.items.size && state.items.isNotEmpty(),
                        onSelectAllToggle = {
                            selectedPaths = if (selectedPaths.size == state.items.size) {
                                emptySet()
                            } else {
                                state.items.map { it.path }.toSet()
                            }
                        },
                        onInvertSelection = {
                            val allPaths = state.items.map { it.path }.toSet()
                            selectedPaths = allPaths - selectedPaths
                        },
                        onCopy = {
                            viewModel.copyMultipleItems(selectedPaths.toList(), state.currentPath)
                            showMessage("Copied ${selectedPaths.size} items to clipboard")
                            selectedPaths = emptySet()
                        },
                        onCut = {
                            viewModel.cutMultipleItems(selectedPaths.toList(), state.currentPath)
                            showMessage("Cut ${selectedPaths.size} items to clipboard")
                            selectedPaths = emptySet()
                        },
                        onShare = {
                            shareMultipleFilesToSystem(context, selectedPaths.toList(), showMessage)
                        },
                        onDelete = {
                            showBatchDeleteDialog = true
                        },
                        onCompress = {
                            showBatchCompressDialog = true
                        }
                    )
                } else {
                    AppHeader(
                        onSearchClick = onSearchClick,
                        showMenuButton = true,
                        menuContent = { dismiss ->
                            GlassContextMenuItem(
                                icon = Icons.Default.CreateNewFolder,
                                iconTint = if (FilesTheme.isDark) ElectricPurple else Color(0xFF7C3AED),
                                iconBg = if (FilesTheme.isDark) Color(0xFF581C87).copy(alpha = 0.22f) else Color(0xFFF3E8FF),
                                label = "New folder",
                                onClick = {
                                    dismiss()
                                    showNewFolderDialog = true
                                }
                            )
                            GlassContextMenuItem(
                                icon = Icons.AutoMirrored.Filled.NoteAdd,
                                iconTint = if (FilesTheme.isDark) ElectricTeal else Color(0xFF0D9488),
                                iconBg = if (FilesTheme.isDark) Color(0xFF134E4A).copy(alpha = 0.22f) else Color(0xFFCCFBF1),
                                label = "New file",
                                onClick = {
                                    dismiss()
                                    showNewFileDialog = true
                                }
                            )
                            GlassContextMenuItem(
                                icon = Icons.Default.Archive,
                                iconTint = if (FilesTheme.isDark) ElectricAmber else Color(0xFFD97706),
                                iconBg = if (FilesTheme.isDark) Color(0xFF78350F).copy(alpha = 0.22f) else Color(0xFFFEF3C7),
                                label = "Compress folder",
                                onClick = {
                                    dismiss()
                                    showFolderCompressDialog = true
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                thickness = 0.8.dp,
                                color = if (FilesTheme.isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)
                            )
                            GlassContextMenuItem(
                                icon = Icons.Default.Settings,
                                iconTint = FilesTheme.colors.primaryAccent,
                                iconBg = if (FilesTheme.isDark) Color(0xFF9333EA).copy(alpha = 0.22f) else Color(0xFFEEF2FF),
                                label = "Settings",
                                onClick = {
                                    dismiss()
                                    viewModel.selectTab(NavTab.SETTINGS)
                                }
                            )
                        }
                    )
                }
            }

            // Breadcrumb Trail with Expand/Shrink Vertical Transition
            AnimatedVisibility(
                visible = state.breadcrumbs.isNotEmpty(),
                enter = expandVertically(animationSpec = if (motion.isReducedMotion) snap() else tween(180)) + fadeIn(),
                exit = shrinkVertically(animationSpec = if (motion.isReducedMotion) snap() else tween(150)) + fadeOut()
            ) {
                BreadcrumbsBar(
                    state = state,
                    onBreadcrumbClick = { path -> viewModel.openDirectory(path) },
                    onBackClick = { viewModel.navigateUpDirectory() }
                )
            }

            // Sensitive Core Directory Protection Banner
            state.protectionInfo?.let { protection ->
                ProtectedDirectoryBanner(
                    info = protection,
                    onLearnMore = { showProtectionInfoDialog = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
                        color = FilesTheme.colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "· ${state.items.size} items",
                        fontFamily = OutfitFontFamily,
                        color = FilesTheme.colors.textMuted,
                        fontSize = 12.sp
                    )
                }

                // Sort Dropdown Button
                Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(FilesTheme.colors.surface)
                                .border(1.dp, FilesTheme.colors.cardBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .clickable {
                                    haptics.trigger(HapticType.SELECTION_NOTCH)
                                    sortMenuExpanded = true
                                }
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
                                    tint = FilesTheme.colors.primaryAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = getShortSortLabel(state.sortOption),
                                    color = FilesTheme.colors.primaryAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = FilesTheme.colors.textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            modifier = Modifier.background(FilesTheme.colors.dialogBg)
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
                                                    tint = FilesTheme.colors.primaryAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.size(16.dp))
                                            }
                                            Text(
                                                text = option.label,
                                                color = if (state.sortOption == option) FilesTheme.colors.primaryAccent else FilesTheme.colors.textPrimary,
                                                fontSize = 13.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        haptics.trigger(HapticType.SELECTION_NOTCH)
                                        viewModel.setSortOption(option)
                                        sortMenuExpanded = false
                                    },
                                    colors = MenuDefaults.itemColors()
                                )
                            }
                        }
                    }
                }

            // Directory Items List with Spatial Push/Pop Transitions
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = state.currentPath,
                    transitionSpec = {
                        if (motion.isReducedMotion) {
                            fadeIn(tween(90)) togetherWith fadeOut(tween(90))
                        } else {
                            val isNavigatingDeeper = targetState.length > initialState.length
                            if (isNavigatingDeeper) {
                                (slideInHorizontally(
                                    initialOffsetX = { fullWidth -> (fullWidth * 0.75f).toInt() },
                                    animationSpec = MotionTokens.SpringSpatialOffset
                                ) + scaleIn(
                                    initialScale = 0.96f,
                                    animationSpec = MotionTokens.SpringSpatialHierarchy
                                ) + fadeIn(
                                    animationSpec = tween(180)
                                )) togetherWith (slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> (-fullWidth * 0.25f).toInt() },
                                    animationSpec = MotionTokens.SpringSpatialOffset
                                ) + fadeOut(
                                    animationSpec = tween(150)
                                ))
                            } else {
                                (slideInHorizontally(
                                    initialOffsetX = { fullWidth -> (-fullWidth * 0.25f).toInt() },
                                    animationSpec = MotionTokens.SpringSpatialOffset
                                ) + fadeIn(
                                    animationSpec = tween(180)
                                )) togetherWith (slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> (fullWidth * 0.75f).toInt() },
                                    animationSpec = MotionTokens.SpringSpatialOffset
                                ) + scaleOut(
                                    targetScale = 0.96f,
                                    animationSpec = MotionTokens.SpringSpatialHierarchy
                                ) + fadeOut(
                                    animationSpec = tween(150)
                                ))
                            }
                        }
                    },
                    label = "SpatialDirectoryTransition"
                ) { _ ->
                    if (state.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
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
                                .fillMaxSize()
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
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item { Spacer(modifier = Modifier.height(2.dp)) }

                            itemsIndexed(state.items, key = { _, item -> item.path }) { index, item ->
                                val isSelected = item.path in selectedPaths

                                Box(
                                    modifier = Modifier
                                        .animateItem(
                                            fadeInSpec = if (motion.isReducedMotion) null else tween(durationMillis = MotionTokens.DurationFast),
                                            fadeOutSpec = if (motion.isReducedMotion) null else tween(durationMillis = 140),
                                            placementSpec = if (motion.isReducedMotion) snap() else MotionTokens.SpringItemReorder
                                        )
                                        .staggeredEntrance(index = index)
                                ) {
                                    DirectoryItemRow(
                                        item = item,
                                        showFileExtensions = settingsState.showFileExtensions,
                                        isSelectionMode = isSelectionMode,
                                        isSelected = isSelected,
                                        onToggleSelect = {
                                            selectedPaths = if (isSelected) {
                                                selectedPaths - item.path
                                            } else {
                                                selectedPaths + item.path
                                            }
                                        },
                                        onLongClick = {
                                            if (!isSelectionMode) {
                                                selectedPaths = setOf(item.path)
                                            } else {
                                                selectedPaths = if (isSelected) {
                                                    selectedPaths - item.path
                                                } else {
                                                    selectedPaths + item.path
                                                }
                                            }
                                        },
                                        onClick = {
                                            if (item.isDirectory) {
                                                viewModel.openDirectory(item.path)
                                            } else if (item.path.contains("::")) {
                                                viewModel.extractAndOpenArchiveItem(context, item.path, item.mimeType, showMessage)
                                            } else if (ArchiveManager.isArchive(File(item.path))) {
                                                archiveItemForOptions = item
                                            } else {
                                                AppFileOpener.openFile(context, item.path, item.mimeType, showMessage)
                                            }
                                        },
                                        onOpen = {
                                            if (item.isDirectory) {
                                                viewModel.openDirectory(item.path)
                                            } else if (item.path.contains("::")) {
                                                viewModel.extractAndOpenArchiveItem(context, item.path, item.mimeType, showMessage)
                                            } else if (ArchiveManager.isArchive(File(item.path))) {
                                                archiveItemForOptions = item
                                            } else {
                                                AppFileOpener.openFile(context, item.path, item.mimeType, showMessage)
                                            }
                                        },
                                        onCopy = {
                                            viewModel.copyItem(item.path, state.currentPath)
                                            showMessage("Copied \"${item.name}\" to clipboard")
                                        },
                                        onCut = {
                                            if (item.isProtected) {
                                                pendingDisclaimerAction = Triple("move", item) {
                                                    viewModel.cutItem(item.path, state.currentPath)
                                                    showMessage("Cut \"${item.name}\" to clipboard")
                                                }
                                            } else {
                                                viewModel.cutItem(item.path, state.currentPath)
                                                showMessage("Cut \"${item.name}\" to clipboard")
                                            }
                                        },
                                        onRename = {
                                            if (item.isProtected) {
                                                pendingDisclaimerAction = Triple("rename", item) {
                                                    itemToRename = item
                                                }
                                            } else {
                                                itemToRename = item
                                            }
                                        },
                                        onShare = {
                                            shareFileToSystem(context, item.path, item.mimeType, showMessage)
                                        },
                                        onDelete = {
                                            if (item.isProtected) {
                                                pendingDisclaimerAction = Triple("delete", item) {
                                                    itemToDelete = item
                                                }
                                            } else {
                                                itemToDelete = item
                                            }
                                        },
                                        onDetails = { selectedItemForDetails = item },
                                        onCompress = {
                                            itemToCompress = item
                                        },
                                        onExtract = if (ArchiveManager.isArchive(File(item.path))) {
                                            { archiveItemForOptions = item }
                                        } else null,
                                        onTestArchive = if (ArchiveManager.isArchive(File(item.path))) {
                                            {
                                                showMessage("Testing integrity of ${item.name}...")
                                                viewModel.testArchiveIntegrity(item.path) { res ->
                                                    res.onSuccess { msg -> showMessage(msg) }
                                                        .onFailure { err -> showMessage("Integrity check failed: ${err.message}") }
                                                }
                                            }
                                        } else null
                                    )
                                }
                            }

                            item { Spacer(modifier = Modifier.height(96.dp)) }
                        }
                    }
                }
            }
        }

        // Floating Paste Capsule Pill with Fluid Slide-Up
        AnimatedVisibility(
            visible = clipboardState.isActive,
            enter = slideInVertically(
                initialOffsetY = { it * 2 },
                animationSpec = MotionTokens.SpringSpatialOffset
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it * 2 },
                animationSpec = tween(150)
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 96.dp)
        ) {
            FloatingPasteCapsule(
                clipboardState = clipboardState,
                onPaste = {
                    viewModel.pasteClipboard(state.currentPath) { res ->
                        showMessage(res.message)
                    }
                },
                onCancel = {
                    viewModel.clearClipboard()
                }
            )
        }
    }

    // File Details Popup Modal (Fully functional)
    selectedItemForDetails?.let { item ->
        FileDetailsDialog(
            item = item,
            onDismiss = { selectedItemForDetails = null }
        )
    }


    // Archive Options Dialog (When clicking .zip, .apk, .tar, etc.)
    archiveItemForOptions?.let { item ->
        ArchiveOptionsDialog(
            item = item,
            onBrowseInPlace = {
                archiveItemForOptions = null
                viewModel.openDirectory(item.path)
            },
            onExtractHere = {
                archiveItemForOptions = null
                showMessage("Extracting \"${item.name}\"...")
                viewModel.extractArchive(item.path, state.currentPath) { res ->
                    res.onSuccess { count ->
                        showMessage("Extracted $count files successfully")
                    }.onFailure { err ->
                        showMessage("Extraction failed: ${err.message}")
                    }
                }
            },
            onExtractToFolder = {
                archiveItemForOptions = null
                val folderName = item.name.substringBeforeLast('.')
                val targetDir = File(state.currentPath, folderName).absolutePath
                showMessage("Extracting to /$folderName/...")
                viewModel.extractArchive(item.path, targetDir) { res ->
                    res.onSuccess { count ->
                        showMessage("Extracted $count files to /$folderName/")
                    }.onFailure { err ->
                        showMessage("Extraction failed: ${err.message}")
                    }
                }
            },
            onTestIntegrity = {
                archiveItemForOptions = null
                showMessage("Testing integrity of \"${item.name}\"...")
                viewModel.testArchiveIntegrity(item.path) { res ->
                    res.onSuccess { msg ->
                        showMessage(msg)
                    }.onFailure { err ->
                        showMessage("Integrity check failed: ${err.message}")
                    }
                }
            },
            onDismiss = { archiveItemForOptions = null }
        )
    }

    // Single Item / Folder Compress Dialog
    itemToCompress?.let { item ->
        CompressDialog(
            initialName = "${item.name}.zip",
            onDismiss = { itemToCompress = null },
            onCompress = { name, format, level ->
                itemToCompress = null
                val destFile = File(state.currentPath, name)
                showMessage("Creating archive $name...")
                viewModel.createArchive(listOf(item.path), destFile, format, level) { res ->
                    res.onSuccess { file ->
                        showMessage("Archive created: ${file.name}")
                    }.onFailure { err ->
                        showMessage("Compression failed: ${err.message}")
                    }
                }
            }
        )
    }

    // Whole Folder Compress Dialog
    if (showFolderCompressDialog) {
        val folderName = File(state.currentPath).name.ifEmpty { "Archive" }
        CompressDialog(
            initialName = "$folderName.zip",
            onDismiss = { showFolderCompressDialog = false },
            onCompress = { name, format, level ->
                showFolderCompressDialog = false
                val destFile = File(File(state.currentPath).parentFile ?: File(state.currentPath), name)
                showMessage("Creating archive $name...")
                viewModel.createArchive(listOf(state.currentPath), destFile, format, level) { res ->
                    res.onSuccess { file ->
                        showMessage("Archive created: ${file.name}")
                    }.onFailure { err ->
                        showMessage("Compression failed: ${err.message}")
                    }
                }
            }
        )
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        NewFolderDialog(
            onDismiss = { showNewFolderDialog = false },
            onCreate = { folderName ->
                showNewFolderDialog = false
                viewModel.createFolder(state.currentPath, folderName) { result ->
                    result.onSuccess {
                        showMessage("Created folder \"${it.name}\"")
                    }.onFailure {
                        showMessage("Failed: ${it.message}")
                    }
                }
            }
        )
    }

    // New File Dialog
    if (showNewFileDialog) {
        NewFileDialog(
            onDismiss = { showNewFileDialog = false },
            onCreate = { fileName ->
                showNewFileDialog = false
                viewModel.createBlankFile(state.currentPath, fileName) { result ->
                    result.onSuccess {
                        showMessage("Created file \"${it.name}\"")
                    }.onFailure {
                        showMessage("Failed: ${it.message}")
                    }
                }
            }
        )
    }

    // Enterprise Rename Dialog
    itemToRename?.let { item ->
        EnterpriseRenameDialog(
            initialName = item.name,
            isDirectory = item.isDirectory,
            onDismiss = { itemToRename = null },
            onRename = { newName ->
                itemToRename = null
                viewModel.renameItem(item.path, newName) { result ->
                    result.onSuccess {
                        showMessage("Renamed to \"${it.name}\"")
                    }.onFailure {
                        showMessage("Failed: ${it.message}")
                    }
                }
            }
        )
    }

    // Enterprise Single Item Delete Confirmation Dialog
    itemToDelete?.let { item ->
        EnterpriseDeleteConfirmDialog(
            name = item.name,
            path = item.path,
            isDirectory = item.isDirectory,
            formattedSize = item.formattedSize,
            onDismiss = { itemToDelete = null },
            onConfirmDelete = {
                itemToDelete = null
                viewModel.deleteItem(item.path) { result ->
                    result.onSuccess {
                        showMessage("Deleted \"${item.name}\"")
                    }.onFailure {
                        showMessage("Failed: ${it.message}")
                    }
                }
            }
        )
    }

    // Enterprise Batch Deletion Dialog
    if (showBatchDeleteDialog) {
        val selectedItemsInfo = state.items
            .filter { it.path in selectedPaths }
            .map {
                BatchDeleteItemInfo(
                    path = it.path,
                    name = it.name,
                    isDirectory = it.isDirectory,
                    formattedSize = it.formattedSize
                )
            }
        EnterpriseBatchDeleteDialog(
            items = selectedItemsInfo,
            onDismiss = { showBatchDeleteDialog = false },
            onConfirmDelete = {
                showBatchDeleteDialog = false
                val count = selectedPaths.size
                val pathsToDelete = selectedPaths.toList()
                val protectedItem = state.items.firstOrNull { it.path in selectedPaths && it.isProtected }
                val performBatchDelete = {
                    viewModel.deleteMultipleItems(pathsToDelete) { result ->
                        result.onSuccess {
                            showMessage("Permanently deleted $count items")
                        }.onFailure {
                            showMessage("Failed to delete: ${it.message}")
                        }
                    }
                    selectedPaths = emptySet()
                }
                if (protectedItem != null) {
                    pendingDisclaimerAction = Triple("delete multiple items including sensitive item", protectedItem, performBatchDelete)
                } else {
                    performBatchDelete()
                }
            }
        )
    }

    // Enterprise Batch Compress Dialog
    if (showBatchCompressDialog) {
        val defaultArchiveName = if (selectedPaths.size == 1) {
            val single = File(selectedPaths.first())
            "${single.nameWithoutExtension}.zip"
        } else {
            "Archive_${System.currentTimeMillis() / 1000}.zip"
        }
        CompressDialog(
            initialName = defaultArchiveName,
            onDismiss = { showBatchCompressDialog = false },
            onCompress = { name, format, level ->
                showBatchCompressDialog = false
                val destFile = File(state.currentPath, name)
                val count = selectedPaths.size
                showMessage("Creating archive $name...")
                viewModel.createArchive(selectedPaths.toList(), destFile, format, level) { res ->
                    res.onSuccess { file ->
                        showMessage("Archive created: ${file.name}")
                        selectedPaths = emptySet()
                    }.onFailure { err ->
                        showMessage("Compression failed: ${err.message}")
                    }
                }
            }
        )
    }

    // Security Protection Info Dialog
    if (showProtectionInfoDialog) {
        state.protectionInfo?.let { info ->
            ProtectedDirectoryInfoDialog(
                info = info,
                onDismiss = { showProtectionInfoDialog = false }
            )
        }
    }

    // Secondary Disclaimer Dialog for Modifying or Deleting Protected / Sensitive Folders
    pendingDisclaimerAction?.let { (action, item, onConfirm) ->
        ProtectedOperationDisclaimerDialog(
            actionName = action,
            itemName = item.name,
            onConfirm = {
                pendingDisclaimerAction = null
                onConfirm()
            },
            onDismiss = { pendingDisclaimerAction = null }
        )
    }
}

@Composable
fun BreadcrumbsBar(
    state: DirectoryScreenState,
    onBreadcrumbClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val haptics = LocalAppHaptics.current
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
                onClick = {
                    haptics.trigger(HapticType.NAV_TICK)
                    onBackClick()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = FilesTheme.colors.primaryAccent,
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
                            if (isLast) (if (FilesTheme.isDark) Color(0xFF20173D) else Color(0xFFEDE4FF)) else Color.Transparent
                        )
                        .clickable(enabled = !isLast) {
                            haptics.trigger(HapticType.NAV_TICK)
                            onBreadcrumbClick(segment.fullPath)
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = segment.title,
                        color = if (isLast) FilesTheme.colors.primaryAccent else FilesTheme.colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                if (!isLast) {
                    Text(
                        text = "/",
                        color = FilesTheme.colors.textMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DirectoryItemRow(
    item: DirectoryItem,
    showFileExtensions: Boolean = true,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onClick: () -> Unit,
    onOpen: () -> Unit,
    onCopy: () -> Unit = {},
    onCut: () -> Unit = {},
    onRename: () -> Unit = {},
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    onCompress: (() -> Unit)? = null,
    onExtract: (() -> Unit)? = null,
    onTestArchive: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val haptics = LocalAppHaptics.current

    val targetBg = if (isSelected) {
        if (FilesTheme.isDark) Color(0xFF2E1253) else Color(0xFFF3E8FF)
    } else {
        FilesTheme.colors.surface
    }
    val animatedBg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = MotionTokens.resolveTween(durationMillis = MotionTokens.DurationFast),
        label = "RowBgColor"
    )
    val targetBorder = if (isSelected) Color(0xFFC084FC) else FilesTheme.colors.cardBorder.copy(alpha = 0.5f)
    val animatedBorder by animateColorAsState(
        targetValue = targetBorder,
        animationSpec = MotionTokens.resolveTween(durationMillis = MotionTokens.DurationFast),
        label = "RowBorderColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(animatedBg)
            .border(
                if (isSelected) 1.5.dp else 1.dp,
                animatedBorder,
                RoundedCornerShape(18.dp)
            )
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        if (item.isDirectory) {
                            haptics.trigger(HapticType.FOLDER_OPEN)
                        } else {
                            haptics.trigger(HapticType.CLICK)
                        }
                        onClick()
                    }
                },
                onLongClick = {
                    haptics.trigger(HapticType.SELECTION_NOTCH)
                    onLongClick()
                }
            )
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // In selection mode, show animated glassmorphic selection checkbox
                AnimatedVisibility(
                    visible = isSelectionMode,
                    enter = scaleIn(initialScale = 0.0f, animationSpec = MotionTokens.SpringCheckboxPop) + fadeIn(animationSpec = tween(140)),
                    exit = scaleOut(targetScale = 0.0f, animationSpec = tween(120)) + fadeOut(animationSpec = tween(120))
                ) {
                    SelectionCheckbox(
                        isSelected = isSelected,
                        onClick = onToggleSelect
                    )
                }

                // Rich Brand & Filetype Icon with Protection Badge
                FileIconSquircle(
                    item = item,
                    size = 44.dp
                )

                val displayName = if (!item.isDirectory && !showFileExtensions) {
                    val dot = item.name.lastIndexOf('.')
                    if (dot > 0) item.name.substring(0, dot) else item.name
                } else item.name

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        color = FilesTheme.colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.fileCountLabel,
                        color = FilesTheme.colors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // More Options Menu Button (hidden in selection mode)
            if (!isSelectionMode) {
                Box {
                    IconButton(
                        onClick = {
                            haptics.trigger(HapticType.MENU_POP)
                            menuExpanded = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Options",
                            tint = FilesTheme.colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    GlassContextMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        onOpen = onOpen,
                        onCopy = onCopy,
                        onCut = onCut,
                        onRename = onRename,
                        onDetails = onDetails,
                        onShare = onShare,
                        onDelete = onDelete,
                        onCompress = onCompress,
                        onExtract = onExtract,
                        onTestArchive = onTestArchive
                    )
                }
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
                        imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Info,
                        contentDescription = null,
                        tint = FilesTheme.colors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = if (item.isDirectory) "Directory Details" else "File Details",
                    color = FilesTheme.colors.textPrimary,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .elasticBounce(targetScale = 0.92f, onClick = onDismiss)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF9333EA), PurpleContainer))
                        )
                        .border(1.dp, FilesTheme.colors.primaryAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
        }
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = FilesTheme.colors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = FilesTheme.colors.textPrimary,
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
    AppFileOpener.openFile(context, item.path, item.mimeType, showMessage)
}

@Composable
fun FloatingPasteCapsule(
    clipboardState: ClipboardState,
    onPaste: () -> Unit,
    onCancel: () -> Unit
) {
    val haptics = LocalAppHaptics.current
    Surface(
        color = if (FilesTheme.isDark) Color(0xFF1B1130) else Color(0xFFFAF5FF),
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (FilesTheme.isDark) Color(0xFFC084FC).copy(alpha = 0.8f) else Color(0xFFA855F7)
        ),
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val count = clipboardState.items.size
            val isCut = clipboardState.mode == ClipboardMode.CUT

            Icon(
                imageVector = if (isCut) Icons.Default.ContentCut else Icons.Default.ContentCopy,
                contentDescription = null,
                tint = FilesTheme.colors.primaryAccent,
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = "${if (isCut) "Moving" else "Copying"} $count item${if (count > 1) "s" else ""}",
                color = FilesTheme.colors.textPrimary,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )

            // Paste Button Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF9333EA), Color(0xFFC084FC))
                        )
                    )
                    .clickable {
                        haptics.trigger(HapticType.CLICK)
                        onPaste()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Paste",
                        color = Color.White,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            // Cancel Button
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (FilesTheme.isDark) Color(0x22FFFFFF) else Color(0x11000000))
                    .clickable {
                        haptics.trigger(HapticType.CLICK)
                        onCancel()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = FilesTheme.colors.textMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun GlassDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = ""
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = FilesTheme.colors.textPrimary,
            fontSize = 15.sp,
            fontFamily = OutfitFontFamily
        ),
        cursorBrush = SolidColor(FilesTheme.colors.primaryAccent),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (FilesTheme.isDark) Color(0xFF1E1035) else Color(0xFFF1F5F9))
            .border(1.dp, FilesTheme.colors.primaryAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { innerTextField ->
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    color = FilesTheme.colors.textMuted,
                    fontSize = 14.sp,
                    fontFamily = OutfitFontFamily
                )
            }
            innerTextField()
        }
    )
}

@Composable
fun NewFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("New Folder") }

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
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        tint = FilesTheme.colors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "New Folder",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FilesTheme.colors.textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Enter a name for the new folder:",
                    color = FilesTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontFamily = OutfitFontFamily
                )
                GlassDialogTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    placeholder = "Folder name"
                )
            }
        },
        confirmButton = {
            Text(
                text = "Create",
                color = FilesTheme.colors.primaryAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = OutfitFontFamily,
                modifier = Modifier
                    .elasticBounce(targetScale = 0.92f) {
                        val trimmed = folderName.trim()
                        if (trimmed.isNotEmpty()) {
                            onCreate(trimmed)
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

@Composable
fun NewFileDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var fileName by remember { mutableStateOf("new_file.txt") }

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
                        imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                        contentDescription = null,
                        tint = FilesTheme.colors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "New File",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FilesTheme.colors.textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Enter a file name with extension:",
                    color = FilesTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontFamily = OutfitFontFamily
                )
                GlassDialogTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    placeholder = "e.g. notes.txt"
                )
            }
        },
        confirmButton = {
            Text(
                text = "Create",
                color = FilesTheme.colors.primaryAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = OutfitFontFamily,
                modifier = Modifier
                    .elasticBounce(targetScale = 0.92f) {
                        val trimmed = fileName.trim()
                        if (trimmed.isNotEmpty()) {
                            onCreate(trimmed)
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

@Composable
fun RenameDialog(
    initialName: String,
    isDirectory: Boolean,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var newName by remember { mutableStateOf(initialName) }

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
                        imageVector = Icons.Default.DriveFileRenameOutline,
                        contentDescription = null,
                        tint = FilesTheme.colors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = if (isDirectory) "Rename Folder" else "Rename File",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FilesTheme.colors.textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Enter a new name:",
                    color = FilesTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontFamily = OutfitFontFamily
                )
                GlassDialogTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = "Name"
                )
            }
        },
        confirmButton = {
            Text(
                text = "Rename",
                color = FilesTheme.colors.primaryAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = OutfitFontFamily,
                modifier = Modifier
                    .elasticBounce(targetScale = 0.92f) {
                        val trimmed = newName.trim()
                        if (trimmed.isNotEmpty() && trimmed != initialName) {
                            onRename(trimmed)
                        } else if (trimmed == initialName) {
                            onDismiss()
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

@Composable
fun DeleteConfirmDialog(
    itemName: String,
    isDirectory: Boolean,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
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
                        .background(Color(0xFFB71C1C).copy(alpha = 0.25f))
                        .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = if (isDirectory) "Delete Folder" else "Delete File",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = FilesTheme.colors.textPrimary
                )
            }
        },
        text = {
            Text(
                text = "Are you sure you want to permanently delete \"$itemName\"? This action cannot be undone.",
                color = FilesTheme.colors.textSecondary,
                fontSize = 13.5.sp,
                fontFamily = OutfitFontFamily,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            Text(
                text = "Delete",
                color = Color(0xFFFF5252),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = OutfitFontFamily,
                modifier = Modifier
                    .elasticBounce(targetScale = 0.92f, hapticType = HapticType.DESTRUCTIVE, onClick = onConfirmDelete)
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

@Composable
fun DeleteConfirmDialog(
    item: DirectoryItem,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) = DeleteConfirmDialog(
    itemName = item.name,
    isDirectory = item.isDirectory,
    onDismiss = onDismiss,
    onConfirmDelete = onConfirmDelete
)
