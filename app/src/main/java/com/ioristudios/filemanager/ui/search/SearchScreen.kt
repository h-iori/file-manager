package com.ioristudios.filemanager.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.graphicsLayer
import com.ioristudios.filemanager.theme.LocalMotionSensitivity
import com.ioristudios.filemanager.theme.MotionTokens
import com.ioristudios.filemanager.ui.components.elasticBounce
import com.ioristudios.filemanager.ui.components.staggeredEntrance
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.MoreVert
import com.ioristudios.filemanager.ui.components.GlassContextMenu
import com.ioristudios.filemanager.ui.components.ArchiveOptionsDialog
import com.ioristudios.filemanager.ui.components.CompressDialog
import com.ioristudios.filemanager.ui.directory.RenameDialog
import com.ioristudios.filemanager.ui.directory.DeleteConfirmDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.ioristudios.filemanager.ui.components.SelectionTopBar
import com.ioristudios.filemanager.ui.components.SelectionCheckbox
import com.ioristudios.filemanager.ui.components.BatchDeleteItemInfo
import com.ioristudios.filemanager.ui.components.EnterpriseBatchDeleteDialog
import com.ioristudios.filemanager.ui.components.EnterpriseDeleteConfirmDialog
import com.ioristudios.filemanager.ui.components.EnterpriseRenameDialog
import com.ioristudios.filemanager.ui.components.shareMultipleFilesToSystem
import com.ioristudios.filemanager.data.ArchiveManager
import com.ioristudios.filemanager.data.ArchiveType
import com.ioristudios.filemanager.data.CompressionLevel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ioristudios.filemanager.data.FileCategory
import com.ioristudios.filemanager.data.SearchFilterCategory
import com.ioristudios.filemanager.data.SearchResultItem
import com.ioristudios.filemanager.data.DirectoryItem
import com.ioristudios.filemanager.data.SecurityProtectionManager
import com.ioristudios.filemanager.data.StorageScanner
import com.ioristudios.filemanager.ui.components.FileIconSquircle
import com.ioristudios.filemanager.haptics.HapticType
import com.ioristudios.filemanager.haptics.LocalAppHaptics
import com.ioristudios.filemanager.theme.*
import com.ioristudios.filemanager.ui.components.AppFileOpener
import com.ioristudios.filemanager.ui.components.FileDetailsModalDialog
import com.ioristudios.filemanager.ui.components.shareFileToSystem
import com.ioristudios.filemanager.ui.main.MainScreenViewModel
import com.ioristudios.filemanager.ui.main.NavTab
import java.io.File
import java.util.Locale

@Composable
fun SearchScreen(
    viewModel: MainScreenViewModel,
    onBackClick: () -> Unit,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchState by viewModel.searchUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalAppHaptics.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val motion = LocalMotionSensitivity.current

    var selectedItemForDetails by remember { mutableStateOf<SearchResultItem?>(null) }
    var itemToRename by remember { mutableStateOf<SearchResultItem?>(null) }
    var itemToDelete by remember { mutableStateOf<SearchResultItem?>(null) }
    var itemForCompress by remember { mutableStateOf<SearchResultItem?>(null) }
    var archiveItemForOptions by remember { mutableStateOf<SearchResultItem?>(null) }
    var queryText by remember { mutableStateOf(searchState.query) }

    var selectedPaths by remember { mutableStateOf(emptySet<String>()) }
    val isSelectionMode = selectedPaths.isNotEmpty()
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showBatchCompressDialog by remember { mutableStateOf(false) }

    LaunchedEffect(searchState.query) {
        if (queryText != searchState.query) {
            queryText = searchState.query
        }
        selectedPaths = emptySet()
    }

    BackHandler(enabled = isSelectionMode) {
        selectedPaths = emptySet()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0817))
    ) {
        // Ambient Radial Purple Glow (Matching Stitch SCREEN_4)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x389333EA),
                            Color(0x159333EA),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            if (isSelectionMode) {
                // Contextual Action Top Bar for Batch Search Results Operations
                SelectionTopBar(
                    selectedCount = selectedPaths.size,
                    totalCount = searchState.results.size,
                    onClose = { selectedPaths = emptySet() },
                    isAllSelected = selectedPaths.size == searchState.results.size && searchState.results.isNotEmpty(),
                    onSelectAllToggle = {
                        selectedPaths = if (selectedPaths.size == searchState.results.size) {
                            emptySet()
                        } else {
                            searchState.results.map { it.path }.toSet()
                        }
                    },
                    onInvertSelection = {
                        val allPaths = searchState.results.map { it.path }.toSet()
                        selectedPaths = allPaths - selectedPaths
                    },
                    onCopy = {
                        val firstParent = searchState.results.firstOrNull { it.path in selectedPaths }?.let { File(it.path).parent } ?: ""
                        viewModel.copyMultipleItems(selectedPaths.toList(), firstParent)
                        showMessage("Copied ${selectedPaths.size} items to clipboard")
                        selectedPaths = emptySet()
                    },
                    onCut = {
                        val firstParent = searchState.results.firstOrNull { it.path in selectedPaths }?.let { File(it.path).parent } ?: ""
                        viewModel.cutMultipleItems(selectedPaths.toList(), firstParent)
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
                // Header Row: Back button + Glowing Search Input Field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Back Button Squircle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF18112E))
                            .border(1.dp, Color(0x40A855F7), RoundedCornerShape(12.dp))
                            .clickable {
                                haptics.trigger(HapticType.SELECTION_NOTCH)
                                keyboardController?.hide()
                                onBackClick()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFFC084FC),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Glowing Search Input Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF140E26))
                            .border(1.2.dp, Color(0x80A855F7), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (queryText.isEmpty()) {
                                    Text(
                                        text = "Search files & folders...",
                                        color = Color(0x66C084FC),
                                        fontSize = 14.sp,
                                        fontFamily = OutfitFontFamily
                                    )
                                }
                                BasicTextField(
                                    value = queryText,
                                    onValueChange = { newQuery ->
                                        queryText = newQuery
                                        viewModel.onSearchQueryChanged(newQuery)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 14.5.sp,
                                        fontFamily = OutfitFontFamily,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    cursorBrush = SolidColor(Color(0xFFC084FC)),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        keyboardController?.hide()
                                    })
                                )
                            }

                            if (queryText.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x66581C87))
                                        .clickable {
                                            queryText = ""
                                            viewModel.clearSearch()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color(0xFFE9D5FF),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Filter Chips Horizontal Carousel (Matching Stitch Screen 4)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchFilterCategory.values().forEach { cat ->
                        val isSelected = searchState.selectedCategory == cat
                        val count = searchState.categoryCounts[cat] ?: 0
                        val countText = if (searchState.query.isNotBlank()) " ($count)" else ""

                        SearchFilterChip(
                            category = cat,
                            isSelected = isSelected,
                            label = "${cat.label}$countText",
                            onClick = {
                                haptics.trigger(HapticType.SELECTION_NOTCH)
                                viewModel.onSearchCategorySelected(cat)
                            }
                        )
                    }
                }
            }

            // Results Sub-header
            if (searchState.query.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Results",
                            fontFamily = OutfitFontFamily,
                            color = Color(0x99E9D5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color(0x66A855F7))
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x553B0764))
                                .border(1.dp, Color(0x40A855F7), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${searchState.results.size} items found",
                                fontFamily = JetBrainsMonoFontFamily,
                                color = Color(0xFFC084FC),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Relevance",
                            fontFamily = JetBrainsMonoFontFamily,
                            color = Color(0x88C084FC),
                            fontSize = 11.sp
                        )
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = null,
                            tint = Color(0x88C084FC),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Search Content / Results List
            if (searchState.isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFC084FC),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else if (searchState.query.isBlank()) {
                // Empty query guide
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0x333B0764))
                                .border(1.dp, Color(0x40A855F7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFFC084FC),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "Instant File Search",
                            color = Color.White,
                            fontFamily = OutfitFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Type to search documents, folders, archives, and media across storage",
                            color = Color(0x88E9D5FF),
                            fontFamily = OutfitFontFamily,
                            fontSize = 12.5.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else if (searchState.results.isEmpty()) {
                // No matches state
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
                        Text(
                            text = "No items found",
                            color = Color.White,
                            fontFamily = OutfitFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "No files or folders matched \"${searchState.query}\"",
                            color = Color(0x88E9D5FF),
                            fontFamily = OutfitFontFamily,
                            fontSize = 12.5.sp
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
                    itemsIndexed(searchState.results, key = { _, item -> item.path }) { index, item ->
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
                            SearchResultRow(
                                item = item,
                                query = searchState.query,
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
                                        viewModel.selectTab(NavTab.FILES)
                                    } else if (ArchiveManager.isArchive(File(item.path))) {
                                        archiveItemForOptions = item
                                    } else {
                                        AppFileOpener.openFile(context, item.path, item.mimeType, showMessage)
                                    }
                                },
                                onCopy = {
                                    viewModel.copyItem(item.path, File(item.path).parent ?: "")
                                    showMessage("Copied to clipboard")
                                },
                                onCut = {
                                    viewModel.cutItem(item.path, File(item.path).parent ?: "")
                                    showMessage("Cut to clipboard")
                                },
                                onRename = {
                                    itemToRename = item
                                },
                                onShare = {
                                    shareFileToSystem(context, item.path, item.mimeType, showMessage)
                                },
                                onDetails = {
                                    selectedItemForDetails = item
                                },
                                onDelete = {
                                    itemToDelete = item
                                },
                                onCompress = {
                                    itemForCompress = item
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

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }

    selectedItemForDetails?.let { item ->
        FileDetailsModalDialog(
            name = item.name,
            path = item.path,
            sizeBytes = item.sizeBytes,
            formattedSize = item.formattedSize,
            dateModifiedMs = item.dateModifiedMs,
            isDirectory = item.isDirectory,
            onDismiss = { selectedItemForDetails = null }
        )
    }

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

    itemToDelete?.let { item ->
        EnterpriseDeleteConfirmDialog(
            name = item.name,
            path = item.path,
            isDirectory = item.isDirectory,
            formattedSize = item.formattedSize,
            onDismiss = { itemToDelete = null },
            onConfirmDelete = {
                itemToDelete = null
                viewModel.deleteItem(item.path) { res ->
                    if (res.isSuccess) showMessage("Deleted ${item.name}")
                    else showMessage("Failed to delete")
                }
            }
        )
    }

    if (showBatchDeleteDialog) {
        val selectedItemsInfo = searchState.results
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
                viewModel.deleteMultipleItems(selectedPaths.toList()) { res ->
                    res.onSuccess {
                        showMessage("Permanently deleted $count items")
                    }.onFailure {
                        showMessage("Failed to delete: ${it.message}")
                    }
                }
                selectedPaths = emptySet()
            }
        )
    }

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
                val firstParent = searchState.results.firstOrNull { it.path in selectedPaths }?.let { File(it.path).parentFile }
                val destFile = File(firstParent ?: context.filesDir, name)
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

    itemForCompress?.let { item ->
        CompressDialog(
            initialName = "${item.name}.zip",
            onDismiss = { itemForCompress = null },
            onCompress = { archiveName, type, level ->
                itemForCompress = null
                val parentDir = File(item.path).parentFile ?: File(item.path)
                val destFile = File(parentDir, archiveName)
                showMessage("Compressing ${item.name} to $archiveName...")
                viewModel.createArchive(listOf(item.path), destFile, type, level) { res ->
                    res.onSuccess {
                        showMessage("Created archive \"${it.name}\"")
                    }.onFailure {
                        showMessage("Compression failed: ${it.message}")
                    }
                }
            }
        )
    }

    archiveItemForOptions?.let { item ->
        ArchiveOptionsDialog(
            name = item.name,
            path = item.path,
            onDismiss = { archiveItemForOptions = null },
            onBrowseInPlace = {
                archiveItemForOptions = null
                viewModel.openDirectory(item.path)
                viewModel.selectTab(NavTab.FILES)
            },
            onExtractHere = {
                archiveItemForOptions = null
                val targetDir = File(item.path).parent ?: ""
                showMessage("Extracting ${item.name}...")
                viewModel.extractArchive(item.path, targetDir) { res ->
                    res.onSuccess { count -> showMessage("Extracted $count files") }
                        .onFailure { showMessage("Extract failed: ${it.message}") }
                }
            },
            onExtractToFolder = {
                archiveItemForOptions = null
                val baseName = item.name.substringBeforeLast('.')
                val targetDir = File(File(item.path).parentFile, baseName).absolutePath
                showMessage("Extracting to $baseName...")
                viewModel.extractArchive(item.path, targetDir) { res ->
                    res.onSuccess { count -> showMessage("Extracted $count files to $baseName") }
                        .onFailure { showMessage("Extract failed: ${it.message}") }
                }
            },
            onTestIntegrity = {
                archiveItemForOptions = null
                showMessage("Testing integrity of ${item.name}...")
                viewModel.testArchiveIntegrity(item.path) { res ->
                    res.onSuccess { showMessage(it) }
                        .onFailure { showMessage("Integrity check failed: ${it.message}") }
                }
            }
        )
    }
}

@Composable
fun SearchFilterChip(
    category: SearchFilterCategory,
    isSelected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val (icon, iconColor) = when (category) {
        SearchFilterCategory.ALL -> Pair(Icons.Default.CheckCircle, Color.White)
        SearchFilterCategory.DOCUMENTS -> Pair(Icons.Default.Description, Color(0xFFF472B6))
        SearchFilterCategory.IMAGES -> Pair(Icons.Default.Image, Color(0xFF22D3EE))
        SearchFilterCategory.AUDIO -> Pair(Icons.Default.Mic, Color(0xFF34D399))
        SearchFilterCategory.FOLDERS -> Pair(Icons.Default.Folder, Color(0xFFC084FC))
    }

    val targetBorder = if (isSelected) Color(0xFFD8B4FE) else Color(0x33A855F7)
    val animatedBorder by animateColorAsState(
        targetValue = targetBorder,
        animationSpec = MotionTokens.resolveTween(durationMillis = MotionTokens.DurationFast),
        label = "FilterChipBorder"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) {
                    Brush.horizontalGradient(listOf(Color(0xFF9333EA), Color(0xFFA855F7)))
                } else {
                    SolidColor(Color(0xFF18112C))
                }
            )
            .border(
                1.dp,
                animatedBorder,
                RoundedCornerShape(20.dp)
            )
            .elasticBounce(
                targetScale = 0.90f,
                hapticType = HapticType.SELECTION_NOTCH,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else iconColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xCCDDD6FE),
                fontSize = 11.5.sp,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultRow(
    item: SearchResultItem,
    query: String,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit,
    onCompress: () -> Unit,
    onExtract: (() -> Unit)?,
    onTestArchive: (() -> Unit)?
) {
    val haptics = LocalAppHaptics.current
    var menuExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scanner = remember { StorageScanner(context) }
    val dirItem = remember(item) {
        val protection = SecurityProtectionManager.checkProtection(item.path)
        val visualType = if (item.isDirectory) {
            scanner.detectFolderVisualType(item.name, item.path)
        } else {
            scanner.detectFileVisualType(item.name, item.mimeType)
        }
        DirectoryItem(
            name = item.name,
            path = item.path,
            isDirectory = item.isDirectory,
            sizeBytes = item.sizeBytes,
            formattedSize = item.formattedSize,
            childCount = 0,
            fileCountLabel = "${item.formattedSize} · ${item.parentFolder}",
            dateModifiedMs = item.dateModifiedMs,
            formattedDate = item.formattedDate,
            visualType = visualType,
            mimeType = item.mimeType,
            isProtected = protection.isProtected,
            protectionLevel = protection.level,
            protectionReason = protection.description
        )
    }

    val targetBg = if (isSelected) Color(0xFF2C1345) else Color(0xFF160F29)
    val animatedBg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = MotionTokens.resolveTween(durationMillis = MotionTokens.DurationFast),
        label = "SearchRowBg"
    )
    val targetBorderColor = if (isSelected) Color(0xFFC084FC) else Color(0x30A855F7)
    val animatedBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = MotionTokens.resolveTween(durationMillis = MotionTokens.DurationFast),
        label = "SearchRowBorder"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(animatedBg)
            .border(
                if (isSelected) 1.5.dp else 1.dp,
                animatedBorderColor,
                RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        onClick()
                    }
                },
                onLongClick = {
                    haptics.trigger(HapticType.SELECTION_NOTCH)
                    onLongClick()
                }
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated Checkbox for batch multi-selection
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = scaleIn(initialScale = 0.0f, animationSpec = MotionTokens.SpringCheckboxPop) + fadeIn(animationSpec = tween(120)),
                exit = scaleOut(targetScale = 0.0f, animationSpec = tween(100)) + fadeOut(animationSpec = tween(100))
            ) {
                Row {
                    SelectionCheckbox(
                        isSelected = isSelected,
                        onClick = onToggleSelect
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }

            // Rich Brand, Filetype & Protected Icon
            FileIconSquircle(
                item = dirItem,
                size = 44.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Subtitle
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Matched substring text highlighting
                Text(
                    text = highlightQuery(item.name, query),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Subtitle metadata
                val subText = if (item.isDirectory) {
                    "${item.parentFolder} · Modified ${item.formattedDate}"
                } else {
                    "${item.formattedSize} · ${item.parentFolder} · ${item.formattedDate}"
                }
                Text(
                    text = subText,
                    color = Color(0x77E9D5FF),
                    fontSize = 11.5.sp,
                    fontFamily = OutfitFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3-dot options menu (hidden during multi-selection mode)
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
                        onOpen = onClick,
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

fun highlightQuery(text: String, query: String): AnnotatedString {
    if (query.isBlank()) {
        return AnnotatedString(text, SpanStyle(color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = OutfitFontFamily))
    }

    val lowerText = text.lowercase(Locale.ROOT)
    val lowerQuery = query.lowercase(Locale.ROOT)
    val startIndex = lowerText.indexOf(lowerQuery)

    if (startIndex == -1) {
        return AnnotatedString(text, SpanStyle(color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = OutfitFontFamily))
    }

    val endIndex = startIndex + query.length

    return buildAnnotatedString {
        append(text.substring(0, startIndex))
        pushStyle(
            SpanStyle(
                color = Color(0xFFC084FC),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.5.sp,
                fontFamily = OutfitFontFamily
            )
        )
        append(text.substring(startIndex, endIndex))
        pop()
        append(text.substring(endIndex))
    }
}

data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
