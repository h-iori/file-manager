package com.ioristudios.filemanager.services

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.ioristudios.filemanager.data.FileOpProgress
import com.ioristudios.filemanager.data.FileOpStatus
import com.ioristudios.filemanager.data.FileOperationController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Android Foreground Service hosting long-running file operations (Copy, Move, Compress, Extract).
 * Prevents OS termination when the user switches to another app and delivers synchronized status bar controls.
 */
class FileOperationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var notificationManager: NotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        FileOperationNotificationHelper.createNotificationChannel(this)

        FileOperationController.setServiceNotifier { progress ->
            handleProgressUpdate(progress)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> {
                FileOperationController.pause()
            }
            ACTION_RESUME -> {
                FileOperationController.resume()
            }
            ACTION_CANCEL -> {
                FileOperationController.cancel()
            }
            ACTION_STOP -> {
                stopForegroundService()
            }
            ACTION_START, null -> {
                val current = FileOperationController.progressState.value
                val notification = FileOperationNotificationHelper.buildNotification(this, current)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            FileOperationNotificationHelper.NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else {
                        startForeground(FileOperationNotificationHelper.NOTIFICATION_ID, notification)
                    }
                } catch (_: Exception) {
                    // Fallback for older platforms or strict background startup constraints
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun handleProgressUpdate(progress: FileOpProgress) {
        if (!progress.isActive) {
            // If completed, cancelled, or failed: post final state then dismiss service cleanly
            val finalNotification = FileOperationNotificationHelper.buildNotification(this, progress)
            notificationManager?.notify(FileOperationNotificationHelper.NOTIFICATION_ID, finalNotification)

            serviceScope.launch {
                delay(1200L)
                stopForegroundService()
            }
            return
        }

        val notification = FileOperationNotificationHelper.buildNotification(this, progress)
        notificationManager?.notify(FileOperationNotificationHelper.NOTIFICATION_ID, notification)
    }

    private fun stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        notificationManager?.cancel(FileOperationNotificationHelper.NOTIFICATION_ID)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        FileOperationController.setServiceNotifier(null)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.ioristudios.filemanager.action.START"
        const val ACTION_PAUSE = "com.ioristudios.filemanager.action.PAUSE"
        const val ACTION_RESUME = "com.ioristudios.filemanager.action.RESUME"
        const val ACTION_CANCEL = "com.ioristudios.filemanager.action.CANCEL"
        const val ACTION_STOP = "com.ioristudios.filemanager.action.STOP"

        fun start(context: Context) {
            val intent = Intent(context, FileOperationService::class.java).apply {
                action = ACTION_START
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
                // In case OS restricts starting foreground service directly from background
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FileOperationService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }
}
