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
        if (!config.isEnabled) return "Automation paused"
        val timeStr = formatTimeOnly(config.targetHour, config.targetMinute)
        val targets = when {
            config.cleanApksEnabled && config.cleanScreenshotsEnabled -> "APKs & Screenshots (>${config.screenshotMaxAgeDays}d)"
            config.cleanApksEnabled -> "APKs only"
            config.cleanScreenshotsEnabled -> "Screenshots only (>${config.screenshotMaxAgeDays}d)"
            else -> "Nothing selected"
        }

        return when (config.mode) {
            ScheduleMode.INTERVAL -> {
                val value = config.intervalValue
                val unitLabel = when (config.intervalUnit) {
                    IntervalUnit.DAYS -> if (value == 1) "day" else "days"
                    IntervalUnit.WEEKS -> if (value == 1) "week" else "weeks"
                    IntervalUnit.MONTHS -> if (value == 1) "month" else "months"
                }
                "Every $value $unitLabel at $timeStr • $targets"
            }
            ScheduleMode.DAY_OF_WEEK -> {
                val day = getDayOfWeekName(config.dayOfWeek)
                "Every $day at $timeStr • $targets"
            }
        }
    }

    /**
     * Calculates initial delay in milliseconds to the next occurrence of targetHour:targetMinute
     */
    fun calculateInitialDelayForTime(
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
        }

        // If target time already passed today, advance to tomorrow
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis - now.timeInMillis
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
                val delay = calculateInitialDelayForTime(config.targetHour, config.targetMinute, now)
                nextCal.timeInMillis = now.timeInMillis + delay
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

    fun formatAgeString(ageDays: Double): String {
        return when {
            ageDays >= 365.0 -> String.format(Locale.getDefault(), "%.1fy ago", ageDays / 365.0)
            ageDays >= 30.0 -> String.format(Locale.getDefault(), "%.0fmo ago", ageDays / 30.0)
            ageDays >= 1.0 -> String.format(Locale.getDefault(), "%.1fd ago", ageDays)
            else -> {
                val hours = (ageDays * 24.0).toInt()
                if (hours >= 1) "${hours}h ago"
                else {
                    val mins = (ageDays * 24.0 * 60.0).toInt()
                    "${mins.coerceAtLeast(1)}m ago"
                }
            }
        }
    }

    fun formatCountdown(remainingMillis: Long): String {
        if (remainingMillis <= 0L) return "Expired"
        val totalSec = remainingMillis / 1000
        val hours = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val days = hours / 24

        return when {
            days >= 1 -> "${days}d ${hours % 24}h left"
            hours >= 1 -> "${hours}h ${mins}m left"
            else -> "${mins.coerceAtLeast(1)}m left"
        }
    }
}
