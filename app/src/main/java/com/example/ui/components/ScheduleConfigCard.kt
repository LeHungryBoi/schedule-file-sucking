package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IntervalUnit
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleMode
import com.example.util.FormatUtils
import java.util.Calendar

@Composable
fun ScheduleConfigCard(
    config: ScheduleConfig,
    onUpdateMode: (ScheduleMode) -> Unit,
    onUpdateInterval: (Int, IntervalUnit) -> Unit,
    onUpdateTargetTime: (Int, Int) -> Unit,
    onUpdateDayOfWeek: (Int) -> Unit,
    onUpdateCleanApks: (Boolean) -> Unit,
    onUpdateCleanScreenshots: (Boolean) -> Unit,
    onUpdateScreenshotRetentionDays: (Int) -> Unit,
    onUpdateConstraints: (Boolean, Boolean) -> Unit,
    onUpdateNotification: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .testTag("schedule_config_card"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Scheduled Cleaning Rules",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 1. Target Cleaners Selection (Independent switches with 1.dp borders)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ACTIVE AUTOMATION TARGETS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Target 1: APK Sweeper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Clean Downloaded APKs",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            )
                            Text(
                                text = "Deletes leftover .apk files from selected downloads folder",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.cleanApksEnabled,
                            onCheckedChange = onUpdateCleanApks,
                            modifier = Modifier.testTag("clean_apks_toggle"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    // Target 2: Screenshots Cleaner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Clean Expired Screenshots",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            )
                            Text(
                                text = "Purges screenshots older than ${config.screenshotMaxAgeDays} day(s)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.cleanScreenshotsEnabled,
                            onCheckedChange = onUpdateCleanScreenshots,
                            modifier = Modifier.testTag("clean_screenshots_toggle"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.secondary,
                                checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }

                    // Screenshot retention slider / chips if enabled
                    AnimatedVisibility(visible = config.cleanScreenshotsEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Screenshot Retention Window:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = if (config.screenshotMaxAgeDays == 1) "1 day (within 24h)" else "${config.screenshotMaxAgeDays} days",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(1, 2, 3, 7, 14, 30).forEach { days ->
                                    val isSelected = config.screenshotMaxAgeDays == days
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                                            .clickable { onUpdateScreenshotRetentionDays(days) }
                                            .padding(vertical = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${days}d",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Execution Time (Exact Hour & Minute) - User requested for interval to specify exact time!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "EXECUTION TIME OF DAY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = FormatUtils.formatTimeOnly(config.targetHour, config.targetMinute),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Stepper / Presets for exact time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val timePresets = listOf(
                            Pair(3, 0) to "03:00",
                            Pair(4, 0) to "04:00",
                            Pair(6, 0) to "06:00",
                            Pair(12, 0) to "12:00",
                            Pair(23, 0) to "23:00"
                        )

                        timePresets.forEach { (timePair, label) ->
                            val (h, m) = timePair
                            val isSelected = config.targetHour == h && config.targetMinute == m
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { onUpdateTargetTime(h, m) }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }

                    // Hour & Minute Stepper
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Custom Hour / Minute:",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Hour minus
                            IconButton(
                                onClick = { onUpdateTargetTime((config.targetHour - 1 + 24) % 24, config.targetMinute) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Remove, null, modifier = Modifier.size(12.dp))
                            }

                            Text(
                                text = String.format("%02dh", config.targetHour),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            // Hour plus
                            IconButton(
                                onClick = { onUpdateTargetTime((config.targetHour + 1) % 24, config.targetMinute) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp))
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Minute minus
                            IconButton(
                                onClick = { onUpdateTargetTime(config.targetHour, (config.targetMinute - 15 + 60) % 60) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Remove, null, modifier = Modifier.size(12.dp))
                            }

                            Text(
                                text = String.format("%02dm", config.targetMinute),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            // Minute plus
                            IconButton(
                                onClick = { onUpdateTargetTime(config.targetHour, (config.targetMinute + 15) % 60) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            // 3. Mode Selection (Interval vs Day of Week)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isInterval = config.mode == ScheduleMode.INTERVAL
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isInterval) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0f))
                        .clickable { onUpdateMode(ScheduleMode.INTERVAL) }
                        .padding(vertical = 6.dp)
                        .testTag("tab_interval_mode"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Interval (Every X Days)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isInterval) FontWeight.Bold else FontWeight.Normal,
                            color = if (isInterval) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                val isDayOfWeek = config.mode == ScheduleMode.DAY_OF_WEEK
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDayOfWeek) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0f))
                        .clickable { onUpdateMode(ScheduleMode.DAY_OF_WEEK) }
                        .padding(vertical = 6.dp)
                        .testTag("tab_day_of_week_mode"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Day of Week (Weekly)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isDayOfWeek) FontWeight.Bold else FontWeight.Normal,
                            color = if (isDayOfWeek) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Interval or Day of Week Settings
            if (config.mode == ScheduleMode.INTERVAL) {
                // Interval Days Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Repeat Interval:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(1, 2, 3, 7, 14, 30).forEach { preset ->
                            val isCurrent = config.intervalValue == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                    .border(1.dp, if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                                    .clickable { onUpdateInterval(preset, IntervalUnit.DAYS) }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${preset}d",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                // Day of Week
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val days = listOf(
                        Calendar.SUNDAY,
                        Calendar.MONDAY,
                        Calendar.TUESDAY,
                        Calendar.WEDNESDAY,
                        Calendar.THURSDAY,
                        Calendar.FRIDAY,
                        Calendar.SATURDAY
                    )
                    days.forEach { dayInt ->
                        val isSelected = config.dayOfWeek == dayInt
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .clickable { onUpdateDayOfWeek(dayInt) }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = FormatUtils.getDayOfWeekName(dayInt, short = true),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            // 4. System Constraints Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Battery not low
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = config.requireBatteryNotLow,
                        onCheckedChange = { onUpdateConstraints(it, config.requireCharging) },
                        modifier = Modifier.size(24.dp).testTag("battery_not_low_checkbox"),
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Skip if Battery Low",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                    )
                }

                // Charging only
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = config.requireCharging,
                        onCheckedChange = { onUpdateConstraints(config.requireBatteryNotLow, it) },
                        modifier = Modifier.size(24.dp).testTag("require_charging_checkbox"),
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Only When Charging",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                    )
                }

                // Notify
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = config.notifyOnCleanup,
                        onCheckedChange = onUpdateNotification,
                        modifier = Modifier.size(24.dp).testTag("notify_cleanup_checkbox"),
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Alert on Purge",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                    )
                }
            }
        }
    }
}
