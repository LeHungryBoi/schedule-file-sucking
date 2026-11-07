package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.ApkFileListCard
import com.example.ui.components.HeroAutomationCard
import com.example.ui.components.HistoryCard
import com.example.ui.components.QuickCleanCard
import com.example.ui.components.ScheduleConfigCard
import com.example.ui.components.ScreenshotLeaderboardCard
import com.example.ui.components.StorageAccessCard
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.MainViewModel
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showInfoDialog by remember { mutableStateOf(false) }
    var showConfirmCleanDialog by remember { mutableStateOf(false) }
    var cleanTargetMode by remember { mutableStateOf("ALL") } // "ALL", "APKS", "SCREENSHOTS"

    // Request permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.scanAllNow()
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissStatusMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_apk_cleaner_1793936298908),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Storage Sweeper",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // Power User Status Dot
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (uiState.scheduleConfig.isEnabled) MaterialTheme.colorScheme.secondary
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                )
                            }
                            Text(
                                text = "Downloads & Expired Screenshots Purger",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.scanAllNow() },
                        modifier = Modifier.size(36.dp).testTag("top_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan Storage",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.size(36.dp).testTag("info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "How It Works",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Power-User Segmented Navigation Tabs with 1.dp thin borders
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val tabs = listOf(
                        AppTab.OVERVIEW to "Overview",
                        AppTab.LEADERBOARD to "Leaderboard",
                        AppTab.SCHEDULE to "Rules",
                        AppTab.HISTORY to "Logs"
                    )

                    tabs.forEach { (tab, label) ->
                        val isSelected = uiState.selectedTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0f),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.selectTab(tab) }
                                .padding(vertical = 6.dp)
                                .testTag("tab_${tab.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // Tab Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (uiState.selectedTab) {
                    AppTab.OVERVIEW -> {
                        // 1. Hero Automation Card
                        item {
                            HeroAutomationCard(
                                config = uiState.scheduleConfig,
                                lastRunTimestamp = uiState.stats.lastCleanupTimestamp,
                                onToggleAutomation = { viewModel.toggleAutomation(it) }
                            )
                        }

                        // 2. Quick Clean Card (Dual monitor)
                        item {
                            QuickCleanCard(
                                apkCount = uiState.scannedApks.size,
                                apkBytes = uiState.totalApkBytes,
                                expiredScreenshotCount = uiState.expiredScreenshotCount,
                                expiredScreenshotBytes = uiState.expiredScreenshotBytes,
                                folderName = uiState.folderDisplayName,
                                isScanning = uiState.isScanning,
                                isCleaning = uiState.isCleaning,
                                onCleanAll = {
                                    cleanTargetMode = "ALL"
                                    showConfirmCleanDialog = true
                                },
                                onCleanApksOnly = {
                                    cleanTargetMode = "APKS"
                                    showConfirmCleanDialog = true
                                },
                                onCleanScreenshotsOnly = {
                                    cleanTargetMode = "SCREENSHOTS"
                                    showConfirmCleanDialog = true
                                },
                                onRefreshScan = { viewModel.scanAllNow() }
                            )
                        }

                        // 3. Mini Leaderboard Preview (Top 3 Expiring)
                        if (uiState.scannedScreenshots.isNotEmpty()) {
                            item {
                                ScreenshotLeaderboardCard(
                                    files = uiState.scannedScreenshots.take(5),
                                    expiredCount = uiState.expiredScreenshotCount,
                                    expiredBytes = uiState.expiredScreenshotBytes,
                                    totalBytes = uiState.totalScreenshotBytes,
                                    retentionDays = uiState.scheduleConfig.screenshotMaxAgeDays,
                                    filter = uiState.screenshotFilter,
                                    isCleaning = uiState.isCleaning,
                                    onFilterChange = { viewModel.setScreenshotFilter(it) },
                                    onRetentionChange = { viewModel.updateScreenshotMaxAgeDays(it) },
                                    onPurgeExpired = {
                                        cleanTargetMode = "SCREENSHOTS"
                                        showConfirmCleanDialog = true
                                    },
                                    onDeleteSingle = { viewModel.deleteSingleScreenshot(it) },
                                    onRefresh = { viewModel.scanAllNow() }
                                )
                            }
                        }

                        // 4. Detected APKs Preview
                        if (uiState.scannedApks.isNotEmpty()) {
                            item {
                                ApkFileListCard(
                                    files = uiState.scannedApks,
                                    onDeleteSingle = { viewModel.deleteSingleApk(it) }
                                )
                            }
                        }

                        // 5. Storage Permissions info
                        item {
                            StorageAccessCard(
                                hasStorageAccess = uiState.hasStorageAccess,
                                customFolderUri = uiState.customFolderUri,
                                folderDisplayName = uiState.folderDisplayName,
                                onFolderSelected = { viewModel.onFolderSelected(it) },
                                onResetToDefault = { viewModel.resetToDefaultDownloadFolder() },
                                onPermissionChanged = { viewModel.checkStorageAccess() }
                            )
                        }
                    }

                    AppTab.LEADERBOARD -> {
                        item {
                            ScreenshotLeaderboardCard(
                                files = uiState.scannedScreenshots,
                                expiredCount = uiState.expiredScreenshotCount,
                                expiredBytes = uiState.expiredScreenshotBytes,
                                totalBytes = uiState.totalScreenshotBytes,
                                retentionDays = uiState.scheduleConfig.screenshotMaxAgeDays,
                                filter = uiState.screenshotFilter,
                                isCleaning = uiState.isCleaning,
                                onFilterChange = { viewModel.setScreenshotFilter(it) },
                                onRetentionChange = { viewModel.updateScreenshotMaxAgeDays(it) },
                                onPurgeExpired = {
                                    cleanTargetMode = "SCREENSHOTS"
                                    showConfirmCleanDialog = true
                                },
                                onDeleteSingle = { viewModel.deleteSingleScreenshot(it) },
                                onRefresh = { viewModel.scanAllNow() }
                            )
                        }
                    }

                    AppTab.SCHEDULE -> {
                        item {
                            ScheduleConfigCard(
                                config = uiState.scheduleConfig,
                                onUpdateMode = { viewModel.updateScheduleMode(it) },
                                onUpdateInterval = { val_, unit -> viewModel.updateInterval(val_, unit) },
                                onUpdateTargetTime = { hour, min -> viewModel.updateTargetTime(hour, min) },
                                onUpdateDayOfWeek = { viewModel.updateDayOfWeek(it) },
                                onUpdateCleanApks = { viewModel.updateCleanApksEnabled(it) },
                                onUpdateCleanScreenshots = { viewModel.updateCleanScreenshotsEnabled(it) },
                                onUpdateScreenshotRetentionDays = { viewModel.updateScreenshotMaxAgeDays(it) },
                                onUpdateConstraints = { battery, charging -> viewModel.updateConstraints(battery, charging) },
                                onUpdateNotification = { viewModel.updateNotifyOnCleanup(it) }
                            )
                        }

                        item {
                            StorageAccessCard(
                                hasStorageAccess = uiState.hasStorageAccess,
                                customFolderUri = uiState.customFolderUri,
                                folderDisplayName = uiState.folderDisplayName,
                                onFolderSelected = { viewModel.onFolderSelected(it) },
                                onResetToDefault = { viewModel.resetToDefaultDownloadFolder() },
                                onPermissionChanged = { viewModel.checkStorageAccess() }
                            )
                        }
                    }

                    AppTab.HISTORY -> {
                        item {
                            HistoryCard(
                                stats = uiState.stats,
                                history = uiState.history,
                                onClearHistory = { viewModel.clearHistory() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog before Manual Clean
    if (showConfirmCleanDialog) {
        val titleText = when (cleanTargetMode) {
            "APKS" -> "Delete APK Installers?"
            "SCREENSHOTS" -> "Purge Expired Screenshots?"
            else -> "Execute Full Storage Sweep?"
        }

        val descriptionText = when (cleanTargetMode) {
            "APKS" -> "This will delete ${uiState.scannedApks.size} .apk installer file(s) from \"${uiState.folderDisplayName}\", reclaiming ${FormatUtils.formatBytes(uiState.totalApkBytes)}."
            "SCREENSHOTS" -> "This will permanently delete ${uiState.expiredScreenshotCount} screenshot(s) older than ${uiState.scheduleConfig.screenshotMaxAgeDays} day(s), reclaiming ${FormatUtils.formatBytes(uiState.expiredScreenshotBytes)}."
            else -> "This will purge ${uiState.scannedApks.size} APK file(s) and ${uiState.expiredScreenshotCount} expired screenshot(s), reclaiming a total of ${FormatUtils.formatBytes(uiState.totalApkBytes + uiState.expiredScreenshotBytes)}."
        }

        AlertDialog(
            onDismissRequest = { showConfirmCleanDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(titleText)
                }
            },
            text = {
                Text(descriptionText)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmCleanDialog = false
                        when (cleanTargetMode) {
                            "APKS" -> viewModel.cleanApksOnly()
                            "SCREENSHOTS" -> viewModel.cleanExpiredScreenshotsOnly()
                            else -> viewModel.cleanAllPending()
                        }
                    },
                    modifier = Modifier.testTag("confirm_clean_button")
                ) {
                    Text("Execute Purge", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCleanDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success dialog after clean
    uiState.lastCleanResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissCleanResultDialog() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Maintenance Complete")
                }
            },
            text = {
                val details = buildList {
                    if (result.apksDeleted > 0) add("${result.apksDeleted} APK file(s) (${FormatUtils.formatBytes(result.apkBytes)})")
                    if (result.screenshotsDeleted > 0) add("${result.screenshotsDeleted} screenshot(s) (${FormatUtils.formatBytes(result.screenshotBytes)})")
                }.joinToString(" and ")

                Text("Successfully purged $details, freeing a total of ${FormatUtils.formatBytes(result.totalBytes)}.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.dismissCleanResultDialog() },
                    modifier = Modifier.testTag("dismiss_result_button")
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Architecture Info Dialog explaining WorkManager and zero CPU usage
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Architecture & Scheduling")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "• 0% CPU Idle Overhead:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "The app does NOT run a permanent foreground service or memory-hogging background countdown.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "• Exact Time-of-Day Alignment:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Whether using Interval mode or Weekly schedule, the cleaner calculates initial delay to fire at your exact designated hour & minute.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "• MediaStore Screenshot Scanning:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Scans system screenshots and evaluates age against your retention threshold (e.g. 1 day, 7 days). Any older screenshot is queued on the Purge Leaderboard.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Understood", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
