package com.ioristudios.filemanager.ui.cleanup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ioristudios.filemanager.data.CleanupEngine
import com.ioristudios.filemanager.data.CleanupItem
import com.ioristudios.filemanager.data.CleanupScanResult
import com.ioristudios.filemanager.haptics.HapticType
import com.ioristudios.filemanager.haptics.LocalAppHaptics
import com.ioristudios.filemanager.theme.FilesTheme
import com.ioristudios.filemanager.theme.OutfitFontFamily
import kotlinx.coroutines.launch

@Composable
fun CleanupScreen(onBack: () -> Unit, onComplete: (String) -> Unit) {
    val context = LocalContext.current
    val engine = remember { CleanupEngine(context.applicationContext) }
    val progress by engine.progress.collectAsState()
    val scope = rememberCoroutineScope()
    val haptics = LocalAppHaptics.current
    var result by remember { mutableStateOf<CleanupScanResult?>(null) }
    var selectedPaths by remember { mutableStateOf(emptySet<String>()) }
    var confirmCleanup by remember { mutableStateOf(false) }
    var cleaning by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)
    LaunchedEffect(Unit) {
        result = engine.scan()
        selectedPaths = result?.categories.orEmpty().flatMap { it.items }
            .filter(CleanupItem::selectedByDefault)
            .map(CleanupItem::path)
            .toSet()
    }

    Box(Modifier.fillMaxSize().background(FilesTheme.colors.bg).statusBarsPadding()) {
        if (result == null || cleaning) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = FilesTheme.colors.primaryAccent)
                Spacer(Modifier.height(18.dp))
                Text(if (cleaning) "Cleaning selected items…" else progress.phase, color = FilesTheme.colors.textPrimary, fontFamily = OutfitFontFamily)
            }
        } else {
            val selectedBytes = result!!.categories.flatMap { it.items }.filter { it.path in selectedPaths }.sumOf { it.bytes }
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = FilesTheme.colors.textPrimary, modifier = Modifier.size(25.dp).clickable(onClick = onBack))
                    Column(Modifier.weight(1f)) {
                        Text("Storage cleanup", color = FilesTheme.colors.textPrimary, fontFamily = OutfitFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Review each item before deleting", color = FilesTheme.colors.textSecondary, fontFamily = OutfitFontFamily, fontSize = 12.sp)
                    }
                }
                LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(result!!.categories, key = { it.title }) { category ->
                        CategoryCard(category.title, category.items, selectedPaths) { path ->
                            selectedPaths = if (path in selectedPaths) selectedPaths - path else selectedPaths + path
                        }
                    }
                    item { Spacer(Modifier.height(92.dp)) }
                }
                Box(
                    Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (selectedPaths.isEmpty()) Color(0xFF4B5563) else Color(0xFF7C3AED))
                        .clickable(enabled = selectedPaths.isNotEmpty()) { haptics.trigger(HapticType.HERO_CLEAN_PULSE); confirmCleanup = true }
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Clean ${formatBytes(selectedBytes)}", color = Color.White, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (confirmCleanup) {
        AlertDialog(
            onDismissRequest = { confirmCleanup = false },
            title = { Text("Delete selected items?") },
            text = { Text("${selectedPaths.size} selected items will be permanently deleted. You can review the list again by cancelling.") },
            confirmButton = {
                Text("Clean", modifier = Modifier.padding(12.dp).clickable {
                    confirmCleanup = false
                    cleaning = true
                    scope.launch {
                        val released = engine.clean(selectedPaths)
                        cleaning = false
                        onComplete("Freed ${formatBytes(released)}")
                        onBack()
                    }
                })
            },
            dismissButton = { Text("Cancel", modifier = Modifier.padding(12.dp).clickable { confirmCleanup = false }) }
        )
    }
}

@Composable
private fun CategoryCard(title: String, items: List<CleanupItem>, selectedPaths: Set<String>, onToggle: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(FilesTheme.colors.surface).border(1.dp, FilesTheme.colors.cardBorder, RoundedCornerShape(16.dp)).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = FilesTheme.colors.textPrimary, fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold)
            Text(formatBytes(items.sumOf { it.bytes }), color = FilesTheme.colors.primaryAccent, fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold)
        }
        items.forEach { item ->
            Row(Modifier.fillMaxWidth().clickable { onToggle(item.path) }.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(18.dp).clip(RoundedCornerShape(5.dp)).background(if (item.path in selectedPaths) FilesTheme.colors.primaryAccent else Color.Transparent).border(1.dp, FilesTheme.colors.cardBorder, RoundedCornerShape(5.dp)))
                Column(Modifier.weight(1f)) {
                    Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis, color = FilesTheme.colors.textPrimary, fontFamily = OutfitFontFamily, fontSize = 13.sp)
                    Text(item.reason, maxLines = 1, overflow = TextOverflow.Ellipsis, color = FilesTheme.colors.textSecondary, fontFamily = OutfitFontFamily, fontSize = 11.sp)
                }
                Text(formatBytes(item.bytes), color = FilesTheme.colors.textMuted, fontFamily = OutfitFontFamily, fontSize = 11.sp)
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB")
    var value = bytes.toDouble() / 1024
    var index = 0
    while (value >= 1024 && index < units.lastIndex) { value /= 1024; index++ }
    return "%.1f %s".format(value, units[index])
}
