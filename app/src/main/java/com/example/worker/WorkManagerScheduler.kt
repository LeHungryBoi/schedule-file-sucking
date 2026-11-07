package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleMode
import com.example.util.FormatUtils
import java.util.concurrent.TimeUnit

object WorkManagerScheduler {

    fun scheduleOrCancel(context: Context, config: ScheduleConfig) {
        val workManager = WorkManager.getInstance(context)

        if (!config.isEnabled) {
            workManager.cancelUniqueWork(ApkCleanupWorker.WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(config.requireBatteryNotLow)
            .setRequiresCharging(config.requireCharging)
            .build()

        val periodicWorkRequest = when (config.mode) {
            ScheduleMode.INTERVAL -> {
                val intervalDays = config.toIntervalDays()
                PeriodicWorkRequestBuilder<ApkCleanupWorker>(intervalDays, TimeUnit.DAYS)
                    .setConstraints(constraints)
                    .build()
            }
            ScheduleMode.DAY_OF_WEEK -> {
                val initialDelayMillis = FormatUtils.calculateInitialDelayForDayOfWeek(
                    targetDayOfWeek = config.dayOfWeek,
                    targetHour = config.targetHour,
                    targetMinute = config.targetMinute
                )

                PeriodicWorkRequestBuilder<ApkCleanupWorker>(7L, TimeUnit.DAYS)
                    .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                    .setConstraints(constraints)
                    .build()
            }
        }

        workManager.enqueueUniquePeriodicWork(
            ApkCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ApkCleanupWorker.WORK_NAME)
    }
}
