package com.ioristudios.filemanager.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import com.ioristudios.filemanager.theme.LocalMotionSensitivity
import com.ioristudios.filemanager.theme.MotionTokens
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ioristudios.filemanager.data.StorageOverview
import com.ioristudios.filemanager.haptics.HapticType
import com.ioristudios.filemanager.haptics.LocalAppHaptics
import com.ioristudios.filemanager.theme.CardBorderPurple
import com.ioristudios.filemanager.theme.ChartPhotosCyan
import com.ioristudios.filemanager.theme.ChartVideosPurple
import com.ioristudios.filemanager.theme.ChartAudioLime
import com.ioristudios.filemanager.theme.ChartDocsPink
import com.ioristudios.filemanager.theme.ChartDownloadsAmber
import com.ioristudios.filemanager.theme.ChartAppsOrange
import com.ioristudios.filemanager.theme.ChartSystemSlate
import com.ioristudios.filemanager.theme.ChartAvailableEmerald
import com.ioristudios.filemanager.theme.ElectricCyan
import com.ioristudios.filemanager.theme.ElectricLavender
import com.ioristudios.filemanager.theme.ElectricMagenta
import com.ioristudios.filemanager.theme.ElectricPurple
import com.ioristudios.filemanager.theme.ElectricTeal
import com.ioristudios.filemanager.theme.ElectricAmber
import com.ioristudios.filemanager.theme.FilesTheme
import com.ioristudios.filemanager.theme.OutfitFontFamily
import com.ioristudios.filemanager.theme.PurpleContainer
import com.ioristudios.filemanager.theme.TextMuted
import com.ioristudios.filemanager.theme.TextPrimary
import com.ioristudios.filemanager.theme.TextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DonutCategorySlice(
    val label: String,
    val bytes: Long,
    val color: Color
)

/**
 * Calculates sweep angles for all slices ensuring active categories have a minimum visible slice (minDegrees)
 * while the entire 360° circle is 100% dynamically filled completely by all categories + available space.
 */
fun calculateDonutAngles(
    slices: List<DonutCategorySlice>,
    totalDegrees: Float = 360f,
    minDegrees: Float = 5.0f
): List<Float> {
    val nonZeroSlices = slices.filter { it.bytes > 0L }
    if (nonZeroSlices.isEmpty()) return slices.map { 0f }
    if (nonZeroSlices.size == 1) {
        return slices.map { if (it.bytes > 0L) totalDegrees else 0f }
    }

    val totalBytes = slices.sumOf { it.bytes }.toFloat().coerceAtLeast(1f)
    val rawAngles = slices.map { (it.bytes.toFloat() / totalBytes) * totalDegrees }

    val activeIndices = slices.indices.filter { slices[it].bytes > 0L }
    val needsAdjustment = activeIndices.any { rawAngles[it] < minDegrees }

    if (!needsAdjustment) {
        return rawAngles
    }

    // Allocate minDegrees to small active categories, distribute remainder proportionally to larger categories
    var fixedDegreesSum = 0f
    var flexibleBytesSum = 0L
    val fixedIndices = mutableSetOf<Int>()

    for (i in activeIndices) {
        if (rawAngles[i] < minDegrees) {
            fixedDegreesSum += minDegrees
            fixedIndices.add(i)
        } else {
            flexibleBytesSum += slices[i].bytes
        }
    }

    val remainingDegrees = (totalDegrees - fixedDegreesSum).coerceAtLeast(0f)

    return slices.indices.map { i ->
        when {
            slices[i].bytes <= 0L -> 0f
            i in fixedIndices -> minDegrees
            flexibleBytesSum > 0L -> (slices[i].bytes.toFloat() / flexibleBytesSum.toFloat()) * remainingDegrees
            else -> minDegrees
        }
    }
}

/**
 * Circular Donut Storage Chart drawing real storage usage by category + system + available space,
 * dynamically filling the complete 360° circle with distinct, high-contrast colors.
 */
@Composable
fun StorageDonutChart(
    storage: StorageOverview,
    modifier: Modifier = Modifier,
    size: Dp = 142.dp,
    strokeWidth: Dp = 14.dp
) {
    val motion = LocalMotionSensitivity.current
    val animProgress = remember { Animatable(if (motion.isReducedMotion) 1f else 0f) }
    val centerScale = remember { Animatable(if (motion.isReducedMotion) 1f else 0.75f) }

    LaunchedEffect(storage.usedBytes) {
        if (!motion.isReducedMotion) {
            animProgress.snapTo(0f)
            centerScale.snapTo(0.75f)
            launch {
                centerScale.animateTo(
                    targetValue = 1f,
                    animationSpec = MotionTokens.SpringOvershoot
                )
            }
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1100, easing = MotionTokens.EaseFluidOut)
            )
        } else {
            animProgress.snapTo(1f)
            centerScale.snapTo(1f)
        }
    }

    val slices = remember(storage) {
        listOf(
            DonutCategorySlice("Photos", storage.photosBytes, ChartPhotosCyan),
            DonutCategorySlice("Videos", storage.videosBytes, ChartVideosPurple),
            DonutCategorySlice("Audio", storage.audioBytes, ChartAudioLime),
            DonutCategorySlice("Docs", storage.documentsBytes, ChartDocsPink),
            DonutCategorySlice("Downloads", storage.downloadsBytes, ChartDownloadsAmber),
            DonutCategorySlice("Apps", storage.appsBytes, ChartAppsOrange),
            DonutCategorySlice("System", storage.systemBytes, ChartSystemSlate),
            DonutCategorySlice("Other", storage.otherBytes, Color(0xFF6366F1)),
            DonutCategorySlice("Available", storage.freeBytes, ChartAvailableEmerald)
        )
    }

    val angles = remember(slices) {
        calculateDonutAngles(slices, totalDegrees = 360f, minDegrees = 5.0f)
    }

    val glassTrackColor = FilesTheme.colors.glassTrackColor

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val canvasSize = this.size.minDimension
            val radius = (canvasSize - strokePx) / 2f
            val centerOffset = androidx.compose.ui.geometry.Offset(this.size.width / 2f, this.size.height / 2f)

            // Background circular track
            drawCircle(
                color = glassTrackColor,
                radius = radius,
                center = centerOffset,
                style = Stroke(width = strokePx)
            )

            // Category Slices dynamically filling the complete 360° circle with dynamic sweep
            var currentAngle = -90f
            val gapDeg = 1.8f
            val progress = animProgress.value

            slices.forEachIndexed { index, slice ->
                val deg = angles.getOrElse(index) { 0f }
                if (deg > 0.5f) {
                    val actualSweep = maxOf(1f, deg - gapDeg) * progress
                    val startAngle = currentAngle + (gapDeg / 2f)

                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = actualSweep,
                        useCenter = false,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    currentAngle += deg
                }
            }
        }

        // Center Hero Metric with animated scale pop
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = centerScale.value
                scaleY = centerScale.value
                alpha = centerScale.value.coerceIn(0f, 1f)
            }
        ) {
            Text(
                text = "USED",
                color = FilesTheme.colors.primaryAccent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OutfitFontFamily,
                letterSpacing = 1.8.sp
            )
            Text(
                text = storage.formattedUsed,
                color = FilesTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = OutfitFontFamily,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "${storage.usedPercent.toInt()}%",
                color = if (FilesTheme.isDark) ChartPhotosCyan else Color(0xFF0284C7),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = OutfitFontFamily
            )
        }
    }
}

/**
 * Reusable elastic press modifier for custom interactive components using an existing interactionSource.
 */
@Composable
fun Modifier.elasticPress(
    interactionSource: MutableInteractionSource,
    targetScale: Float = 0.92f
): Modifier {
    val motion = LocalMotionSensitivity.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        if (motion.isReducedMotion) {
            scale.snapTo(1f)
            return@LaunchedEffect
        }
        if (isPressed) {
            scale.animateTo(targetScale, MotionTokens.SpringElasticPress)
        } else {
            scale.animateTo(1.0f, MotionTokens.SpringBouncyJuicy)
        }
    }

    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/**
 * Reusable elastic bounce modifier applying Apple Vision / iOS physical touch feedback.
 * Compresses down to targetScale on press and springs back through overshoot on release.
 */
fun Modifier.elasticBounce(
    targetScale: Float = 0.92f,
    hapticType: HapticType = HapticType.CLICK,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalAppHaptics.current
    val motion = LocalMotionSensitivity.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isPressed, enabled) {
        if (!enabled || motion.isReducedMotion) {
            scale.snapTo(1f)
            return@LaunchedEffect
        }
        if (isPressed) {
            scale.animateTo(targetScale, MotionTokens.SpringElasticPress)
        } else {
            scale.animateTo(1.0f, MotionTokens.SpringBouncyJuicy)
        }
    }

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled
                ) {
                    haptics.trigger(hapticType)
                    onClick()
                }
            } else {
                Modifier
            }
        )
}

/**
 * Cascading staggered entrance modifier sliding in from bottom with spring physics.
 */
fun Modifier.staggeredEntrance(
    index: Int,
    delayPerItem: Int = MotionTokens.StaggerDelayPerItem,
    offsetY: Dp = 24.dp
): Modifier = composed {
    val motion = LocalMotionSensitivity.current
    if (motion.isReducedMotion || index >= 8) return@composed this

    val visibleState = remember { Animatable(0f) }
    val delayMs = (index * delayPerItem).coerceAtMost(120)

    LaunchedEffect(Unit) {
        if (delayMs > 0) {
            kotlinx.coroutines.delay(delayMs.toLong())
        }
        visibleState.animateTo(
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = 0.72f,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
            )
        )
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val offsetPx = with(density) { offsetY.toPx() }

    this.graphicsLayer {
        alpha = visibleState.value.coerceIn(0f, 1f)
        translationY = (1f - visibleState.value) * offsetPx
        scaleX = 0.94f + (0.06f * visibleState.value)
        scaleY = 0.94f + (0.06f * visibleState.value)
    }
}

/**
 * Ambient liquid glowing border sweep around hero cards, containers, and dialogs.
 */
fun Modifier.liquidBorderGlow(
    strokeWidth: Dp = 1.5.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    colors: List<Color> = listOf(
        Color(0xFFC084FC).copy(alpha = 0.8f),
        Color(0xFFA855F7).copy(alpha = 0.2f),
        Color(0xFF38BDF8).copy(alpha = 0.8f),
        Color(0xFFC084FC).copy(alpha = 0.8f)
    )
): Modifier = composed {
    val motion = LocalMotionSensitivity.current
    if (motion.isReducedMotion) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "LiquidBorderGlow")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    this.drawWithContent {
        drawContent()
        val strokePx = strokeWidth.toPx()
        val brush = Brush.sweepGradient(
            colors = colors,
            center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        )
        drawRoundRect(
            brush = brush,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
            style = Stroke(width = strokePx)
        )
    }
}

/**
 * Sweeping specular glass shimmer refraction highlight across cards and surfaces.
 */
fun Modifier.visionGlassShimmer(
    active: Boolean = true,
    shimmerColor: Color = Color.White.copy(alpha = 0.08f)
): Modifier = composed {
    val motion = LocalMotionSensitivity.current
    if (!active || motion.isReducedMotion) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "GlassShimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GlassShimmerOffset"
    )

    this.drawWithContent {
        drawContent()
        val width = size.width
        val height = size.height
        val startX = shimmerOffset * width
        val brush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                shimmerColor,
                Color.Transparent
            ),
            start = androidx.compose.ui.geometry.Offset(startX, 0f),
            end = androidx.compose.ui.geometry.Offset(startX + width * 0.45f, height)
        )
        drawRect(brush = brush)
    }
}

/**
 * Reusable frosted glass circular icon button for headers with tactile elastic bounce.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hapticType: HapticType = HapticType.CLICK
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (FilesTheme.isDark) Color(0x14FFFFFF) else Color(0xFFF1F5F9))
            .border(1.dp, if (FilesTheme.isDark) Color(0x1EFFFFFF) else Color(0xFFE2E8F0), CircleShape)
            .elasticBounce(targetScale = 0.88f, hapticType = hapticType, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = FilesTheme.colors.textPrimary,
            modifier = Modifier.size(19.dp)
        )
    }
}

/**
 * Beautified Floating Glass Context Menu anchored directly next to the 3-dot button.
 */
@Composable
fun GlassContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onOpen: () -> Unit,
    onDetails: () -> Unit,
    onShare: () -> Unit,
    onCopy: (() -> Unit)? = null,
    onCut: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onCompress: (() -> Unit)? = null,
    onExtract: (() -> Unit)? = null,
    onTestArchive: (() -> Unit)? = null
) {
    val haptics = LocalAppHaptics.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .width(175.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(FilesTheme.colors.dialogBg)
            .border(
                1.dp,
                FilesTheme.colors.cardBorder,
                RoundedCornerShape(16.dp)
            )
            .padding(vertical = 4.dp)
    ) {
        // Open
        GlassContextMenuItem(
            icon = Icons.Default.OpenInNew,
            iconTint = if (FilesTheme.isDark) ElectricCyan else Color(0xFF0284C7),
            iconBg = if (FilesTheme.isDark) Color(0xFF0284C7).copy(alpha = 0.22f) else Color(0xFFE0F2FE),
            label = "Open",
            onClick = {
                haptics.trigger(HapticType.CLICK)
                onDismissRequest()
                onOpen()
            }
        )

        // Copy
        if (onCopy != null) {
            GlassContextMenuItem(
                icon = Icons.Default.ContentCopy,
                iconTint = if (FilesTheme.isDark) ElectricPurple else Color(0xFF7C3AED),
                iconBg = if (FilesTheme.isDark) Color(0xFF581C87).copy(alpha = 0.22f) else Color(0xFFF3E8FF),
                label = "Copy",
                onClick = {
                    haptics.trigger(HapticType.CLICK)
                    onDismissRequest()
                    onCopy()
                }
            )
        }

        // Cut / Move
        if (onCut != null) {
            GlassContextMenuItem(
                icon = Icons.Default.ContentCut,
                iconTint = if (FilesTheme.isDark) ElectricAmber else Color(0xFFD97706),
                iconBg = if (FilesTheme.isDark) Color(0xFF78350F).copy(alpha = 0.22f) else Color(0xFFFEF3C7),
                label = "Cut (Move)",
                onClick = {
                    haptics.trigger(HapticType.CLICK)
                    onDismissRequest()
                    onCut()
                }
            )
        }

        // Rename
        if (onRename != null) {
            GlassContextMenuItem(
                icon = Icons.Default.DriveFileRenameOutline,
                iconTint = if (FilesTheme.isDark) ElectricTeal else Color(0xFF0D9488),
                iconBg = if (FilesTheme.isDark) Color(0xFF134E4A).copy(alpha = 0.22f) else Color(0xFFCCFBF1),
                label = "Rename",
                onClick = {
                    haptics.trigger(HapticType.CLICK)
                    onDismissRequest()
                    onRename()
                }
            )
        }

        // Details
        GlassContextMenuItem(
            icon = Icons.Default.Info,
            iconTint = FilesTheme.colors.primaryAccent,
            iconBg = if (FilesTheme.isDark) Color(0xFF9333EA).copy(alpha = 0.22f) else Color(0xFFEEF2FF),
            label = "Details",
            onClick = {
                haptics.trigger(HapticType.CLICK)
                onDismissRequest()
                onDetails()
            }
        )

        // Share
        GlassContextMenuItem(
            icon = Icons.Default.Share,
            iconTint = if (FilesTheme.isDark) ElectricTeal else Color(0xFF059669),
            iconBg = if (FilesTheme.isDark) Color(0xFF0F766E).copy(alpha = 0.25f) else Color(0xFFD1FAE5),
            label = "Share",
            onClick = {
                haptics.trigger(HapticType.CLICK)
                onDismissRequest()
                onShare()
            }
        )

        // Extract
        if (onExtract != null) {
            GlassContextMenuItem(
                icon = Icons.Default.Unarchive,
                iconTint = if (FilesTheme.isDark) ElectricCyan else Color(0xFF0284C7),
                iconBg = if (FilesTheme.isDark) Color(0xFF0284C7).copy(alpha = 0.22f) else Color(0xFFE0F2FE),
                label = "Extract...",
                onClick = {
                    haptics.trigger(HapticType.CLICK)
                    onDismissRequest()
                    onExtract()
                }
            )
        }

        // Test Archive
        if (onTestArchive != null) {
            GlassContextMenuItem(
                icon = Icons.Default.Security,
                iconTint = if (FilesTheme.isDark) ElectricMagenta else Color(0xFFDB2777),
                iconBg = if (FilesTheme.isDark) Color(0xFFBE185D).copy(alpha = 0.22f) else Color(0xFFFCE7F3),
                label = "Test Archive",
                onClick = {
                    haptics.trigger(HapticType.CLICK)
                    onDismissRequest()
                    onTestArchive()
                }
            )
        }

        // Compress
        if (onCompress != null) {
            GlassContextMenuItem(
                icon = Icons.Default.Archive,
                iconTint = if (FilesTheme.isDark) ElectricPurple else Color(0xFF9333EA),
                iconBg = if (FilesTheme.isDark) Color(0xFF9333EA).copy(alpha = 0.22f) else Color(0xFFF3E8FF),
                label = "Compress...",
                onClick = {
                    haptics.trigger(HapticType.CLICK)
                    onDismissRequest()
                    onCompress()
                }
            )
        }

        // Divider
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            thickness = 0.8.dp,
            color = if (FilesTheme.isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)
        )

        // Delete
        GlassContextMenuItem(
            icon = Icons.Default.Delete,
            iconTint = Color(0xFFFF5252),
            iconBg = if (FilesTheme.isDark) Color(0xFFB71C1C).copy(alpha = 0.22f) else Color(0xFFFFE4E6),
            label = "Delete",
            isDanger = true,
            onClick = {
                haptics.trigger(HapticType.DESTRUCTIVE)
                onDismissRequest()
                onDelete()
            }
        )
    }
}

@Composable
fun GlassContextMenuItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    label: String,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBg)
                        .border(1.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = label,
                    color = if (isDanger) Color(0xFFFF5252) else FilesTheme.colors.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = if (isDanger) FontWeight.Bold else FontWeight.Medium
                )
            }
        },
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    )
}

/**
 * Universal File Details Dialog for displaying rich device metadata.
 */
@Composable
fun FileDetailsModalDialog(
    name: String,
    path: String,
    sizeBytes: Long,
    formattedSize: String,
    dateModifiedMs: Long,
    isDirectory: Boolean,
    childCount: Int = 0,
    onDismiss: () -> Unit
) {
    val fileObj = File(path)
    val exactBytes = if (isDirectory) sizeBytes else fileObj.length()
    val lastModifiedFormatted = SimpleDateFormat("MMM d, yyyy · h:mm:ss a", Locale.US).format(Date(dateModifiedMs))

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
                        imageVector = if (isDirectory) Icons.Default.Folder else Icons.Default.Info,
                        contentDescription = null,
                        tint = FilesTheme.colors.primaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = if (isDirectory) "Directory Details" else "File Details",
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
                ModalDetailRow(label = "Name", value = name)
                ModalDetailRow(
                    label = "Type",
                    value = if (isDirectory) "Directory ($childCount items)" else "File"
                )
                ModalDetailRow(label = "Location", value = path)
                ModalDetailRow(
                    label = "Size",
                    value = "$formattedSize (${String.format(Locale.US, "%,d bytes", exactBytes)})"
                )
                ModalDetailRow(label = "Modified", value = lastModifiedFormatted)
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF9333EA), PurpleContainer))
                    )
                    .border(1.dp, FilesTheme.colors.primaryAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
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
fun ModalDetailRow(label: String, value: String) {
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

/**
 * Universal file sharing helper via Android ShareSheet and FileProvider.
 */
fun shareFileToSystem(context: Context, path: String, mimeType: String = "*/*", showMessage: (String) -> Unit) {
    try {
        val file = File(path)
        if (!file.exists()) {
            showMessage("File does not exist")
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (mimeType.isNotBlank()) mimeType else "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(intent, "Share ${file.name}").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        showMessage("Cannot share file: ${e.message}")
    }
}

/**
 * Universal batch file sharing helper via Android ShareSheet (ACTION_SEND_MULTIPLE).
 */
fun shareMultipleFilesToSystem(context: Context, paths: List<String>, showMessage: (String) -> Unit) {
    try {
        val files = paths.map { File(it) }.filter { it.exists() && it.isFile }
        if (files.isEmpty()) {
            showMessage("No regular files to share (folders cannot be directly shared)")
            return
        }
        val uris = ArrayList<Uri>()
        for (file in files) {
            uris.add(
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            )
        }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(intent, "Share ${files.size} files").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        showMessage("Cannot share files: ${e.message}")
    }
}

/**
 * Glassmorphic selection checkbox component with fluid state animations.
 */
@Composable
fun SelectionCheckbox(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onBoundsChanged: ((Rect) -> Unit)? = null
) {
    val haptics = LocalAppHaptics.current
    val motion = LocalMotionSensitivity.current
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.85f,
        animationSpec = MotionTokens.SpringCheckboxPop,
        label = "checkboxScale"
    )

    Box(
        modifier = modifier
            .size(24.dp)
            .onGloballyPositioned { coordinates ->
                onBoundsChanged?.invoke(coordinates.boundsInRoot())
            }
            .graphicsLayer {
                if (!motion.isReducedMotion) {
                    scaleX = scale
                    scaleY = scale
                }
            }
            .clip(CircleShape)
            .background(
                if (isSelected) {
                    Brush.linearGradient(listOf(Color(0xFF9333EA), Color(0xFFA855F7)))
                } else {
                    SolidColor(if (FilesTheme.isDark) Color(0x22FFFFFF) else Color(0x11000000))
                }
            )
            .border(
                1.5.dp,
                if (isSelected) Color(0xFFC084FC) else FilesTheme.colors.cardBorder.copy(alpha = 0.8f),
                CircleShape
            )
            .clickable(enabled = onClick != null) {
                haptics.trigger(HapticType.SELECTION_NOTCH)
                onClick?.invoke()
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isSelected,
            enter = scaleIn(initialScale = 0.0f, animationSpec = MotionTokens.SpringCheckboxPop) + fadeIn(animationSpec = tween(120)),
            exit = scaleOut(targetScale = 0.0f, animationSpec = tween(100)) + fadeOut(animationSpec = tween(100))
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier
                    .size(15.dp)
                    .padding(1.dp)
            )
        }
    }
}

/**
 * Contextual Top Action Bar for Multiple File/Folder Selection.
 */
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onSelectAllToggle: () -> Unit,
    isAllSelected: Boolean,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onCompress: () -> Unit,
    onInvertSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalAppHaptics.current
    var overflowExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (FilesTheme.isDark) Color(0xFF1E1038) else Color(0xFFFAF5FF),
                        if (FilesTheme.isDark) Color(0xFF130924) else Color(0xFFF3E8FF)
                    )
                )
            )
            .border(
                1.dp,
                if (FilesTheme.isDark) Color(0x44A855F7) else Color(0x447C3AED),
                RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Dismiss Selection Mode Button & Counter Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (FilesTheme.isDark) Color(0x33A855F7) else Color(0x339333EA))
                        .clickable {
                            haptics.trigger(HapticType.CLICK)
                            onClose()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Selection",
                        tint = FilesTheme.colors.primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "$selectedCount selected",
                        color = FilesTheme.colors.textPrimary,
                        fontFamily = OutfitFontFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$totalCount total items",
                        color = FilesTheme.colors.textMuted,
                        fontFamily = OutfitFontFamily,
                        fontSize = 11.5.sp,
                        maxLines = 1
                    )
                }
            }

            // Action Icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Select All / Deselect All Quick Toggle
                IconButton(
                    onClick = {
                        haptics.trigger(HapticType.SELECTION_NOTCH)
                        onSelectAllToggle()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = if (isAllSelected) "Deselect All" else "Select All",
                        tint = if (isAllSelected) FilesTheme.colors.primaryAccent else FilesTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Copy Action
                IconButton(
                    onClick = {
                        haptics.trigger(HapticType.CLICK)
                        onCopy()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = FilesTheme.colors.textSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Cut / Move Action
                IconButton(
                    onClick = {
                        haptics.trigger(HapticType.CLICK)
                        onCut()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = "Cut",
                        tint = FilesTheme.colors.textSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Share Action
                IconButton(
                    onClick = {
                        haptics.trigger(HapticType.CLICK)
                        onShare()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = FilesTheme.colors.textSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Delete Action (Highlighted with danger tint)
                IconButton(
                    onClick = {
                        haptics.trigger(HapticType.DESTRUCTIVE)
                        onDelete()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 3-Dot Overflow Menu for Batch Operations
                Box {
                    IconButton(
                        onClick = {
                            haptics.trigger(HapticType.MENU_POP)
                            overflowExpanded = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Batch actions",
                            tint = FilesTheme.colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = overflowExpanded,
                        onDismissRequest = { overflowExpanded = false },
                        modifier = Modifier.background(FilesTheme.colors.dialogBg)
                    ) {
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = null,
                                    tint = FilesTheme.colors.primaryAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = if (isAllSelected) "Deselect All" else "Select All",
                                    color = FilesTheme.colors.textPrimary,
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 13.5.sp
                                )
                            },
                            onClick = {
                                overflowExpanded = false
                                onSelectAllToggle()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = "Invert Selection",
                                    color = FilesTheme.colors.textPrimary,
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 13.5.sp
                                )
                            },
                            onClick = {
                                overflowExpanded = false
                                onInvertSelection()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = ElectricAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = "Compress to ZIP",
                                    color = FilesTheme.colors.textPrimary,
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 13.5.sp
                                )
                            },
                            onClick = {
                                overflowExpanded = false
                                onCompress()
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            thickness = 0.8.dp,
                            color = if (FilesTheme.isDark) Color(0x22FFFFFF) else Color(0xFFE2E8F0)
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            text = {
                                Text(
                                    text = "Delete Selected ($selectedCount)",
                                    color = Color(0xFFEF4444),
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                )
                            },
                            onClick = {
                                overflowExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

data class BatchDeleteItemInfo(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val formattedSize: String
)

/**
 * Enterprise-grade batch deletion confirmation dialog with scrollable file list,
 * size breakdown, and explicit danger warnings.
 */
@Composable
fun EnterpriseBatchDeleteDialog(
    items: List<BatchDeleteItemInfo>,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val haptics = LocalAppHaptics.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val motion = LocalMotionSensitivity.current
        var dialogEntered by remember { mutableStateOf(motion.isReducedMotion) }
        LaunchedEffect(Unit) {
            dialogEntered = true
        }
        val dialogScale by animateFloatAsState(
            targetValue = if (dialogEntered) 1.0f else 0.88f,
            animationSpec = MotionTokens.SpringDialogEntrance,
            label = "DialogScale"
        )
        val dialogAlpha by animateFloatAsState(
            targetValue = if (dialogEntered) 1.0f else 0f,
            animationSpec = tween(MotionTokens.DurationFast),
            label = "DialogAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .graphicsLayer {
                    scaleX = dialogScale
                    scaleY = dialogScale
                    alpha = dialogAlpha
                }
                .clip(RoundedCornerShape(24.dp))
                .background(if (FilesTheme.isDark) Color(0xFF160E2A) else Color(0xFFFFFFFF))
                .border(
                    1.2.dp,
                    if (FilesTheme.isDark) Color(0x66A855F7) else Color(0x337C3AED),
                    RoundedCornerShape(24.dp)
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.16f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Delete ${items.size} Items?",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = FilesTheme.colors.textPrimary
                        )
                        Text(
                            text = "Permanent file system deletion",
                            fontFamily = OutfitFontFamily,
                            fontSize = 11.5.sp,
                            color = Color(0xFFEF4444)
                        )
                    }
                }

                // Danger warning banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF7F1D1D).copy(alpha = 0.22f))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "This action cannot be undone. Folder contents will be permanently purged.",
                            color = if (FilesTheme.isDark) Color(0xFFFECACA) else Color(0xFF991B1B),
                            fontSize = 11.5.sp,
                            fontFamily = OutfitFontFamily,
                            lineHeight = 15.sp
                        )
                    }
                }

                Text(
                    text = "Selected for removal (${items.size}):",
                    color = FilesTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Medium
                )

                // Scrollable Item Preview List
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (FilesTheme.isDark) Color(0xFF10081E) else Color(0xFFF1F5F9))
                        .border(1.dp, FilesTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(items) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = if (item.isDirectory) ElectricPurple else FilesTheme.colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = item.name,
                                    color = FilesTheme.colors.textPrimary,
                                    fontSize = 12.5.sp,
                                    fontFamily = OutfitFontFamily,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = item.formattedSize,
                                    color = FilesTheme.colors.textMuted,
                                    fontSize = 11.sp,
                                    fontFamily = OutfitFontFamily
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .elasticBounce(targetScale = 0.92f, onClick = onDismiss)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, FilesTheme.colors.cardBorder.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            color = FilesTheme.colors.textSecondary,
                            fontSize = 13.5.sp,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .elasticBounce(targetScale = 0.92f, hapticType = HapticType.DESTRUCTIVE, onClick = onConfirmDelete)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFDC2626), Color(0xFFEF4444))
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Delete Forever (${items.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            fontFamily = OutfitFontFamily
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enterprise-grade single item deletion confirmation dialog with full details and danger styling.
 */
@Composable
fun EnterpriseDeleteConfirmDialog(
    name: String,
    path: String,
    isDirectory: Boolean,
    formattedSize: String,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val haptics = LocalAppHaptics.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val motion = LocalMotionSensitivity.current
        var dialogEntered by remember { mutableStateOf(motion.isReducedMotion) }
        LaunchedEffect(Unit) {
            dialogEntered = true
        }
        val dialogScale by animateFloatAsState(
            targetValue = if (dialogEntered) 1.0f else 0.88f,
            animationSpec = MotionTokens.SpringDialogEntrance,
            label = "DialogScale"
        )
        val dialogAlpha by animateFloatAsState(
            targetValue = if (dialogEntered) 1.0f else 0f,
            animationSpec = tween(MotionTokens.DurationFast),
            label = "DialogAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .graphicsLayer {
                    scaleX = dialogScale
                    scaleY = dialogScale
                    alpha = dialogAlpha
                }
                .clip(RoundedCornerShape(24.dp))
                .background(if (FilesTheme.isDark) Color(0xFF160E2A) else Color(0xFFFFFFFF))
                .border(
                    1.2.dp,
                    if (FilesTheme.isDark) Color(0x66A855F7) else Color(0x337C3AED),
                    RoundedCornerShape(24.dp)
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.16f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isDirectory) "Delete Folder?" else "Delete File?",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = FilesTheme.colors.textPrimary
                        )
                        Text(
                            text = "Permanent file deletion",
                            fontFamily = OutfitFontFamily,
                            fontSize = 11.5.sp,
                            color = Color(0xFFEF4444)
                        )
                    }
                }

                // Item details card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (FilesTheme.isDark) Color(0xFF10081E) else Color(0xFFF1F5F9))
                        .border(1.dp, FilesTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = null,
                                tint = if (isDirectory) ElectricPurple else ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = name,
                                color = FilesTheme.colors.textPrimary,
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "Location: $path",
                            color = FilesTheme.colors.textMuted,
                            fontFamily = OutfitFontFamily,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (formattedSize.isNotBlank()) {
                            Text(
                                text = "Size: $formattedSize",
                                color = FilesTheme.colors.textSecondary,
                                fontFamily = OutfitFontFamily,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }

                // Danger warning box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF7F1D1D).copy(alpha = 0.22f))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isDirectory) {
                                "Permanently deletes this directory and all contained files. This action cannot be undone."
                            } else {
                                "Are you sure you want to permanently delete this file? This action cannot be reversed."
                            },
                            color = if (FilesTheme.isDark) Color(0xFFFECACA) else Color(0xFF991B1B),
                            fontSize = 11.5.sp,
                            fontFamily = OutfitFontFamily,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .elasticBounce(targetScale = 0.92f, onClick = onDismiss)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, FilesTheme.colors.cardBorder.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            color = FilesTheme.colors.textSecondary,
                            fontSize = 13.5.sp,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .elasticBounce(targetScale = 0.92f, hapticType = HapticType.DESTRUCTIVE, onClick = onConfirmDelete)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFDC2626), Color(0xFFEF4444))
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Delete Forever",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            fontFamily = OutfitFontFamily
                        )
                    }
                }
            }
        }
    }
}

/**
 * Enterprise-grade Rename Dialog with auto-selection of the base name (excluding extension),
 * real-time OS validation (blocking forbidden chars: \ / : * ? " < > |), and inline error reporting.
 */
@Composable
fun EnterpriseRenameDialog(
    initialName: String,
    isDirectory: Boolean,
    existingNames: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    val haptics = LocalAppHaptics.current

    // Compute base name boundary so extension is excluded from initial selection
    val dotIndex = if (!isDirectory) initialName.lastIndexOf('.') else -1
    val baseLength = if (dotIndex > 0) dotIndex else initialName.length

    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialName,
                selection = TextRange(0, baseLength)
            )
        )
    }

    val forbiddenChars = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
    val trimmedName = textFieldValue.text.trim()
    val containsForbidden = forbiddenChars.firstOrNull { trimmedName.contains(it) }
    val isDuplicate = existingNames.any {
        it.equals(trimmedName, ignoreCase = true) && !it.equals(initialName, ignoreCase = true)
    }

    val errorMessage = when {
        trimmedName.isEmpty() -> "Name cannot be empty"
        containsForbidden != null -> "Name cannot contain forbidden character: $containsForbidden"
        trimmedName.startsWith(".") && isDirectory -> "Folder name cannot start with a dot"
        isDuplicate -> "An item named \"$trimmedName\" already exists"
        else -> null
    }

    val isValid = errorMessage == null && trimmedName.isNotEmpty()
    val isChanged = trimmedName != initialName

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val motion = LocalMotionSensitivity.current
        var dialogEntered by remember { mutableStateOf(motion.isReducedMotion) }
        LaunchedEffect(Unit) {
            dialogEntered = true
        }
        val dialogScale by animateFloatAsState(
            targetValue = if (dialogEntered) 1.0f else 0.88f,
            animationSpec = MotionTokens.SpringDialogEntrance,
            label = "DialogScale"
        )
        val dialogAlpha by animateFloatAsState(
            targetValue = if (dialogEntered) 1.0f else 0f,
            animationSpec = tween(MotionTokens.DurationFast),
            label = "DialogAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .graphicsLayer {
                    scaleX = dialogScale
                    scaleY = dialogScale
                    alpha = dialogAlpha
                }
                .clip(RoundedCornerShape(24.dp))
                .background(if (FilesTheme.isDark) Color(0xFF160E2A) else Color(0xFFFFFFFF))
                .border(
                    1.2.dp,
                    if (FilesTheme.isDark) Color(0x66A855F7) else Color(0x337C3AED),
                    RoundedCornerShape(24.dp)
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF9333EA).copy(alpha = 0.22f))
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
                    Column {
                        Text(
                            text = if (isDirectory) "Rename Folder" else "Rename File",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = FilesTheme.colors.textPrimary
                        )
                        Text(
                            text = if (isDirectory) "Enter new directory title" else "Filename extension is preserved",
                            fontFamily = OutfitFontFamily,
                            fontSize = 11.5.sp,
                            color = FilesTheme.colors.textMuted
                        )
                    }
                }

                // Text Field Box
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    textStyle = TextStyle(
                        color = FilesTheme.colors.textPrimary,
                        fontSize = 15.sp,
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(FilesTheme.colors.primaryAccent),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (FilesTheme.isDark) Color(0xFF1E1035) else Color(0xFFF1F5F9))
                        .border(
                            1.5.dp,
                            if (errorMessage != null && textFieldValue.text.isNotEmpty()) {
                                Color(0xFFEF4444)
                            } else if (isChanged) {
                                FilesTheme.colors.primaryAccent
                            } else {
                                FilesTheme.colors.cardBorder
                            },
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                )

                // Inline validation error or tip
                if (errorMessage != null && textFieldValue.text.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = errorMessage,
                            color = Color(0xFFEF4444),
                            fontSize = 11.5.sp,
                            fontFamily = OutfitFontFamily
                        )
                    }
                } else if (!isDirectory && dotIndex > 0) {
                    Text(
                        text = "Tip: Extension \".${initialName.substring(dotIndex + 1)}\" is selected automatically to prevent corruption.",
                        color = FilesTheme.colors.textMuted,
                        fontSize = 11.sp,
                        fontFamily = OutfitFontFamily,
                        lineHeight = 14.sp
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .elasticBounce(targetScale = 0.92f, onClick = onDismiss)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, FilesTheme.colors.cardBorder.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            color = FilesTheme.colors.textSecondary,
                            fontSize = 13.5.sp,
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .elasticBounce(
                                targetScale = 0.92f,
                                hapticType = HapticType.CLICK,
                                enabled = isValid && isChanged,
                                onClick = { onRename(trimmedName) }
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isValid && isChanged) {
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF9333EA), Color(0xFFC084FC))
                                    )
                                } else {
                                    SolidColor(if (FilesTheme.isDark) Color(0x33FFFFFF) else Color(0x11000000))
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Rename",
                            color = if (isValid && isChanged) Color.White else FilesTheme.colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            fontFamily = OutfitFontFamily
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable VisionOS-styled frosted dialog container featuring elastic spring scale
 * and dynamic background scrim blur and fade.
 */
@Composable
fun VisionGlassDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val motion = LocalMotionSensitivity.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(MotionTokens.DurationFast)) + scaleIn(
            initialScale = if (motion.isReducedMotion) 1.0f else 0.88f,
            animationSpec = MotionTokens.SpringDialogEntrance
        ),
        exit = fadeOut(animationSpec = tween(140)) + scaleOut(
            targetScale = if (motion.isReducedMotion) 1.0f else 0.92f,
            animationSpec = tween(140)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.58f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {} // Prevent click-through dismissal
            ) {
                content()
            }
        }
    }
}

/**
 * Liquid Shimmer Progress Bar featuring continuous fluid specular highlight wave,
 * smoothly animated percentage number, and real-time status indication.
 */
@Composable
fun LiquidShimmerProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 10.dp,
    barColors: List<Color> = listOf(Color(0xFF9333EA), Color(0xFFC084FC), Color(0xFF00E5FF)),
    trackColor: Color = if (FilesTheme.isDark) Color(0x33FFFFFF) else Color(0x1A000000)
) {
    val motion = LocalMotionSensitivity.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = MotionTokens.SpringSpatialHierarchy,
        label = "LiquidProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "LiquidWave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor)
    ) {
        // Active Progress Fill
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .background(Brush.horizontalGradient(barColors))
                .drawWithContent {
                    drawContent()
                    if (!motion.isReducedMotion) {
                        val width = size.width
                        val heightPx = size.height
                        val startX = waveOffset * width
                        val shimmerBrush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            start = androidx.compose.ui.geometry.Offset(startX, 0f),
                            end = androidx.compose.ui.geometry.Offset(startX + width * 0.4f, heightPx)
                        )
                        drawRect(brush = shimmerBrush)
                    }
                }
        )
    }
}

/**
 * Celebratory operation completion burst showing an elastic scale checkmark
 * with an expanding neon pulse ring.
 */
@Composable
fun OperationCompletionBurst(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 64.dp,
    color: Color = Color(0xFF10B981)
) {
    val motion = LocalMotionSensitivity.current
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = MotionTokens.SpringCheckboxPop,
        label = "SuccessBurstScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                if (!motion.isReducedMotion) {
                    scaleX = scale
                    scaleY = scale
                }
            }
            .clip(CircleShape)
            .background(color.copy(alpha = 0.20f))
            .border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Success",
            tint = color,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
