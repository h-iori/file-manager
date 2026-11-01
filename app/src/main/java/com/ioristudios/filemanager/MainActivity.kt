package com.ioristudios.filemanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ioristudios.filemanager.data.DefaultDataRepository
import com.ioristudios.filemanager.haptics.HapticFeedbackManager
import com.ioristudios.filemanager.haptics.LocalAppHaptics
import com.ioristudios.filemanager.theme.FilesTheme
import com.ioristudios.filemanager.ui.main.MainScreen
import com.ioristudios.filemanager.ui.main.MainScreenViewModel
import com.ioristudios.filemanager.ui.main.NavTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var repository: DefaultDataRepository

    private val viewModel: MainScreenViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainScreenViewModel(repository) as T
            }
        }
    }

    private val legacyPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refresh(hasPermission = checkHasPermission())
    }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refresh(hasPermission = checkHasPermission())
    }

    private val hapticManager by lazy { HapticFeedbackManager(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = DefaultDataRepository(applicationContext)

        enableEdgeToEdge()

        // Highest All Files Permission requested immediately at first boot
        requestHighestStoragePermission()

        handleIncomingIntent(intent)

        setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                com.ioristudios.filemanager.haptics.LocalAppHaptics provides hapticManager
            ) {
                val settingsState by viewModel.settingsUiState.collectAsStateWithLifecycle()
                FilesTheme(themeMode = settingsState.themeMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh(hasPermission = checkHasPermission())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_SEND) return

        val uri: Uri? = intent.data
            ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            ?: intent.getParcelableExtra(Intent.EXTRA_STREAM)

        if (uri == null) return

        lifecycleScope.launch(Dispatchers.IO) {
            val archiveFile = resolveOrCacheArchive(uri)
            if (archiveFile != null && archiveFile.exists()) {
                withContext(Dispatchers.Main) {
                    viewModel.selectTab(NavTab.FILES)
                    viewModel.openDirectory(archiveFile.absolutePath)
                }
            }
        }
    }

    private fun resolveOrCacheArchive(uri: Uri): File? {
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            val f = File(path)
            if (f.exists()) return f
        }

        if (uri.scheme == "content") {
            if (uri.authority == "com.android.externalstorage.documents") {
                try {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":")
                    if (split.size == 2) {
                        val type = split[0]
                        val relPath = split[1]
                        val baseDir = if ("primary".equals(type, ignoreCase = true)) {
                            Environment.getExternalStorageDirectory()
                        } else {
                            File("/storage/$type")
                        }
                        val f = File(baseDir, relPath)
                        if (f.exists()) return f
                    }
                } catch (_: Exception) {}
            }

            var displayName: String? = null
            try {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && cursor.moveToFirst()) {
                        displayName = cursor.getString(nameIdx)
                    }
                }
            } catch (_: Exception) {}

            if (displayName.isNullOrBlank()) {
                displayName = uri.lastPathSegment?.let { File(it).name }
            }
            if (displayName.isNullOrBlank() || !displayName!!.contains('.')) {
                displayName = "received_archive.zip"
            }

            val safeName = File(displayName!!).name
            val destDir = File(cacheDir, "opened_archives")
            destDir.mkdirs()
            val destFile = File(destDir, safeName)

            try {
                contentResolver.openInputStream(uri)?.use { inStream ->
                    destFile.outputStream().use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                if (destFile.exists() && destFile.length() > 0) {
                    return destFile
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to cache incoming archive: ${e.message}")
            }
        }

        return null
    }

    private fun checkHasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestHighestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    manageStorageLauncher.launch(intent)
                } catch (_: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        manageStorageLauncher.launch(intent)
                    } catch (_: Exception) {}
                }
            }
        } else {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val needed = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                legacyPermissionLauncher.launch(needed.toTypedArray())
            }
        }
    }
}
