package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.preferences.PreferencesManager
import com.example.data.storage.ApkScanner
import com.example.util.FormatUtils
import kotlinx.coroutines.flow.first

class ApkCleanupWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "apk_sweeper_periodic_work"
        const val CHANNEL_ID = "apk_cleaner_notifications"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        val prefs = PreferencesManager(appContext)
        val config = prefs.scheduleConfigFlow.first()

        if (!config.isEnabled) {
            // Automation was disabled, skip execution
            return Result.success()
        }

        val customFolderUri = prefs.customFolderUriFlow.first()
        val scanner = ApkScanner(appContext)

        val scanResult = scanner.scanApkFiles(customFolderUri)
        if (scanResult.files.isEmpty()) {
            // Record successful run with 0 deleted
            prefs.recordCleanupRun(
                filesDeleted = 0,
                bytesReclaimed = 0L,
                isAutomated = true
            )
            return Result.success()
        }

        val deleteResult = scanner.deleteApkFiles(scanResult.files)

        prefs.recordCleanupRun(
            filesDeleted = deleteResult.deletedCount,
            bytesReclaimed = deleteResult.bytesReclaimed,
            isAutomated = true
        )

        if (config.notifyOnCleanup && deleteResult.deletedCount > 0) {
            sendCleanupNotification(deleteResult.deletedCount, deleteResult.bytesReclaimed)
        }

        return Result.success()
    }

    private fun sendCleanupNotification(count: Int, bytes: Long) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "APK Cleanup Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when background APK sweep finishes"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedSize = FormatUtils.formatBytes(bytes)
        val fileWord = if (count == 1) "file" else "files"
        val message = "Removed $count .apk $fileWord • Reclaimed $formattedSize"

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Downloads Folder Cleaned")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("APK Sweeper automatically cleaned your Downloads folder:\n$message"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Missing POST_NOTIFICATIONS permission
        }
    }
}
