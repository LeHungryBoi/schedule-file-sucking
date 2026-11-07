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
import com.example.data.preferences.PreferencesManager
import com.example.data.storage.ApkScanner
import com.example.data.storage.ScreenshotScanner
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

        if (!config.isEnabled || (!config.cleanApksEnabled && !config.cleanScreenshotsEnabled)) {
            // Automation disabled or neither cleaner active
            return Result.success()
        }

        var apksDeleted = 0
        var apkBytes = 0L

        // 1. Clean APKs if enabled
        if (config.cleanApksEnabled) {
            val customFolderUri = prefs.customFolderUriFlow.first()
            val apkScanner = ApkScanner(appContext)
            val apkScanResult = apkScanner.scanApkFiles(customFolderUri)
            if (apkScanResult.files.isNotEmpty()) {
                val deleteResult = apkScanner.deleteApkFiles(apkScanResult.files)
                apksDeleted = deleteResult.deletedCount
                apkBytes = deleteResult.bytesReclaimed
            }
        }

        // 2. Clean expired screenshots if enabled
        var screenshotsDeleted = 0
        var screenshotBytes = 0L

        if (config.cleanScreenshotsEnabled) {
            val screenshotScanner = ScreenshotScanner(appContext)
            val screenshotScan = screenshotScanner.scanScreenshots(config.screenshotMaxAgeDays)
            val expiredList = screenshotScan.files.filter { it.isExpired }
            if (expiredList.isNotEmpty()) {
                val deleteResult = screenshotScanner.deleteScreenshots(expiredList)
                screenshotsDeleted = deleteResult.deletedCount
                screenshotBytes = deleteResult.bytesReclaimed
            }
        }

        val totalDeleted = apksDeleted + screenshotsDeleted
        val totalBytes = apkBytes + screenshotBytes

        // Record history
        prefs.recordCleanupRun(
            apksDeleted = apksDeleted,
            screenshotsDeleted = screenshotsDeleted,
            bytesReclaimed = totalBytes,
            isAutomated = true
        )

        if (config.notifyOnCleanup && totalDeleted > 0) {
            sendCleanupNotification(apksDeleted, screenshotsDeleted, totalBytes)
        }

        return Result.success()
    }

    private fun sendCleanupNotification(apksCount: Int, screenshotsCount: Int, bytes: Long) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cleanup Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when background cleaner maintenance finishes"
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
        val details = buildList {
            if (apksCount > 0) add("$apksCount APK(s)")
            if (screenshotsCount > 0) add("$screenshotsCount screenshot(s)")
        }.joinToString(" and ")

        val message = "Reclaimed $formattedSize • Removed $details"

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Automated Maintenance Completed")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Sweeper auto-cleaned your storage:\n$message"))
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
