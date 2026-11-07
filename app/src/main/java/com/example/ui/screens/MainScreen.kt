package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ThumbUp
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
import com.example.ui.components.StorageAccessCard
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

    // Request POST_NOTIFICATIONS on Android 13+ if enabled
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
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
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "APK Sweeper",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                            Text(
                                text = "Downloads Cleaner",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.testTag("info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Architecture Info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Hero Automation Switch Card
                item {
                    HeroAutomationCard(
                        config = uiState.scheduleConfig,
                        lastRunTimestamp = uiState.stats.lastCleanupTimestamp,
                        onToggleAutomation = { viewModel.toggleAutomation(it) }
                    )
                }

                // 2. Quick Clean Card (Current storage status + Clean Now button)
                item {
                    QuickCleanCard(
                        apkCount = uiState.scannedApks.size,
                        totalBytes = uiState.totalScannedBytes,
                        folderName = uiState.folderDisplayName,
                        isScanning = uiState.isScanning,
                        isCleaning = uiState.isCleaning,
                        onCleanNow = { showConfirmCleanDialog = true },
                        onRefreshScan = { viewModel.scanNow() }
                    )
                }

                // 3. Schedule & Interval Configuration Card
                item {
                    ScheduleConfigCard(
                        config = uiState.scheduleConfig,
                        onUpdateMode = { viewModel.updateScheduleMode(it) },
                        onUpdateInterval = { val_, unit -> viewModel.updateInterval(val_, unit) },
                        onUpdateDayOfWeek = { day, hour, min -> viewModel.updateDayOfWeekSchedule(day, hour, min) },
                        onUpdateConstraints = { battery, charging -> viewModel.updateConstraints(battery, charging) },
                        onUpdateNotification = { viewModel.updateNotifyOnCleanup(it) }
                    )
                }

                // 4. Storage & Directory Permissions Card
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

                // 5. Scanned APK Files Preview List
                item {
                    ApkFileListCard(
                        files = uiState.scannedApks,
                        onDeleteSingle = { viewModel.deleteSingleApk(it) }
                    )
                }

                // 6. Lifetime Stats and Recent History
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

    // Confirmation Dialog before Manual Clean
    if (showConfirmCleanDialog) {
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
                    Text("Delete APK Files?")
                }
            },
            text = {
                Text(
                    "This will delete ${uiState.scannedApks.size} .apk installer file(s) from \"${uiState.folderDisplayName}\", freeing ${FormatUtils.formatBytes(uiState.totalScannedBytes)} of storage space."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmCleanDialog = false
                        viewModel.cleanNow()
                    },
                    modifier = Modifier.testTag("confirm_clean_button")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
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
                    Text("Cleanup Complete")
                }
            },
            text = {
                Text(
                    "Successfully removed ${result.deletedCount} .apk file(s) and reclaimed ${FormatUtils.formatBytes(result.bytesReclaimed)}."
                )
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
                    Text("How Automation Works")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "• 0% CPU Idle Overhead:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "The app does NOT run a background service or constantly countdown in memory.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Android JobScheduler Integration:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Powered by Android's WorkManager API. The Android operating system wakes up the cleaner only when your interval arrives, during scheduled maintenance windows.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Battery & Doze Friendly:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Respects Android Doze mode and skips jobs if battery is low.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Got it", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
