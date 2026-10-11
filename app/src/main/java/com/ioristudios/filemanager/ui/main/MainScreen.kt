package com.ioristudios.filemanager.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.ioristudios.filemanager.ui.components.FileDetailsModalDialog
import com.ioristudios.filemanager.ui.components.GlassContextMenu
import com.ioristudios.filemanager.ui.components.GlassIconButton
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
import com.ioristudios.filemanager.ui.settings.SettingsScreen
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val homeState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    // Handle back button for subfolder navigation, files tab, and settings tab return
    BackHandler(enabled = selectedTab != NavTab.HOME) {
        if (selectedTab == NavTab.FILES) {
            if (!viewModel.navigateUpDirectory()) {
                viewModel.selectTab(NavTab.HOME)
            }
        } else {
            viewModel.selectTab(NavTab.HOME)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FilesTheme.colors.bg)
    ) {
        Crossfade(targetState = selectedTab, label = "TabCrossfade") { currentTab ->
            when (currentTab) {
                NavTab.HOME -> {
                    HomeScreenContent(
                        state = homeState,
                        showMessage = showMessage,
                        onViewAllClick = { viewModel.selectTab(NavTab.FILES) },
                        onSettingsClick = { viewModel.selectTab(NavTab.SETTINGS) }
                    )
                }
                NavTab.FILES -> {
                    DirectoryScreen(
                        viewModel = viewModel,
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

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}

@Composable
fun HomeScreenContent(
    state: HomeScreenData,
    showMessage: (String) -> Unit,
    onViewAllClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedRecentFileForDetails by remember { mutableStateOf<RecentFile?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // App Header
        AppHeader(
            onSearchClick = { showMessage("Global file search coming soon") },
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

            // Section 1: Storage Hero Card
            item {
                StorageHeroCard(
                    storage = state.storage,
                    onCardClick = onViewAllClick
                )
            }

            // Section 2: Storage Breakdown
            item {
                StorageBreakdownSection(
                    categories = state.categories,
                    onCategoryClick = { onViewAllClick() },
                    onViewAllClick = onViewAllClick
                )
            }

            // Section 3: Smart Cleanup Recommendation
            item {
                CleanupRecommendationCard(
                    reclaimableFormatted = state.reclaimableFormatted,
                    onCleanupClick = {
                        showMessage("Deep clean engine coming soon in next update!")
                    }
                )
            }

            // Section 4: Recent Files
            item {
                RecentFilesSection(
                    recentFiles = state.recentFiles,
                    onFileClick = { file ->
                        openFile(context, file, showMessage)
                    },
                    onFileOpenClick = { file ->
                        openFile(context, file, showMessage)
                    },
                    onFileDetailsClick = { file ->
                        selectedRecentFileForDetails = file
                    },
                    onFileShareClick = { file ->
                        shareFileToSystem(context, file.path, file.mimeType, showMessage)
                    },
                    onFileDeleteClick = { file ->
                        showMessage("Deleting ${file.name} is restricted in preview")
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
    onMenuClick: () -> Unit = {}
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
                GlassIconButton(
                    icon = Icons.Outlined.MoreVert,
                    contentDescription = "More",
                    onClick = onMenuClick,
                    hapticType = HapticType.MENU_POP
                )
            }
        }
    }
}

@Composable
fun StorageHeroCard(
    storage: StorageOverview,
    onCardClick: () -> Unit
) {
    val haptics = LocalAppHaptics.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(FilesTheme.colors.cardBgStart, FilesTheme.colors.cardBgEnd)
                )
            )
            .border(1.dp, FilesTheme.colors.cardBorder.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .clickable {
                haptics.trigger(HapticType.FOLDER_OPEN)
                onCardClick()
            }
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Title & Chip
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
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Storage",
                        tint = FilesTheme.colors.primaryAccent,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = "Internal storage",
                        fontFamily = OutfitFontFamily,
                        color = FilesTheme.colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(FilesTheme.colors.chipBg)
                        .border(1.dp, FilesTheme.colors.chipBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NVMe 4.0",
                        fontFamily = OutfitFontFamily,
                        color = FilesTheme.colors.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Your device storage at a glance",
                fontFamily = OutfitFontFamily,
                color = FilesTheme.colors.textSecondary,
                fontSize = 12.sp
            )

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
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { item ->
                    CategoryCard(
                        item = item,
                        modifier = Modifier.weight(1f),
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

    val haptics = LocalAppHaptics.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(FilesTheme.colors.surface)
            .border(1.dp, FilesTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .clickable {
                haptics.trigger(HapticType.FOLDER_OPEN)
                onClick()
            }
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (FilesTheme.isDark) Color(0xFF241A42) else Color(0xFFF1F5F9))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.progressFraction)
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

                    val haptics = LocalAppHaptics.current

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF9333EA), PurpleContainer)
                                )
                            )
                            .border(1.dp, FilesTheme.colors.primaryAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable {
                                haptics.trigger(HapticType.HERO_CLEAN_PULSE)
                                onCleanupClick()
                            }
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
                modifier = Modifier.clickable(onClick = onSeeAllClick),
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
                RecentFileRow(
                    file = file,
                    onClick = { onFileClick(file) },
                    onOpen = { onFileOpenClick(file) },
                    onDetails = { onFileDetailsClick(file) },
                    onShare = { onFileShareClick(file) },
                    onDelete = { onFileDeleteClick(file) }
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
    onDelete: () -> Unit
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptics.trigger(HapticType.CLICK)
                onClick()
            }
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
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
                onDetails = onDetails,
                onShare = onShare,
                onDelete = onDelete
            )
        }
    }
}

@Composable
fun FloatingBottomNav(
    selectedTab: NavTab,
    onTabSelect: (NavTab) -> Unit
) {
    Surface(
        color = FilesTheme.colors.bottomNavBg,
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, FilesTheme.colors.bottomNavBorder),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Destination 1: Home
            BottomNavTabItem(
                isActive = selectedTab == NavTab.HOME,
                icon = Icons.Default.Home,
                contentDescription = "Home",
                onClick = { onTabSelect(NavTab.HOME) }
            )

            // Destination 2: Files / Directory Explorer
            BottomNavTabItem(
                isActive = selectedTab == NavTab.FILES,
                icon = Icons.Default.FolderOpen,
                contentDescription = "Files",
                onClick = { onTabSelect(NavTab.FILES) }
            )

            // Destination 3: Share
            BottomNavTabItem(
                isActive = selectedTab == NavTab.SHARE,
                icon = Icons.Default.Share,
                contentDescription = "Share",
                onClick = { onTabSelect(NavTab.SHARE) }
            )

            // Destination 4: Settings
            BottomNavTabItem(
                isActive = selectedTab == NavTab.SETTINGS,
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                onClick = { onTabSelect(NavTab.SETTINGS) }
            )
        }
    }
}

@Composable
fun BottomNavTabItem(
    isActive: Boolean,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val haptics = LocalAppHaptics.current
    val handleTabClick = {
        haptics.trigger(HapticType.NAV_TICK)
        onClick()
    }

    if (isActive) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(FilesTheme.colors.bottomNavActiveBg)
                .clickable(onClick = handleTabClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = FilesTheme.colors.bottomNavActiveIcon,
                    modifier = Modifier.size(22.dp)
                )
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(FilesTheme.colors.bottomNavActiveIcon)
                )
            }
        }
    } else {
        IconButton(onClick = handleTabClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = FilesTheme.colors.bottomNavInactiveIcon,
                modifier = Modifier.size(22.dp)
            )
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
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val contentUri = if (file.path.isNotBlank()) {
            val localFile = File(file.path)
            if (localFile.exists()) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    localFile
                )
            } else file.uri
        } else file.uri

        if (contentUri != Uri.EMPTY) {
            val mime = if (file.mimeType.isNotBlank()) file.mimeType else "*/*"
            intent.setDataAndType(contentUri, mime)
            context.startActivity(intent)
        } else {
            showMessage("File: ${file.name}")
        }
    } catch (e: Exception) {
        showMessage("Cannot open ${file.name}: ${e.message}")
    }
}
