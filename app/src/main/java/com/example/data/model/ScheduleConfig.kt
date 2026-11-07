package com.example.data.model

import java.util.Calendar

enum class ScheduleMode {
    INTERVAL,
    DAY_OF_WEEK
}

enum class IntervalUnit(val label: String, val daysMultiplier: Long) {
    DAYS("Days", 1L),
    WEEKS("Weeks", 7L),
    MONTHS("Months", 30L)
}

data class ScheduleConfig(
    val isEnabled: Boolean = false,
    val mode: ScheduleMode = ScheduleMode.INTERVAL,
    val intervalValue: Int = 1,
    val intervalUnit: IntervalUnit = IntervalUnit.DAYS,
    val dayOfWeek: Int = Calendar.SUNDAY, // Calendar.SUNDAY..SATURDAY
    val targetHour: Int = 3, // 03:00 AM default (ideal for overnight maintenance)
    val targetMinute: Int = 0,
    val requireBatteryNotLow: Boolean = true,
    val requireCharging: Boolean = false,
    val notifyOnCleanup: Boolean = true
) {
    /**
     * Calculates total interval in days for INTERVAL mode
     */
    fun toIntervalDays(): Long {
        return (intervalValue.coerceAtLeast(1) * intervalUnit.daysMultiplier).coerceAtLeast(1L)
    }
}
