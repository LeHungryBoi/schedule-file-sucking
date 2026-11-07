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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.util.FormatUtils

@Composable
fun QuickCleanCard(
    apkCount: Int,
    apkBytes: Long,
    expiredScreenshotCount: Int,
    expiredScreenshotBytes: Long,
    folderName: String,
    isScanning: Boolean,
    isCleaning: Boolean,
    onCleanAll: () -> Unit,
    onCleanApksOnly: () -> Unit,
    onCleanScreenshotsOnly: () -> Unit,
    onRefreshScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalReclaimableBytes = apkBytes + expiredScreenshotBytes
    val totalPendingFiles = apkCount + expiredScreenshotCount

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Storage Sweep Monitor",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    IconButton(
                        onClick = onRefreshScan,
                        enabled = !isScanning && !isCleaning,
                        modifier = Modifier.size(28.dp).testTag("refresh_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // High Information Density 2-Column Metrics Box with 1.dp borders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Metric 1: APKs in Downloads
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "APK DOWNLOADS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$apkCount",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${FormatUtils.formatBytes(apkBytes)})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = folderName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                // Metric 2: Expired Screenshots
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (expiredScreenshotCount > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        )
                        .border(
                            1.dp,
                            if (expiredScreenshotCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (expiredScreenshotCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "EXPIRED SHOTS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (expiredScreenshotCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$expiredScreenshotCount",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp
                                ),
                                color = if (expiredScreenshotCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${FormatUtils.formatBytes(expiredScreenshotBytes)})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Auto-scanned via MediaStore",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Total reclaim banner (Thin 1.dp outline)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Storage Reclaimable:",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormatUtils.formatBytes(totalReclaimableBytes),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        ),
                        color = if (totalReclaimableBytes > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Primary Action: Sweep All
                Button(
                    onClick = onCleanAll,
                    enabled = totalPendingFiles > 0 && !isCleaning,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(36.dp)
                        .testTag("clean_all_now_button"),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCleaning) "Cleaning..." else "Sweep All (${FormatUtils.formatBytes(totalReclaimableBytes)})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }

                // Secondary Action 1: APKs Only
                OutlinedButton(
                    onClick = onCleanApksOnly,
                    enabled = apkCount > 0 && !isCleaning,
                    modifier = Modifier
                        .weight(0.9f)
                        .height(36.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(6.dp)
                        )
                        .testTag("clean_apks_only_button"),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "APKs ($apkCount)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                    )
                }

                // Secondary Action 2: Shots Only
                OutlinedButton(
                    onClick = onCleanScreenshotsOnly,
                    enabled = expiredScreenshotCount > 0 && !isCleaning,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .border(
                            1.dp,
                            if (expiredScreenshotCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(6.dp)
                        )
                        .testTag("clean_shots_only_button"),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Shots ($expiredScreenshotCount)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (expiredScreenshotCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
