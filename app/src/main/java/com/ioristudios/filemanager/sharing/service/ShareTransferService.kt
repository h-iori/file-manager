package com.ioristudios.filemanager.sharing.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ioristudios.filemanager.MainActivity
import com.ioristudios.filemanager.R
import com.ioristudios.filemanager.sharing.resilience.TransferLifecycleStatus
import com.ioristudios.filemanager.sharing.resilience.TransferProgressSnapshot

/**
 * Foreground Service that maintains persistent P2P transfer execution in the background
 * and provides live notification controls (Pause, Resume, Cancel).
 */
class ShareTransferService : Service() {

    companion object {
        const val CHANNEL_ID = "files_share_channel"
        const val NOTIFICATION_ID = 2002

        const val ACTION_START = "com.ioristudios.filemanager.share.START"
        const val ACTION_UPDATE_PROGRESS = "com.ioristudios.filemanager.share.UPDATE_PROGRESS"
        const val ACTION_STOP = "com.ioristudios.filemanager.share.STOP"

        const val ACTION_PAUSE = "com.ioristudios.filemanager.share.PAUSE"
        const val ACTION_RESUME = "com.ioristudios.filemanager.share.RESUME"
        const val ACTION_CANCEL = "com.ioristudios.filemanager.share.CANCEL"

        var onActionReceived: ((String) -> Unit)? = null

        fun startService(context: Context) {
            val intent = Intent(context, ShareTransferService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ShareTransferService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                val notification = buildNotification("Initializing transfer...", 0, "Preparing connection...")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    } else {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    }
                    startForeground(NOTIFICATION_ID, notification, fgsType)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            ACTION_PAUSE -> {
                onActionReceived?.invoke(ACTION_PAUSE)
            }
            ACTION_RESUME -> {
                onActionReceived?.invoke(ACTION_RESUME)
            }
            ACTION_CANCEL -> {
                onActionReceived?.invoke(ACTION_CANCEL)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    fun updateProgress(snapshot: TransferProgressSnapshot) {
        val percentInt = (snapshot.percent * 100).toInt().coerceIn(0, 100)
        val title = when (snapshot.status) {
            TransferLifecycleStatus.TRANSFERRING -> "Transferring: ${snapshot.currentFileName}"
            TransferLifecycleStatus.PAUSED -> "Paused: ${snapshot.currentFileName}"
            TransferLifecycleStatus.COMPLETED -> "Transfer Complete"
            TransferLifecycleStatus.FAILED -> "Transfer Failed"
            else -> "Quick Share Active"
        }
        val subtext = "${snapshot.speedFormatted} • ${snapshot.etaFormatted} (${snapshot.fileIndex}/${snapshot.totalFiles})"
        val notification = buildNotification(title, percentInt, subtext, isPaused = snapshot.status == TransferLifecycleStatus.PAUSED)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, progress: Int, content: String, isPaused: Boolean = false): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingLaunch = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, ShareTransferService::class.java).apply { action = ACTION_CANCEL }
        val pendingCancel = PendingIntent.getService(
            this, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, ShareTransferService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pendingPauseResume = PendingIntent.getService(
            this, 2, pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingLaunch)
            .setOngoing(!isPaused && progress < 100)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, progress == 0)
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) "Resume" else "Pause",
                pendingPauseResume
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", pendingCancel)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "File Sharing Transfers",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress and controls for active P2P file transfers"
                enableVibration(false)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

