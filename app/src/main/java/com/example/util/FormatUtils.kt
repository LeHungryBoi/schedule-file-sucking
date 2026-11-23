package com.example.util

import android.content.Context
import com.example.R
import com.example.data.model.IntervalUnit
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object FormatUtils {

    fun formatBytes(context: Context, bytes: Long): String {
        val format = context.getString(R.string.format_bytes)
        if (bytes <= 0) {
            return String.format(format, "0", context.getString(R.string.unit_bytes))
        }
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format(
                format,
                String.format(Locale.getDefault(), "%.2f", gb),
                context.getString(R.string.unit_gb)
            )
            mb >= 1.0 -> String.format(
                format,
                String.format(Locale.getDefault(), "%.1f", mb),
                context.getString(R.string.unit_mb)
            )
            kb >= 1.0 -> String.format(
                format,
                String.format(Locale.getDefault(), "%.1f", kb),
                context.getString(R.string.unit_kb)
            )
            else -> String.format(format, bytes.toString(), context.getString(R.string.unit_bytes))
        }
    }

    fun formatDate(context: Context, timestamp: Long): String {
        if (timestamp <= 0) return context.getString(R.string.never)
        val sdf = SimpleDateFormat(context.getString(R.string.date_pattern), Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTimeOnly(context: Context, hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val sdf = SimpleDateFormat(context.getString(R.string.time_only_pattern), Locale.getDefault())
        return sdf.format(cal.time)
    }

    fun getDayOfWeekName(context: Context, calendarDay: Int, short: Boolean = false): String {
        val names = when (calendarDay) {
            Calendar.MONDAY -> R.string.day_monday to R.string.day_short_monday
            Calendar.TUESDAY -> R.string.day_tuesday to R.string.day_short_tuesday
            Calendar.WEDNESDAY -> R.string.day_wednesday to R.string.day_short_wednesday
            Calendar.THURSDAY -> R.string.day_thursday to R.string.day_short_thursday
            Calendar.FRIDAY -> R.string.day_friday to R.string.day_short_friday
            Calendar.SATURDAY -> R.string.day_saturday to R.string.day_short_saturday
            else -> R.string.day_sunday to R.string.day_short_sunday
        }
        return context.getString(if (short) names.second else names.first)
    }

    fun formatScheduleSummary(context: Context, config: ScheduleConfig): String {
        if (!config.isEnabled) return context.getString(R.string.automation_paused_summary)
        val timeStr = formatTimeOnly(context, config.targetHour, config.targetMinute)
        val targets = when {
            config.cleanApksEnabled && config.cleanScreenshotsEnabled ->
                context.getString(R.string.schedule_target_both, config.screenshotMaxAgeDays)
            config.cleanApksEnabled -> context.getString(R.string.schedule_target_apks)
            config.cleanScreenshotsEnabled ->
                context.getString(R.string.schedule_target_screenshots, config.screenshotMaxAgeDays)
            else -> context.getString(R.string.schedule_target_none)
        }

        return when (config.mode) {
            ScheduleMode.INTERVAL -> {
                val value = config.intervalValue
                val unitRes = when (config.intervalUnit) {
                    IntervalUnit.DAYS -> if (value == 1) R.string.interval_unit_day else R.string.interval_unit_days
                    IntervalUnit.WEEKS -> if (value == 1) R.string.interval_unit_week else R.string.interval_unit_weeks
                    IntervalUnit.MONTHS -> if (value == 1) R.string.interval_unit_month else R.string.interval_unit_months
                }
                context.getString(
                    R.string.schedule_summary_interval,
                    value,
                    context.getString(unitRes),
                    timeStr,
                    targets
                )
            }
            ScheduleMode.DAY_OF_WEEK -> context.getString(
                R.string.schedule_summary_weekly,
                getDayOfWeekName(context, config.dayOfWeek),
                timeStr,
                targets
            )
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
    fun formatNextScheduledExecution(context: Context, config: ScheduleConfig, lastRunTimestamp: Long): String {
        if (!config.isEnabled) return context.getString(R.string.not_scheduled)

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

        val sdf = SimpleDateFormat(context.getString(R.string.next_execution_pattern), Locale.getDefault())
        return sdf.format(nextCal.time)
    }

    fun formatAgeString(context: Context, ageDays: Double): String {
        return when {
            ageDays >= 365.0 -> context.getString(
                R.string.age_years,
                String.format(Locale.getDefault(), "%.1f", ageDays / 365.0)
            )
            ageDays >= 30.0 -> context.getString(
                R.string.age_months,
                String.format(Locale.getDefault(), "%.0f", ageDays / 30.0)
            )
            ageDays >= 1.0 -> context.getString(
                R.string.age_days,
                String.format(Locale.getDefault(), "%.1f", ageDays)
            )
            else -> {
                val hours = (ageDays * 24.0).toInt()
                if (hours >= 1) context.getString(R.string.age_hours, hours)
                else {
                    val mins = (ageDays * 24.0 * 60.0).toInt()
                    context.getString(R.string.age_minutes, mins.coerceAtLeast(1))
                }
            }
        }
    }

    fun formatCountdown(context: Context, remainingMillis: Long): String {
        if (remainingMillis <= 0L) return context.getString(R.string.countdown_expired)
        val totalSec = remainingMillis / 1000
        val hours = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val days = hours / 24

        return when {
            days >= 1 -> context.getString(R.string.countdown_days_hours, days, hours % 24)
            hours >= 1 -> context.getString(R.string.countdown_hours_minutes, hours, mins)
            else -> context.getString(R.string.countdown_minutes, mins.coerceAtLeast(1))
        }
    }
}
