package com.ioristudios.filemanager.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ioristudios.filemanager.data.StorageOverview
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
    val progressAnimation by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "donutAnimation"
    )

    val slices = remember(storage) {
        listOf(
            DonutCategorySlice("Photos", storage.photosBytes, ChartPhotosCyan),
            DonutCategorySlice("Videos", storage.videosBytes, ChartVideosPurple),
            DonutCategorySlice("Audio", storage.audioBytes, ChartAudioLime),
            DonutCategorySlice("Docs", storage.documentsBytes, ChartDocsPink),
            DonutCategorySlice("Downloads", storage.downloadsBytes, ChartDownloadsAmber),
            DonutCategorySlice("Apps", storage.appsBytes, ChartAppsOrange),
            DonutCategorySlice("System", storage.systemBytes, ChartSystemSlate),
            DonutCategorySlice("Available", storage.freeBytes, ChartAvailableEmerald)
        )
    }

    val angles = remember(slices) {
        calculateDonutAngles(slices, totalDegrees = 360f, minDegrees = 5.0f)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val canvasSize = this.size.minDimension
            val radius = (canvasSize - strokePx) / 2f
            val centerOffset = androidx.compose.ui.geometry.Offset(this.size.width / 2f, this.size.height / 2f)

            // Deep background circular track
            drawCircle(
                color = Color(0xFF1B1033),
                radius = radius,
                center = centerOffset,
                style = Stroke(width = strokePx)
            )

            // Category Slices dynamically filling the complete 360° circle
            var currentAngle = -90f
            val gapDeg = 1.8f

            slices.forEachIndexed { index, slice ->
                val deg = angles.getOrElse(index) { 0f }
                if (deg > 0.5f) {
                    val actualSweep = maxOf(1f, deg - gapDeg) * progressAnimation
                    val startAngle = currentAngle + (gapDeg / 2f)

                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = actualSweep,
                        useCenter = false,
                        style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                    )
                    currentAngle += deg
                }
            }
        }

        // Center Hero Metric with Outfit typography
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "USED",
                color = ElectricLavender,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OutfitFontFamily,
                letterSpacing = 1.8.sp
            )
            Text(
                text = storage.formattedUsed,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = OutfitFontFamily,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "${storage.usedPercent.toInt()}%",
                color = ChartPhotosCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = OutfitFontFamily
            )
        }
    }
}

/**
 * Reusable frosted glass circular icon button for headers.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x1EFFFFFF), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFFE2E8F0),
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
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .width(175.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF160F2C))
            .border(
                1.2.dp,
                Brush.verticalGradient(
                    listOf(
                        CardBorderPurple.copy(alpha = 0.9f),
                        PurpleContainer.copy(alpha = 0.4f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(vertical = 4.dp)
    ) {
        // Open
        GlassContextMenuItem(
            icon = Icons.Default.OpenInNew,
            iconTint = ElectricCyan,
            iconBg = Color(0xFF0284C7).copy(alpha = 0.22f),
            label = "Open",
            onClick = {
                onDismissRequest()
                onOpen()
            }
        )

        // Details
        GlassContextMenuItem(
            icon = Icons.Default.Info,
            iconTint = ElectricPurple,
            iconBg = Color(0xFF9333EA).copy(alpha = 0.22f),
            label = "Details",
            onClick = {
                onDismissRequest()
                onDetails()
            }
        )

        // Share
        GlassContextMenuItem(
            icon = Icons.Default.Share,
            iconTint = ElectricTeal,
            iconBg = Color(0xFF0F766E).copy(alpha = 0.25f),
            label = "Share",
            onClick = {
                onDismissRequest()
                onShare()
            }
        )

        // Divider
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            thickness = 0.8.dp,
            color = Color(0x22FFFFFF)
        )

        // Delete
        GlassContextMenuItem(
            icon = Icons.Default.Delete,
            iconTint = Color(0xFFFF5252),
            iconBg = Color(0xFFB71C1C).copy(alpha = 0.22f),
            label = "Delete",
            isDanger = true,
            onClick = {
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
                    color = if (isDanger) Color(0xFFFF5252) else TextPrimary,
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
                        imageVector = if (isDirectory) Icons.Default.Folder else Icons.Default.Info,
                        contentDescription = null,
                        tint = ElectricPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = if (isDirectory) "Directory Details" else "File Details",
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
fun ModalDetailRow(label: String, value: String) {
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
