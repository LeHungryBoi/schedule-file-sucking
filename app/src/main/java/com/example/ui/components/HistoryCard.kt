package com.example.ui.components

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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CleanupHistoryItem
import com.example.data.preferences.StatsData
import com.example.util.FormatUtils

@Composable
fun HistoryCard(
    stats: StatsData,
    history: List<CleanupHistoryItem>,
    onClearHistory: () -> Unit,
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
            .testTag("history_card"),
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lifetime Stats & Execution Log",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (history.isNotEmpty() || stats.totalBytesReclaimed > 0) {
                    TextButton(
                        onClick = onClearHistory,
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Text(
                            text = "Clear Log",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // 3-Column Lifetime Stats Grid with 1.dp borders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Stat 1: APKs Cleared
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = "APKS PURGED",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${stats.totalApksDeleted}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                // Stat 2: Screenshots Cleared
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = "SHOTS PURGED",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "${stats.totalScreenshotsDeleted}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                // Stat 3: Total Space Reclaimed
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = "SPACE RECLAIMED",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FormatUtils.formatBytes(stats.totalBytesReclaimed),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // History Log List
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No cleanup history logged yet.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    history.take(15).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (item.isAutomated) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.secondaryContainer
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (item.isAutomated) Icons.Default.AutoAwesome else Icons.Default.TouchApp,
                                        contentDescription = null,
                                        tint = if (item.isAutomated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (item.isAutomated) "Scheduled Run" else "Manual Clean",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val breakdown = buildList {
                                            if (item.apksDeleted > 0) add("${item.apksDeleted} APK")
                                            if (item.screenshotsDeleted > 0) add("${item.screenshotsDeleted} Shot")
                                            if (isEmpty() && item.filesDeleted > 0) add("${item.filesDeleted} files")
                                        }.joinToString(" + ")

                                        if (breakdown.isNotEmpty()) {
                                            Text(
                                                text = "($breakdown)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = FormatUtils.formatDate(item.timestamp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                text = "+${FormatUtils.formatBytes(item.bytesReclaimed)}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}
