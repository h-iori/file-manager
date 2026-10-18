package com.ioristudios.filemanager.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ioristudios.filemanager.MainActivity
import com.ioristudios.filemanager.R
import com.ioristudios.filemanager.data.FileOpProgress
import com.ioristudios.filemanager.data.FileOpStatus

object FileOperationNotificationHelper {

    const val CHANNEL_ID = "file_operations_channel"
    private const val CHANNEL_NAME = "File Operations"
    const val NOTIFICATION_ID = 9001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress for copying, moving, compressing and extracting files in the background"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun buildNotification(
        context: Context,
        progress: FileOpProgress
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Title: e.g. "Copying 4 items (45%)" or "Paused · Extracting..."
        val percent = (progress.progressFraction * 100f).toInt().coerceIn(0, 100)
        val titleText = when (progress.status) {
            FileOpStatus.PAUSED -> "Paused · ${progress.title}"
            FileOpStatus.COMPLETED -> "Finished · ${progress.title}"
            FileOpStatus.CANCELLED -> "Cancelled · ${progress.title}"
            FileOpStatus.FAILED -> "Failed · ${progress.title}"
            else -> if (progress.isIndeterminate) progress.title else "${progress.title} ($percent%)"
        }

        // Subtitle: e.g. "video.mp4 · 24.5 MB/s (120 MB / 450 MB)"
        val contentText = buildString {
            if (progress.currentFileName.isNotEmpty()) {
                append(progress.currentFileName)
                append(" · ")
            }
            if (progress.status == FileOpStatus.RUNNING) {
                append(progress.formattedSpeed)
                append(" (")
                append(progress.formattedProcessedBytes)
                append(" / ")
                append(progress.formattedTotalBytes)
                append(")")
                if (progress.formattedEta.isNotEmpty()) {
                    append(" · ")
                    append(progress.formattedEta)
                }
            } else if (progress.status == FileOpStatus.PAUSED) {
                append("Paused at ")
                append(progress.formattedProcessedBytes)
            } else if (progress.errorMessage != null) {
                append(progress.errorMessage)
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(progress.isActive)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Progress bar
        if (progress.isActive) {
            if (progress.isIndeterminate) {
                builder.setProgress(0, 0, true)
            } else {
                builder.setProgress(100, percent, false)
            }

            // Pause / Resume Action
            if (progress.status == FileOpStatus.PAUSED) {
                val resumeIntent = Intent(context, FileOperationService::class.java).apply {
                    action = FileOperationService.ACTION_RESUME
                }
                val resumePending = PendingIntent.getService(
                    context, 1, resumeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePending)
            } else {
                val pauseIntent = Intent(context, FileOperationService::class.java).apply {
                    action = FileOperationService.ACTION_PAUSE
                }
                val pausePending = PendingIntent.getService(
                    context, 2, pauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePending)
            }

            // Cancel Action
            val cancelIntent = Intent(context, FileOperationService::class.java).apply {
                action = FileOperationService.ACTION_CANCEL
            }
            val cancelPending = PendingIntent.getService(
                context, 3, cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPending)
        }

        return builder.build()
    }
}
