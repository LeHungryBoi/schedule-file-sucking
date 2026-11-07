package com.example.util

import com.example.data.model.IntervalUnit
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FormatUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0) return "Never"
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTimeOnly(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun getDayOfWeekName(calendarDay: Int, short: Boolean = false): String {
        return when (calendarDay) {
            Calendar.SUNDAY -> if (short) "Sun" else "Sunday"
            Calendar.MONDAY -> if (short) "Mon" else "Monday"
            Calendar.TUESDAY -> if (short) "Tue" else "Tuesday"
            Calendar.WEDNESDAY -> if (short) "Wed" else "Wednesday"
            Calendar.THURSDAY -> if (short) "Thu" else "Thursday"
            Calendar.FRIDAY -> if (short) "Fri" else "Friday"
            Calendar.SATURDAY -> if (short) "Sat" else "Saturday"
            else -> "Sunday"
        }
    }

    fun formatScheduleSummary(config: ScheduleConfig): String {
        if (!config.isEnabled) return "Automation is paused"
        return when (config.mode) {
            ScheduleMode.INTERVAL -> {
                val value = config.intervalValue
                val unitLabel = when (config.intervalUnit) {
                    IntervalUnit.DAYS -> if (value == 1) "day" else "days"
                    IntervalUnit.WEEKS -> if (value == 1) "week" else "weeks"
                    IntervalUnit.MONTHS -> if (value == 1) "month" else "months"
                }
                "Runs every $value $unitLabel"
            }
            ScheduleMode.DAY_OF_WEEK -> {
                val day = getDayOfWeekName(config.dayOfWeek)
                val time = formatTimeOnly(config.targetHour, config.targetMinute)
                "Runs every $day at $time"
            }
        }
    }

    /**
     * Calculates initial delay in milliseconds for DAY_OF_WEEK schedule mode
     */
    fun calculateInitialDelayForDayOfWeek(
        targetDayOfWeek: Int,
        targetHour: Int,
        targetMinute: Int,
        now: Calendar = Calendar.getInstance()
    ): Long {
        val target = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, targetDayOfWeek)
        }

        // If target time already passed this week, advance to next week
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.WEEK_OF_YEAR, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }

    /**
     * Returns an estimated next execution date formatted string
     */
    fun formatNextScheduledExecution(config: ScheduleConfig, lastRunTimestamp: Long): String {
        if (!config.isEnabled) return "Not scheduled"

        val now = Calendar.getInstance()
        val nextCal = Calendar.getInstance()

        when (config.mode) {
            ScheduleMode.INTERVAL -> {
                val intervalMillis = config.toIntervalDays() * 24L * 60L * 60L * 1000L
                val base = if (lastRunTimestamp > 0) lastRunTimestamp else now.timeInMillis
                var next = base + intervalMillis
                while (next <= now.timeInMillis) {
                    next += intervalMillis
                }
                nextCal.timeInMillis = next
            }
            ScheduleMode.DAY_OF_WEEK -> {
                val delay = calculateInitialDelayForDayOfWeek(
                    config.dayOfWeek,
                    config.targetHour,
                    config.targetMinute,
                    now
                )
                nextCal.timeInMillis = now.timeInMillis + delay
            }
        }

        val sdf = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
        return sdf.format(nextCal.time)
    }
}
