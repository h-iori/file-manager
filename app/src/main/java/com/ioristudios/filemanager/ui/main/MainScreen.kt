package com.ioristudios.filemanager.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import com.ioristudios.filemanager.theme.LocalMotionSensitivity
import com.ioristudios.filemanager.theme.MotionTokens
import com.ioristudios.filemanager.ui.components.elasticBounce
import com.ioristudios.filemanager.ui.components.staggeredEntrance
import com.ioristudios.filemanager.ui.components.liquidBorderGlow
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import com.ioristudios.filemanager.ui.components.ApkIconManager
import com.ioristudios.filemanager.ui.components.FileOperationProgressModal
import com.ioristudios.filemanager.ui.components.FloatingProgressPill
import com.ioristudios.filemanager.ui.components.FileDetailsModalDialog
import com.ioristudios.filemanager.ui.components.GlassContextMenu
import com.ioristudios.filemanager.ui.components.GlassIconButton
import com.ioristudios.filemanager.ui.components.ArchiveOptionsDialog
import com.ioristudios.filemanager.ui.components.CompressDialog
import com.ioristudios.filemanager.ui.directory.RenameDialog
import com.ioristudios.filemanager.ui.directory.DeleteConfirmDialog
import com.ioristudios.filemanager.data.ArchiveManager
import com.ioristudios.filemanager.data.ArchiveType
import com.ioristudios.filemanager.data.CompressionLevel
import java.io.File
import com.ioristudios.filemanager.haptics.HapticType
import com.ioristudios.filemanager.haptics.LocalAppHaptics
import com.ioristudios.filemanager.ui.components.StorageDonutChart
import com.ioristudios.filemanager.ui.components.shareFileToSystem
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ioristudios.filemanager.R
import com.ioristudios.filemanager.data.CategoryItem
import com.ioristudios.filemanager.data.FileCategory
import com.ioristudios.filemanager.data.HomeScreenData
import com.ioristudios.filemanager.data.RecentFile
import com.ioristudios.filemanager.data.StorageOverview
import com.ioristudios.filemanager.theme.BgDeepDark
import com.ioristudios.filemanager.theme.BorderSubtle
import com.ioristudios.filemanager.theme.CardBorderPurple
import com.ioristudios.filemanager.theme.*
import com.ioristudios.filemanager.ui.directory.DirectoryScreen
import com.ioristudios.filemanager.ui.cleanup.CleanupScreen
import com.ioristudios.filemanager.ui.settings.SettingsScreen
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Usb
import com.ioristudios.filemanager.data.StorageVolumeInfo
import com.ioristudios.filemanager.data.StorageVolumeType
import com.ioristudios.filemanager.ui.components.AppFileOpener
import com.ioristudios.filemanager.ui.components.AppManagerDialog
import com.ioristudios.filemanager.ui.search.SearchScreen
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val homeState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val clipboardState by viewModel.clipboardState.collectAsStateWithLifecycle()
    val fileOpProgress by viewModel.fileOpProgress.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var userDismissedModalToBackground by remember { mutableStateOf(false) }
    var showCleanup by remember { mutableStateOf(false) }

    LaunchedEffect(fileOpProgress.operationId) {
        userDismissedModalToBackground = false
    }

    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    // Handle back button for subfolder navigation, files tab, search tab, and settings tab return
    BackHandler(enabled = showCleanup) {
        showCleanup = false
    }

    BackHandler(enabled = selectedTab != NavTab.HOME && !showCleanup) {
        when (selectedTab) {
            NavTab.FILES -> {
                if (!viewModel.navigateUpDirectory()) {
                    viewModel.selectTab(NavTab.HOME)
                }
            }
            NavTab.SEARCH -> {
                viewModel.clearSearch()
                viewModel.selectTab(NavTab.HOME)
            }
            else -> {
                viewModel.selectTab(NavTab.HOME)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FilesTheme.colors.bg)
    ) {
        val motion = LocalMotionSensitivity.current
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                if (motion.isReducedMotion) {
                    fadeIn(tween(90)) togetherWith fadeOut(tween(90))
                } else {
                    val forward = targetState.ordinal > initialState.ordinal
                    if (forward) {
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth / 4 },
                            animationSpec = MotionTokens.SpringSpatialOffset
                        ) + fadeIn(tween(180)) + scaleIn(initialScale = 0.96f)) togetherWith
                        (slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth / 4 },
                            animationSpec = MotionTokens.SpringSpatialOffset
                        ) + fadeOut(tween(150)) + scaleOut(targetScale = 1.04f))
                    } else {
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth / 4 },
                            animationSpec = MotionTokens.SpringSpatialOffset
                        ) + fadeIn(tween(180)) + scaleIn(initialScale = 1.04f)) togetherWith
                        (slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth / 4 },
                            animationSpec = MotionTokens.SpringSpatialOffset
                        ) + fadeOut(tween(150)) + scaleOut(targetScale = 0.96f))
                    }
                }
            },
            label = "SpatialTabNavigation"
        ) { currentTab ->
            when (currentTab) {
                NavTab.HOME -> {
                    HomeScreenContent(
                        viewModel = viewModel,
                        state = homeState,
                        showMessage = showMessage,
                        onViewAllClick = { viewModel.selectTab(NavTab.FILES) },
                        onSearchClick = { viewModel.selectTab(NavTab.SEARCH) },
                        onSettingsClick = { viewModel.selectTab(NavTab.SETTINGS) },
                        onCleanupClick = { showCleanup = true },
                        onSelectVolume = { volumeId -> viewModel.selectStorageVolume(volumeId) }
                    )
                }
                NavTab.FILES -> {
                    DirectoryScreen(
                        viewModel = viewModel,
                        showMessage = showMessage,
                        onSearchClick = { viewModel.selectTab(NavTab.SEARCH) },
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    )
                }
                NavTab.SEARCH -> {
                    SearchScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            viewModel.clearSearch()
                            viewModel.selectTab(NavTab.HOME)
                        },
                        showMessage = showMessage,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    )
                }
                NavTab.SHARE -> {
                    PlaceholderScreen(
                        title = "Share",
                        subtitle = "Wireless high-speed file transfer coming soon",
                        icon = Icons.Default.Share
                    )
                }
                NavTab.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        showMessage = showMessage,
                        onBackClick = { viewModel.selectTab(NavTab.HOME) },
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    )
                }
            }
        }

        // Floating Capsule Bottom Navigation (Present across screens matching Stitch design)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp)
        ) {
            FloatingBottomNav(
                selectedTab = selectedTab,
                onTabSelect = { tab ->
                    if (tab == NavTab.SHARE) {
                        showMessage("Wireless share coming soon")
                    } else {
                        viewModel.selectTab(tab)
                    }
                }
            )
        }

        // Snackbar Host (Dynamic vertical elevation above BottomNav and Paste Capsule avoiding overlaps)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = if (clipboardState.isActive) 148.dp else 84.dp)
        )

        // Glassmorphic File Operation Progress Modal
        if (fileOpProgress.isActive && !userDismissedModalToBackground) {
            FileOperationProgressModal(
                progress = fileOpProgress,
                onPause = { viewModel.pauseFileOperation() },
                onResume = { viewModel.resumeFileOperation() },
                onCancel = { viewModel.cancelFileOperation() },
                onDismissToBackground = {
                    userDismissedModalToBackground = true
                }
            )
        }

        AnimatedVisibility(
            visible = userDismissedModalToBackground && fileOpProgress.isActive,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 10.dp)
        ) {
            FloatingProgressPill(
                progress = fileOpProgress,
                onClick = { userDismissedModalToBackground = false }
            )
        }

        if (showCleanup) {
            CleanupScreen(
                onBack = { showCleanup = false },
                onComplete = showMessage
            )
        }
    }
}

@Composable
fun HomeScreenContent(
    viewModel: MainScreenViewModel,
    state: HomeScreenData,
    showMessage: (String) -> Unit,
    onViewAllClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCleanupClick: () -> Unit,
    onSelectVolume: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedRecentFileForDetails by remember { mutableStateOf<RecentFile?>(null) }
    var itemToRename by remember { mutableStateOf<RecentFile?>(null) }
    var itemToDelete by remember { mutableStateOf<RecentFile?>(null) }
    var itemForCompress by remember { mutableStateOf<RecentFile?>(null) }
    var archiveItemForOptions by remember { mutableStateOf<RecentFile?>(null) }
    var showAppManager by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // App Header
        AppHeader(
            onSearchClick = onSearchClick,
            showMenuButton = false
        )

        // Scrollable Content
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Section 1: Storage Hero Card (With live SD Card & USB OTG Switcher if present)
            item {
                StorageHeroCard(
                    storage = state.storage,
                    availableVolumes = state.availableVolumes,
                    selectedVolumeId = state.selectedVolumeId,
                    onSelectVolume = onSelectVolume,
                    onCardClick = onViewAllClick
                )
            }

            // Section 2: Storage Breakdown
            item {
                StorageBreakdownSection(
                    categories = state.categories,
                    onCategoryClick = { cat ->
                        if (cat.title.equals("Apps", ignoreCase = true)) {
                            showAppManager = true
                        } else {
                            onViewAllClick()
                        }
                    },
                    onViewAllClick = onViewAllClick
                )
            }

            // Section 3: Smart Cleanup Recommendation
            item {
                CleanupRecommendationCard(
                    reclaimableFormatted = state.reclaimableFormatted,
                    onCleanupClick = {
                        onCleanupClick()
                    }
                )
            }

            // Section 4: Recent Files
            item {
                RecentFilesSection(
                    recentFiles = state.recentFiles,
                    onFileClick = { file ->
                        if (ArchiveManager.isArchive(File(file.path))) {
                            archiveItemForOptions = file
                        } else {
                            AppFileOpener.openFile(context, file.path, file.mimeType, showMessage)
                        }
                    },
                    onFileOpenClick = { file ->
                        if (ArchiveManager.isArchive(File(file.path))) {
                            archiveItemForOptions = file
                        } else {
                            AppFileOpener.openFile(context, file.path, file.mimeType, showMessage)
                        }
                    },
                    onFileDetailsClick = { file ->
                        selectedRecentFileForDetails = file
                    },
                    onFileShareClick = { file ->
                        shareFileToSystem(context, file.path, file.mimeType, showMessage)
                    },
                    onFileDeleteClick = { file ->
                        itemToDelete = file
                    },
                    onFileCopyClick = { file ->
                        viewModel.copyItem(file.path, File(file.path).parent ?: "")
                        showMessage("Copied to clipboard")
                    },
                    onFileCutClick = { file ->
                        viewModel.cutItem(file.path, File(file.path).parent ?: "")
                        showMessage("Cut to clipboard")
                    },
                    onFileRenameClick = { file ->
                        itemToRename = file
                    },
                    onFileCompressClick = { file ->
                        itemForCompress = file
                    },
                    onFileExtractClick = { file ->
                        archiveItemForOptions = file
                    },
                    onFileTestArchiveClick = { file ->
                        showMessage("Testing integrity of ${file.name}...")
                        viewModel.testArchiveIntegrity(file.path) { res ->
                            res.onSuccess { msg -> showMessage(msg) }
                                .onFailure { err -> showMessage("Integrity check failed: ${err.message}") }
                        }
                    },
                    onSeeAllClick = onViewAllClick
                )
            }

            item {
                Spacer(modifier = Modifier.height(96.dp))
            }
        }
    }

    // Recent File Details Modal
    selectedRecentFileForDetails?.let { file ->
        FileDetailsModalDialog(
            name = file.name,
            path = file.path,
            sizeBytes = file.sizeBytes,
            formattedSize = file.formattedSize,
            dateModifiedMs = file.dateModifiedMs,
            isDirectory = false,
            onDismiss = { selectedRecentFileForDetails = null }
        )
    }

    itemToRename?.let { item ->
        val siblings = remember(item.path) {
            java.io.File(item.path).parentFile?.list()?.toSet() ?: emptySet()
        }
        RenameDialog(
            initialName = item.name,
            isDirectory = false,
            existingNames = siblings,
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
        DeleteConfirmDialog(
            itemName = item.name,
            isDirectory = false,
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

    // Installed App Manager & APK Extractor Dialog
    if (showAppManager) {
        AppManagerDialog(
            viewModel = viewModel,
            showMessage = showMessage,
            onDismiss = { showAppManager = false }
        )
    }
}

@Composable
fun PlaceholderScreen(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(FilesTheme.colors.chipBg)
                .border(1.dp, FilesTheme.colors.chipBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FilesTheme.colors.primaryAccent,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = FilesTheme.colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            color = FilesTheme.colors.textSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun AppHeader(
    onSearchClick: () -> Unit,
    showMenuButton: Boolean = false,
    onMenuClick: () -> Unit = {},
    menuContent: (@Composable ColumnScope.(dismiss: () -> Unit) -> Unit)? = null
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            // High-End Luxury Brand Icon Squircle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(FilesTheme.colors.headerIconBgStart, FilesTheme.colors.headerIconBgEnd)
                        )
                    )
                    .border(
                        1.2.dp,
                        FilesTheme.colors.headerIconBorder,
                        RoundedCornerShape(13.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_brand_logo),
                    contentDescription = "Files Logo",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // "Files" with 3D Purple Extrusion and Neon Glow matching AnyDoc style
                Box {
                    // Layer 1: Diffuse Neon Aura Glow (Dark only)
                    if (FilesTheme.isDark) {
                        Text(
                            text = "Files",
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color(0xCCE9D5FF),
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color(0xFFA855F7),
                                    offset = Offset(0f, 0f),
                                    blurRadius = 20f
                                )
                            ),
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Layer 2: 3D Extrusion Drop Layer (Offset Down-Right)
                    Text(
                        text = "Files",
                        modifier = Modifier.offset(x = 1.5.dp, y = 1.8.dp),
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = if (FilesTheme.isDark) Color(0xFF7E22CE) else Color(0xFFCBD5E1),
                        letterSpacing = 0.5.sp
                    )

                    // Layer 3: Crisp Surface
                    Text(
                        text = "Files",
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = if (FilesTheme.isDark) Color.White else TextPrimaryLight,
                        letterSpacing = 0.5.sp
                    )
                }

                // Subtitle: "by IORI STUDIOS"
                Text(
                    text = "by IORI STUDIOS",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.5.sp,
                    color = if (FilesTheme.isDark) Color(0xFFD8B4FE) else TextSecondaryLight,
                    style = TextStyle(
                        shadow = Shadow(
                            color = if (FilesTheme.isDark) Color(0x80A855F7) else Color.Transparent,
                            blurRadius = 6f
                        )
                    ),
                    letterSpacing = 1.2.sp
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                icon = Icons.Outlined.Search,
                contentDescription = "Search",
                onClick = onSearchClick,
                hapticType = HapticType.CLICK
            )
            if (showMenuButton) {
                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    GlassIconButton(
                        icon = Icons.Outlined.MoreVert,
                        contentDescription = "More",
                        onClick = {
                            if (menuContent != null) {
                                menuExpanded = true
                            } else {
                                onMenuClick()
                            }
                        },
                        hapticType = HapticType.MENU_POP
                    )

                    if (menuContent != null) {
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier
                                .width(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(FilesTheme.colors.dialogBg)
                                .border(
                                    1.dp,
                                    FilesTheme.colors.cardBorder,
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(vertical = 4.dp)
                        ) {
                            menuContent { menuExpanded = false }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StorageHeroCard(
    storage: StorageOverview,
    availableVolumes: List<StorageVolumeInfo> = emptyList(),
    selectedVolumeId: String = "internal",
    onSelectVolume: (String) -> Unit = {},
    onCardClick: () -> Unit
) {
    val haptics = LocalAppHaptics.current
    val currentVolume = availableVolumes.find { it.id == selectedVolumeId } ?: availableVolumes.firstOrNull()
    val headerTitle = when (currentVolume?.type) {
        StorageVolumeType.SD_CARD -> "SD Card storage"
        StorageVolumeType.USB_OTG -> "USB OTG storage"
        else -> "Internal storage"
    }
    val headerBadge = when (currentVolume?.type) {
        StorageVolumeType.SD_CARD -> "MicroSD"
        StorageVolumeType.USB_OTG -> "USB 3.0"
        else -> null
    }
    val headerIcon = when (currentVolume?.type) {
        StorageVolumeType.SD_CARD -> Icons.Default.SdCard
        StorageVolumeType.USB_OTG -> Icons.Default.Usb
        else -> Icons.Default.Storage
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredEntrance(index = 0)
            .liquidBorderGlow()
            .elasticBounce(targetScale = 0.94f, hapticType = HapticType.FOLDER_OPEN, onClick = onCardClick)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(FilesTheme.colors.cardBgStart, FilesTheme.colors.cardBgEnd)
                )
            )
            .border(1.dp, FilesTheme.colors.cardBorder.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Title & Optional External Media Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = headerIcon,
                        contentDescription = "Storage",
                        tint = FilesTheme.colors.primaryAccent,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = headerTitle,
                        fontFamily = OutfitFontFamily,
                        color = FilesTheme.colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (headerBadge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(FilesTheme.colors.chipBg)
                            .border(1.dp, FilesTheme.colors.chipBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = headerBadge,
                            fontFamily = OutfitFontFamily,
                            color = FilesTheme.colors.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Your device storage at a glance",
                fontFamily = OutfitFontFamily,
                color = FilesTheme.colors.textSecondary,
                fontSize = 12.sp
            )

            // Storage Volumes Switcher (Displayed ONLY if SD Card or USB OTG is present; hidden by default otherwise)
            if (availableVolumes.size > 1) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (FilesTheme.isDark) Color(0x18FFFFFF) else Color(0x0A000000))
                        .border(1.dp, FilesTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableVolumes.forEach { vol ->
                        val isSelected = vol.id == selectedVolumeId
                        val (vIcon, vLabel) = when (vol.type) {
                            StorageVolumeType.INTERNAL -> Icons.Default.Storage to "Internal"
                            StorageVolumeType.SD_CARD -> Icons.Default.SdCard to "SD Card"
                            StorageVolumeType.USB_OTG -> Icons.Default.Usb to "USB OTG"
                        }

                        val targetBg = if (isSelected) (if (FilesTheme.isDark) Color(0xFF331A60) else Color(0xFFEDE4FF))
                                       else Color.Transparent
                        val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = tween(MotionTokens.DurationFast), label = "VolBg")
                        val targetBorderColor = if (isSelected) FilesTheme.colors.primaryAccent.copy(alpha = 0.8f) else Color.Transparent
                        val animatedBorderColor by animateColorAsState(targetValue = targetBorderColor, animationSpec = tween(MotionTokens.DurationFast), label = "VolBorder")

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .elasticBounce(
                                    targetScale = 0.94f,
                                    hapticType = HapticType.SELECTION_NOTCH,
                                    onClick = { onSelectVolume(vol.id) }
                                )
                                .clip(RoundedCornerShape(9.dp))
                                .background(animatedBg)
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = animatedBorderColor,
                                    shape = RoundedCornerShape(9.dp)
                                )
                                .padding(vertical = 7.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = vIcon,
                                    contentDescription = vLabel,
                                    tint = if (isSelected) FilesTheme.colors.primaryAccent else FilesTheme.colors.textMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = vLabel,
                                    fontFamily = OutfitFontFamily,
                                    color = if (isSelected) FilesTheme.colors.textPrimary else FilesTheme.colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Compact Split Card: Donut Chart on Left, 8-Category Breakdown on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut Chart (Full 360° dynamic fill with all categories + system + available space)
                StorageDonutChart(
                    storage = storage,
                    size = 142.dp,
                    strokeWidth = 14.dp
                )

                // Breakdown list with 8 distinguishable categories + complete units (MB, GB)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    CategoryMetricRow("Photos", formatShortBytes(storage.photosBytes), ChartPhotosCyan)
                    CategoryMetricRow("Videos", formatShortBytes(storage.videosBytes), ChartVideosPurple)
                    CategoryMetricRow("Audio", formatShortBytes(storage.audioBytes), ChartAudioLime)
                    CategoryMetricRow("Docs", formatShortBytes(storage.documentsBytes), ChartDocsPink)
                    CategoryMetricRow("Downloads", formatShortBytes(storage.downloadsBytes), ChartDownloadsAmber)
                    CategoryMetricRow("Apps", formatShortBytes(storage.appsBytes), ChartAppsOrange)
                    CategoryMetricRow("System", formatShortBytes(storage.systemBytes), ChartSystemSlate)
                    CategoryMetricRow("Other", formatShortBytes(storage.otherBytes), Color(0xFF6366F1))
                    CategoryMetricRow("Available", formatShortBytes(storage.freeBytes), ChartAvailableEmerald)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Status Summary
            HorizontalDivider(
                color = if (FilesTheme.isDark) Color(0x18FFFFFF) else Color(0x18000000),
                thickness = 0.8.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ChartAvailableEmerald)
                    )
                    Text(
                        text = "${storage.formattedFree} available",
                        fontFamily = OutfitFontFamily,
                        color = FilesTheme.colors.textPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "${storage.formattedTotal} total",
                    fontFamily = OutfitFontFamily,
                    color = FilesTheme.colors.textMuted,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun CategoryMetricRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                color = FilesTheme.colors.textSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = OutfitFontFamily
            )
        }
        Text(
            text = value,
            color = FilesTheme.colors.textPrimary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = OutfitFontFamily
        )
    }
}

@Composable
fun StorageBreakdownSection(
    categories: List<CategoryItem>,
    onCategoryClick: (CategoryItem) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Storage breakdown",
                fontFamily = OutfitFontFamily,
                color = FilesTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.clickable(onClick = onViewAllClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "View all",
                    fontFamily = OutfitFontFamily,
                    color = FilesTheme.colors.primaryAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = FilesTheme.colors.primaryAccent,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        val chunked = categories.chunked(2)
        chunked.forEachIndexed { rowIndex, rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEachIndexed { colIndex, item ->
                    val itemIndex = rowIndex * 2 + colIndex
                    CategoryCard(
                        item = item,
                        modifier = Modifier
                            .weight(1f)
                            .staggeredEntrance(index = itemIndex + 1),
                        onClick = { onCategoryClick(item) }
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    item: CategoryItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (icon, iconTint, iconBg, gradStart, gradEnd) = when (item.category) {
        FileCategory.IMAGE -> if (FilesTheme.isDark) {
            CategoryVisuals(
                Icons.Default.PhotoLibrary,
                ChartPhotosCyan,
                Color(0xFF00E5FF).copy(alpha = 0.2f),
                Color(0xFF00E5FF),
                Color(0xFF0284C7)
            )
        } else {
            CategoryVisuals(
                Icons.Default.PhotoLibrary,
                PhotosCyanLightTint,
                PhotosCyanLightBg,
                Color(0xFF0284C7),
                Color(0xFF0369A1)
            )
        }
        FileCategory.VIDEO -> if (FilesTheme.isDark) {
            CategoryVisuals(
                Icons.Default.Movie,
                ChartVideosPurple,
                Color(0xFF8B5CF6).copy(alpha = 0.2f),
                Color(0xFF8B5CF6),
                Color(0xFF6D28D9)
            )
        } else {
            CategoryVisuals(
                Icons.Default.Movie,
                VideosVioletLightTint,
                VideosVioletLightBg,
                Color(0xFF7C3AED),
                Color(0xFF6D28D9)
            )
        }
        FileCategory.DOCUMENT -> if (FilesTheme.isDark) {
            CategoryVisuals(
                Icons.Default.Description,
                ChartDocsPink,
                Color(0xFFF43F5E).copy(alpha = 0.2f),
                Color(0xFFF43F5E),
                Color(0xFFBE123C)
            )
        } else {
            CategoryVisuals(
                Icons.Default.Description,
                DocsRoseLightTint,
                DocsRoseLightBg,
                Color(0xFFE11D48),
                Color(0xFFBE123C)
            )
        }
        FileCategory.AUDIO -> if (FilesTheme.isDark) {
            CategoryVisuals(
                Icons.Default.Audiotrack,
                ChartAudioLime,
                Color(0xFFA3E635).copy(alpha = 0.2f),
                Color(0xFFA3E635),
                Color(0xFF65A30D)
            )
        } else {
            CategoryVisuals(
                Icons.Default.Audiotrack,
                AudioEmeraldLightTint,
                AudioEmeraldLightBg,
                Color(0xFF059669),
                Color(0xFF047857)
            )
        }
        FileCategory.ARCHIVE -> if (FilesTheme.isDark) {
            CategoryVisuals(
                Icons.Outlined.Download,
                ChartDownloadsAmber,
                Color(0xFFF59E0B).copy(alpha = 0.2f),
                Color(0xFFF59E0B),
                Color(0xFFB45309)
            )
        } else {
            CategoryVisuals(
                Icons.Outlined.Download,
                FolderAmberLightTint,
                FolderAmberLightBg,
                Color(0xFFD97706),
                Color(0xFFB45309)
            )
        }
        FileCategory.OTHER -> if (FilesTheme.isDark) {
            CategoryVisuals(
                Icons.Default.Folder,
                Color(0xFF818CF8),
                Color(0xFF6366F1).copy(alpha = 0.2f),
                Color(0xFF818CF8),
                Color(0xFF4338CA)
            )
        } else {
            CategoryVisuals(
                Icons.Default.Folder,
                Color(0xFF4F46E5),
                Color(0xFFE0E7FF),
                Color(0xFF4F46E5),
                Color(0xFF3730A3)
            )
        }
        else -> if (item.title.equals("System", ignoreCase = true)) {
            if (FilesTheme.isDark) {
                CategoryVisuals(
                    Icons.Default.Settings,
                    ChartSystemSlate,
                    Color(0xFF94A3B8).copy(alpha = 0.2f),
                    Color(0xFF94A3B8),
                    Color(0xFF475569)
                )
            } else {
                CategoryVisuals(
                    Icons.Default.Settings,
                    SystemSlateLightTint,
                    SystemSlateLightBg,
                    Color(0xFF475569),
                    Color(0xFF334155)
                )
            }
        } else {
            if (FilesTheme.isDark) {
                CategoryVisuals(
                    Icons.Default.Apps,
                    ChartAppsOrange,
                    Color(0xFFFF5722).copy(alpha = 0.2f),
                    Color(0xFFFF5722),
                    Color(0xFFC2410C)
                )
            } else {
                CategoryVisuals(
                    Icons.Default.Apps,
                    AppsOrangeLightTint,
                    AppsOrangeLightBg,
                    Color(0xFFEA580C),
                    Color(0xFFC2410C)
                )
            }
        }
    }

    Box(
        modifier = modifier
            .elasticBounce(targetScale = 0.94f, hapticType = HapticType.FOLDER_OPEN, onClick = onClick)
            .clip(RoundedCornerShape(18.dp))
            .background(FilesTheme.colors.surface)
            .border(1.dp, FilesTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg)
                        .border(1.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = item.title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = item.fileCountLabel,
                    fontFamily = OutfitFontFamily,
                    color = FilesTheme.colors.textSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = item.title,
                fontFamily = OutfitFontFamily,
                color = FilesTheme.colors.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.formattedSize,
                fontFamily = OutfitFontFamily,
                color = FilesTheme.colors.textSecondary,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            val animProgressFraction by animateFloatAsState(
                targetValue = item.progressFraction,
                animationSpec = tween(durationMillis = 850, easing = MotionTokens.EaseFluidOut),
                label = "CategoryProgressFraction"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (FilesTheme.isDark) Color(0xFF241A42) else Color(0xFFF1F5F9))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animProgressFraction.coerceIn(0.02f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(listOf(gradStart, gradEnd))
                        )
                )
            }
        }
    }
}

data class CategoryVisuals(
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val gradStart: Color,
    val gradEnd: Color
)

@Composable
fun CleanupRecommendationCard(
    reclaimableFormatted: String,
    onCleanupClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = FilesTheme.colors.primaryAccent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Free up space",
                fontFamily = OutfitFontFamily,
                color = FilesTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(FilesTheme.colors.bannerBgStart, FilesTheme.colors.bannerBgEnd)
                    )
                )
                .border(1.dp, FilesTheme.colors.cardBorder.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF9333EA).copy(alpha = 0.25f))
                            .border(1.dp, FilesTheme.colors.primaryAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "Cleanup",
                            tint = FilesTheme.colors.primaryAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ready for a cleanup?",
                            fontFamily = OutfitFontFamily,
                            color = FilesTheme.colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Find unnecessary files and reclaim valuable storage.",
                            fontFamily = OutfitFontFamily,
                            color = FilesTheme.colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(FilesTheme.colors.chipBg)
                            .border(1.dp, FilesTheme.colors.chipBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Reclaimable: ",
                                fontFamily = OutfitFontFamily,
                                color = FilesTheme.colors.textSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = reclaimableFormatted,
                                fontFamily = OutfitFontFamily,
                                color = if (FilesTheme.isDark) ChartPhotosCyan else Color(0xFF0284C7),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .elasticBounce(targetScale = 0.93f, hapticType = HapticType.HERO_CLEAN_PULSE, onClick = onCleanupClick)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF9333EA), PurpleContainer)
                                )
                            )
                            .border(1.dp, FilesTheme.colors.primaryAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Clean",
                                fontFamily = OutfitFontFamily,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentFilesSection(
    recentFiles: List<RecentFile>,
    onFileClick: (RecentFile) -> Unit,
    onFileOpenClick: (RecentFile) -> Unit,
    onFileDetailsClick: (RecentFile) -> Unit,
    onFileShareClick: (RecentFile) -> Unit,
    onFileDeleteClick: (RecentFile) -> Unit,
    onFileCopyClick: (RecentFile) -> Unit,
    onFileCutClick: (RecentFile) -> Unit,
    onFileRenameClick: (RecentFile) -> Unit,
    onFileCompressClick: (RecentFile) -> Unit,
    onFileExtractClick: (RecentFile) -> Unit,
    onFileTestArchiveClick: (RecentFile) -> Unit,
    onSeeAllClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent files",
                fontFamily = OutfitFontFamily,
                color = FilesTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.elasticBounce(targetScale = 0.94f, hapticType = HapticType.CLICK, onClick = onSeeAllClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "See all",
                    fontFamily = OutfitFontFamily,
                    color = FilesTheme.colors.primaryAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = FilesTheme.colors.primaryAccent,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            recentFiles.forEach { file ->
                val isArchive = ArchiveManager.isArchive(File(file.path))
                RecentFileRow(
                    file = file,
                    onClick = { onFileClick(file) },
                    onOpen = { onFileOpenClick(file) },
                    onDetails = { onFileDetailsClick(file) },
                    onShare = { onFileShareClick(file) },
                    onDelete = { onFileDeleteClick(file) },
                    onCopy = { onFileCopyClick(file) },
                    onCut = { onFileCutClick(file) },
                    onRename = { onFileRenameClick(file) },
                    onCompress = { onFileCompressClick(file) },
                    onExtract = if (isArchive) { { onFileExtractClick(file) } } else null,
                    onTestArchive = if (isArchive) { { onFileTestArchiveClick(file) } } else null
                )
            }
        }
    }
}

@Composable
fun RecentFileRow(
    file: RecentFile,
    onClick: () -> Unit,
    onOpen: () -> Unit,
    onDetails: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: () -> Unit,
    onCompress: () -> Unit,
    onExtract: (() -> Unit)?,
    onTestArchive: (() -> Unit)?
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val (icon, iconTint, iconBg) = when (file.category) {
        FileCategory.IMAGE -> if (FilesTheme.isDark) {
            Triple(Icons.Default.Image, ElectricPurple, Color(0xFF20173D))
        } else {
            Triple(Icons.Default.Image, PhotosCyanLightTint, PhotosCyanLightBg)
        }
        FileCategory.DOCUMENT -> if (FilesTheme.isDark) {
            Triple(Icons.Default.PictureAsPdf, PdfRed, PdfRedBg)
        } else {
            Triple(Icons.Default.PictureAsPdf, DocsRoseLightTint, DocsRoseLightBg)
        }
        FileCategory.AUDIO -> if (FilesTheme.isDark) {
            Triple(Icons.Default.GraphicEq, ElectricMagenta, Color(0xFF3B0764))
        } else {
            Triple(Icons.Default.GraphicEq, AudioEmeraldLightTint, AudioEmeraldLightBg)
        }
        FileCategory.ARCHIVE -> if (FilesTheme.isDark) {
            Triple(Icons.Default.FolderZip, ZipAmber, ZipAmberBg)
        } else {
            Triple(Icons.Default.FolderZip, FolderAmberLightTint, FolderAmberLightBg)
        }
        FileCategory.SPREADSHEET -> if (FilesTheme.isDark) {
            Triple(Icons.Default.TableChart, ExcelGreen, ExcelGreenBg)
        } else {
            Triple(Icons.Default.TableChart, AudioEmeraldLightTint, AudioEmeraldLightBg)
        }
        FileCategory.VIDEO -> if (FilesTheme.isDark) {
            Triple(Icons.Default.PlayCircle, ElectricCyan, Color(0xFF0C4A6E))
        } else {
            Triple(Icons.Default.PlayCircle, VideosVioletLightTint, VideosVioletLightBg)
        }
        else -> if (FilesTheme.isDark) {
            Triple(Icons.AutoMirrored.Filled.InsertDriveFile, TextSecondary, Color(0xFF20173D))
        } else {
            Triple(Icons.AutoMirrored.Filled.InsertDriveFile, SystemSlateLightTint, SystemSlateLightBg)
        }
    }

    val haptics = LocalAppHaptics.current
    val context = LocalContext.current
    val isApk = file.name.endsWith(".apk", ignoreCase = true)
    var apkBitmap by remember(file.path) {
        mutableStateOf(if (isApk) ApkIconManager.getCached(file.path) else null)
    }

    if (isApk && apkBitmap == null) {
        LaunchedEffect(file.path) {
            apkBitmap = ApkIconManager.loadApkIcon(context, file.path)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .elasticBounce(targetScale = 0.98f, hapticType = HapticType.CLICK, onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg)
                .border(1.dp, iconTint.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (apkBitmap != null) {
                Image(
                    bitmap = apkBitmap!!,
                    contentDescription = file.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (file.category == FileCategory.IMAGE) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color(0xFF0C0819))
                        .border(1.dp, PurpleContainer.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = ElectricPurple,
                        modifier = Modifier.size(9.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                color = FilesTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${file.formattedDate} · ${file.formattedSize}",
                color = FilesTheme.colors.textSecondary,
                fontSize = 11.sp
            )
        }

        Box {
            IconButton(
                onClick = {
                    haptics.trigger(HapticType.MENU_POP)
                    menuExpanded = true
                },
                modifier = Modifier.size(32.dp)
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

@Composable
fun FloatingBottomNav(
    selectedTab: NavTab,
    onTabSelect: (NavTab) -> Unit
) {
    val motion = LocalMotionSensitivity.current
    val tabs = remember { listOf(NavTab.HOME, NavTab.FILES, NavTab.SHARE, NavTab.SETTINGS) }
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    val tabWidth = 62.dp
    val tabHeight = 46.dp

    val indicatorOffset by animateDpAsState(
        targetValue = tabWidth * selectedIndex,
        animationSpec = if (motion.isReducedMotion) tween(80) else MotionTokens.SpringPillSlide,
        label = "BottomNavPillOffset"
    )

    Surface(
        color = FilesTheme.colors.bottomNavBg,
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, FilesTheme.colors.bottomNavBorder),
        shadowElevation = 14.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Smooth Sliding Active Pill Indicator
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .height(tabHeight)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                FilesTheme.colors.bottomNavActiveBg,
                                FilesTheme.colors.primaryAccent.copy(alpha = 0.28f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        FilesTheme.colors.primaryAccent.copy(alpha = 0.5f),
                        RoundedCornerShape(999.dp)
                    )
            )

            // Tabs
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isActive = tab == selectedTab
                    val icon = when (tab) {
                        NavTab.HOME -> Icons.Default.Home
                        NavTab.FILES -> Icons.Default.FolderOpen
                        NavTab.SHARE -> Icons.Default.Share
                        NavTab.SETTINGS -> Icons.Default.Settings
                        else -> Icons.Default.Home
                    }
                    val label = when (tab) {
                        NavTab.HOME -> "Home"
                        NavTab.FILES -> "Files"
                        NavTab.SHARE -> "Share"
                        NavTab.SETTINGS -> "Settings"
                        else -> ""
                    }

                    BottomNavTabItem(
                        isActive = isActive,
                        icon = icon,
                        contentDescription = label,
                        onClick = { onTabSelect(tab) },
                        modifier = Modifier
                            .width(tabWidth)
                            .height(tabHeight)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavTabItem(
    isActive: Boolean,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motion = LocalMotionSensitivity.current

    val iconScale by animateFloatAsState(
        targetValue = if (isActive && !motion.isReducedMotion) 1.20f else 1.0f,
        animationSpec = MotionTokens.SpringBouncyJuicy,
        label = "BottomNavIconScale"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isActive) FilesTheme.colors.bottomNavActiveIcon else FilesTheme.colors.bottomNavInactiveIcon,
        animationSpec = MotionTokens.resolveTween(durationMillis = MotionTokens.DurationFast),
        label = "BottomNavIconTint"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .elasticBounce(
                targetScale = 0.88f,
                hapticType = HapticType.NAV_TICK,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
            if (isActive) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(FilesTheme.colors.bottomNavActiveIcon)
                )
            }
        }
    }
}

private fun formatShortBytes(bytes: Long): String {
    if (bytes <= 0) return "0 GB"
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 1.0) {
        String.format(Locale.US, "%.1f GB", gb)
    } else {
        val mb = bytes / (1024.0 * 1024.0)
        if (mb >= 1.0) {
            String.format(Locale.US, "%.1f MB", mb)
        } else {
            val kb = bytes / 1024.0
            if (kb >= 1.0) {
                String.format(Locale.US, "%.1f KB", kb)
            } else {
                "$bytes B"
            }
        }
    }
}

private fun openFile(context: Context, file: RecentFile, showMessage: (String) -> Unit) {
    AppFileOpener.openFile(context, file.path, file.mimeType, showMessage)
}
