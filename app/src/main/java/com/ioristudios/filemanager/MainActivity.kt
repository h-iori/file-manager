package com.ioristudios.filemanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import com.ioristudios.filemanager.data.DefaultDataRepository
import com.ioristudios.filemanager.haptics.HapticFeedbackManager
import com.ioristudios.filemanager.haptics.LocalAppHaptics
import com.ioristudios.filemanager.theme.FilesTheme
import com.ioristudios.filemanager.ui.main.MainScreen
import com.ioristudios.filemanager.ui.main.MainScreenViewModel

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
