package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ScreenshotFileInfo
import com.example.ui.viewmodel.ScreenshotFilter
import com.example.util.FormatUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScreenshotLeaderboardCard(
    files: List<ScreenshotFileInfo>,
    expiredCount: Int,
    expiredBytes: Long,
    totalBytes: Long,
    retentionDays: Int,
    filter: ScreenshotFilter,
    isCleaning: Boolean,
    onFilterChange: (ScreenshotFilter) -> Unit,
    onRetentionChange: (Int) -> Unit,
    onPurgeExpired: () -> Unit,
    onDeleteSingle: (ScreenshotFileInfo) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayedFiles = when (filter) {
        ScreenshotFilter.ALL -> files
        ScreenshotFilter.EXPIRED_ONLY -> files.filter { it.isExpired }
        ScreenshotFilter.PENDING_EXPIRY -> files.filter { !it.isExpired }
    }

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
            // Header: Title & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoDelete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Screenshot Purge Leaderboard",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(28.dp).testTag("refresh_screenshots_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Retention Rule Chip Selector (Power-user compact selector)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "RETENTION THRESHOLD",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (retentionDays == 1) "Keep within 24h (older deleted)" else "Keep within $retentionDays days (older deleted)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val presets = listOf(1, 3, 7, 14, 30)
                    presets.forEach { days ->
                        val isSelected = retentionDays == days
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { onRetentionChange(days) }
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${days}d",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            // Stats Strip (Compact 3-column metrics with thin borders)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Metric 1: Expired
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            if (expiredCount > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(6.dp)
                        )
                        .background(
                            if (expiredCount > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 5.dp)
                ) {
                    Column {
                        Text(
                            text = "EXPIRED QUEUE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (expiredCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$expiredCount",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp
                                ),
                                color = if (expiredCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${FormatUtils.formatBytes(expiredBytes)})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Metric 2: Total Scanned
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(6.dp)
                        )
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 5.dp)
                ) {
                    Column {
                        Text(
                            text = "TOTAL SCANNED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${files.size}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 16.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${FormatUtils.formatBytes(totalBytes)})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Purge Button if there are expired files
            AnimatedVisibility(visible = expiredCount > 0) {
                Button(
                    onClick = onPurgeExpired,
                    enabled = !isCleaning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .testTag("purge_expired_screenshots_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Purge $expiredCount Expired (${FormatUtils.formatBytes(expiredBytes)})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Filter Tabs (All / Expired / Pending)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val filters = listOf(
                    ScreenshotFilter.ALL to "All (${files.size})",
                    ScreenshotFilter.EXPIRED_ONLY to "Expired ($expiredCount)",
                    ScreenshotFilter.PENDING_EXPIRY to "Safe (${files.size - expiredCount})"
                )
                filters.forEach { (itemFilter, label) ->
                    val isSelected = filter == itemFilter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { onFilterChange(itemFilter) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Ranked List / Empty state
            if (displayedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "No screenshots in this category",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    displayedFiles.forEachIndexed { index, item ->
                        ScreenshotLeaderboardRow(
                            rank = index + 1,
                            item = item,
                            onDelete = { onDeleteSingle(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScreenshotLeaderboardRow(
    rank: Int,
    item: ScreenshotFileInfo,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpired = item.isExpired
    val borderCol = if (isExpired) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }

    val bgCol = if (isExpired) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bgCol)
            .border(1.dp, borderCol, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank Badge: #1, #2, etc.
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(
                    if (isExpired) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(4.dp)
                )
                .border(
                    1.dp,
                    if (isExpired) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Thumbnail with Coil
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = item.uri,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // File Details
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = FormatUtils.formatBytes(item.sizeBytes),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Age description
                Text(
                    text = "Age: ${FormatUtils.formatAgeString(item.ageDays)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Expiration badge
                if (isExpired) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                RoundedCornerShape(3.dp)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                                RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "EXPIRED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                RoundedCornerShape(3.dp)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = FormatUtils.formatCountdown(item.timeUntilExpiryMillis),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Individual Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp).testTag("delete_shot_${item.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
