package com.ioristudios.filemanager.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.filemanager.data.DirectoryItem
import com.ioristudios.filemanager.data.DirectoryProtectionInfo
import com.ioristudios.filemanager.data.ProtectionLevel
import com.ioristudios.filemanager.theme.ElectricAmber
import com.ioristudios.filemanager.theme.ElectricCoral
import com.ioristudios.filemanager.theme.ElectricPurple
import com.ioristudios.filemanager.theme.FilesTheme
import com.ioristudios.filemanager.theme.OutfitFontFamily

@Composable
fun ProtectedDirectoryBanner(
    info: DirectoryProtectionInfo,
    onLearnMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSystemCore = info.level == ProtectionLevel.SYSTEM_CORE_LOCKED
    val tint = if (isSystemCore) ElectricCoral else ElectricAmber

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        tint.copy(alpha = 0.16f),
                        tint.copy(alpha = 0.05f)
                    )
                )
            )
            .border(1.dp, tint.copy(alpha = 0.40f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.20f))
                    .border(1.dp, tint.copy(alpha = 0.50f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSystemCore) Icons.Default.Security else Icons.Default.Lock,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.title,
                    fontFamily = OutfitFontFamily,
                    color = tint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = info.description,
                    color = FilesTheme.colors.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.20f))
                    .clickable { onLearnMore() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Info",
                    color = tint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ProtectedDirectoryInfoDialog(
    info: DirectoryProtectionInfo,
    onDismiss: () -> Unit
) {
    val isSystemCore = info.level == ProtectionLevel.SYSTEM_CORE_LOCKED
    val tint = if (isSystemCore) ElectricCoral else ElectricAmber

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FilesTheme.colors.dialogBg,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.20f))
                        .border(1.dp, tint.copy(alpha = 0.50f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSystemCore) Icons.Default.Security else Icons.Default.Shield,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = info.title,
                        fontFamily = OutfitFontFamily,
                        color = FilesTheme.colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isSystemCore) "Android System Partition" else "Google Scoped Storage Safeguard",
                        color = tint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = info.description,
                    color = FilesTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(FilesTheme.colors.surface)
                        .border(1.dp, FilesTheme.colors.cardBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Security Policies Enforced:",
                            color = FilesTheme.colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "• Modification and deletion are locked to prevent app bricking.\n• Sandboxed private app state is shielded from unauthorized access.\n• Safe browsing is preserved where permitted by OS permissions.",
                            color = FilesTheme.colors.textSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = tint)
            ) {
                Text(text = "Understood", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun ProtectedOperationDisclaimerDialog(
    actionName: String,
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FilesTheme.colors.dialogBg,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ElectricCoral.copy(alpha = 0.20f))
                        .border(1.dp, ElectricCoral.copy(alpha = 0.50f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = ElectricCoral,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Sensitive Directory Warning",
                        fontFamily = OutfitFontFamily,
                        color = FilesTheme.colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Confirmation Required",
                        color = ElectricCoral,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "You are attempting to $actionName \"$itemName\".",
                    color = FilesTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "This item is part of a protected system partition or an application container (Scoped Storage). Modifying or deleting its files may cause installed applications to lose data, reset logins, or fail to launch.",
                    color = FilesTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElectricCoral.copy(alpha = 0.10f))
                        .border(1.dp, ElectricCoral.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "⚠️ Are you sure you want to proceed with this operation?",
                        color = ElectricCoral,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral)
            ) {
                Text(text = "Proceed Anyway", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = FilesTheme.colors.textSecondary)
            }
        }
    )
}

@Composable
fun ProtectedActionBlockedDialog(
    itemName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FilesTheme.colors.dialogBg,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ElectricCoral.copy(alpha = 0.20f))
                        .border(1.dp, ElectricCoral.copy(alpha = 0.50f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = ElectricCoral,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Action Blocked",
                    fontFamily = OutfitFontFamily,
                    color = FilesTheme.colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Text(
                text = "Modifying, moving, or deleting \"$itemName\" is blocked. This directory is protected under Android system security and Scoped Storage policies to maintain app data integrity.",
                color = FilesTheme.colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCoral)
            ) {
                Text(text = "Got it", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun ScopedStorageRestrictedAccessDialog(
    item: DirectoryItem,
    context: Context,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FilesTheme.colors.dialogBg,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ElectricAmber.copy(alpha = 0.20f))
                        .border(1.dp, ElectricAmber.copy(alpha = 0.50f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = ElectricAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Restricted Access",
                    fontFamily = OutfitFontFamily,
                    color = FilesTheme.colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Starting with Android 11, Google restricts direct third-party access to \"${item.name}\" to protect user privacy and app accounts.",
                    color = FilesTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Text(
                    text = "You can view these files by connecting your phone to a PC via USB (MTP / ADB) or using the pre-installed system Files app.",
                    color = FilesTheme.colors.textMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(Uri.parse("content://com.android.externalstorage.documents/root/primary"), "vnd.android.document/root")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "System Files", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = FilesTheme.colors.textSecondary)
            }
        }
    )
}
