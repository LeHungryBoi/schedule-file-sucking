package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.data.model.ScheduleConfig
import com.example.util.FormatUtils

@Composable
fun HeroAutomationCard(
    config: ScheduleConfig,
    lastRunTimestamp: Long,
    onToggleAutomation: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnabled = config.isEnabled
    val borderColor = if (isEnabled) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .testTag("hero_automation_card"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Main Switch Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEnabled) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.PauseCircle,
                            contentDescription = if (isEnabled) "Active" else "Paused",
                            tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isEnabled) "Automation Active" else "Automation Paused",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Status Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (isEnabled) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        1.dp,
                                        if (isEnabled) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        RoundedCornerShape(3.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (isEnabled) "WORKMANAGER" else "STANDBY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = if (isEnabled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = if (isEnabled) FormatUtils.formatScheduleSummary(config) else "Turn on to automatically run background cleanup",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleAutomation,
                    modifier = Modifier.testTag("automation_toggle"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            AnimatedVisibility(visible = isEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Next execution & cleaners strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Next Execution:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = FormatUtils.formatNextScheduledExecution(config, lastRunTimestamp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (config.cleanApksEnabled) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("APK", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (config.cleanScreenshotsEnabled) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("SHOTS (>${config.screenshotMaxAgeDays}d)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
