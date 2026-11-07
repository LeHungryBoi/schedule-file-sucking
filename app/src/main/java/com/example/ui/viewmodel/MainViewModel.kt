package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ApkFileInfo
import com.example.data.model.CleanupHistoryItem
import com.example.data.model.IntervalUnit
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleMode
import com.example.data.model.ScreenshotFileInfo
import com.example.data.preferences.PreferencesManager
import com.example.data.preferences.StatsData
import com.example.data.storage.ApkScanner
import com.example.data.storage.DeleteResult
import com.example.data.storage.ScreenshotScanner
import com.example.worker.WorkManagerScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab {
    OVERVIEW,
    LEADERBOARD,
    SCHEDULE,
    HISTORY
}

enum class ScreenshotFilter {
    ALL,
    EXPIRED_ONLY,
    PENDING_EXPIRY
}

data class CombinedCleanResult(
    val apksDeleted: Int,
    val apkBytes: Long,
    val screenshotsDeleted: Int,
    val screenshotBytes: Long
) {
    val totalDeleted: Int get() = apksDeleted + screenshotsDeleted
    val totalBytes: Long get() = apkBytes + screenshotBytes
}

data class UiState(
    val scheduleConfig: ScheduleConfig = ScheduleConfig(),
    val customFolderUri: String? = null,
    val stats: StatsData = StatsData(0L, 0, 0, 0L),
    val history: List<CleanupHistoryItem> = emptyList(),
    val scannedApks: List<ApkFileInfo> = emptyList(),
    val totalApkBytes: Long = 0L,
    val folderDisplayName: String = "Downloads",
    val scannedScreenshots: List<ScreenshotFileInfo> = emptyList(),
    val expiredScreenshotCount: Int = 0,
    val expiredScreenshotBytes: Long = 0L,
    val totalScreenshotBytes: Long = 0L,
    val isScanning: Boolean = false,
    val isCleaning: Boolean = false,
    val lastCleanResult: CombinedCleanResult? = null,
    val hasStorageAccess: Boolean = false,
    val statusMessage: String? = null,
    val selectedTab: AppTab = AppTab.OVERVIEW,
    val screenshotFilter: ScreenshotFilter = ScreenshotFilter.ALL
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val prefs = PreferencesManager(context)
    private val apkScanner = ApkScanner(context)
    private val screenshotScanner = ScreenshotScanner(context)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        observePreferences()
        checkStorageAccess()
        scanAllNow()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            prefs.scheduleConfigFlow.collectLatest { config ->
                _uiState.update { it.copy(scheduleConfig = config) }
            }
        }
        viewModelScope.launch {
            prefs.customFolderUriFlow.collectLatest { uri ->
                _uiState.update { it.copy(customFolderUri = uri) }
                scanApks()
            }
        }
        viewModelScope.launch {
            prefs.statsFlow.collectLatest { stats ->
                _uiState.update { it.copy(stats = stats) }
            }
        }
        viewModelScope.launch {
            prefs.historyFlow.collectLatest { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setScreenshotFilter(filter: ScreenshotFilter) {
        _uiState.update { it.copy(screenshotFilter = filter) }
    }

    fun checkStorageAccess() {
        val hasAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager() || !_uiState.value.customFolderUri.isNullOrEmpty()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                    !_uiState.value.customFolderUri.isNullOrEmpty()
        }
        _uiState.update { it.copy(hasStorageAccess = hasAccess) }
    }

    fun toggleAutomation(enabled: Boolean) {
        val updated = _uiState.value.scheduleConfig.copy(isEnabled = enabled)
        saveAndReschedule(updated)
        _uiState.update {
            it.copy(
                statusMessage = if (enabled) "Automation active with WorkManager" else "Automation paused"
            )
        }
    }

    fun updateCleanApksEnabled(enabled: Boolean) {
        val updated = _uiState.value.scheduleConfig.copy(cleanApksEnabled = enabled)
        saveAndReschedule(updated)
    }

    fun updateCleanScreenshotsEnabled(enabled: Boolean) {
        val updated = _uiState.value.scheduleConfig.copy(cleanScreenshotsEnabled = enabled)
        saveAndReschedule(updated)
    }

    fun updateScreenshotMaxAgeDays(days: Int) {
        val updated = _uiState.value.scheduleConfig.copy(screenshotMaxAgeDays = days.coerceAtLeast(1))
        saveAndReschedule(updated)
        scanScreenshots(days.coerceAtLeast(1))
    }

    fun updateScheduleMode(mode: ScheduleMode) {
        val updated = _uiState.value.scheduleConfig.copy(mode = mode)
        saveAndReschedule(updated)
    }

    fun updateInterval(value: Int, unit: IntervalUnit) {
        val updated = _uiState.value.scheduleConfig.copy(
            intervalValue = value.coerceAtLeast(1),
            intervalUnit = unit
        )
        saveAndReschedule(updated)
    }

    fun updateTargetTime(hour: Int, minute: Int) {
        val updated = _uiState.value.scheduleConfig.copy(
            targetHour = hour.coerceIn(0, 23),
            targetMinute = minute.coerceIn(0, 59)
        )
        saveAndReschedule(updated)
    }

    fun updateDayOfWeek(dayOfWeek: Int) {
        val updated = _uiState.value.scheduleConfig.copy(dayOfWeek = dayOfWeek)
        saveAndReschedule(updated)
    }

    fun updateConstraints(requireBatteryNotLow: Boolean, requireCharging: Boolean) {
        val updated = _uiState.value.scheduleConfig.copy(
            requireBatteryNotLow = requireBatteryNotLow,
            requireCharging = requireCharging
        )
        saveAndReschedule(updated)
    }

    fun updateNotifyOnCleanup(notify: Boolean) {
        val updated = _uiState.value.scheduleConfig.copy(notifyOnCleanup = notify)
        saveAndReschedule(updated)
    }

    private fun saveAndReschedule(config: ScheduleConfig) {
        viewModelScope.launch {
            prefs.updateScheduleConfig(config)
            WorkManagerScheduler.scheduleOrCancel(context, config)
        }
    }

    fun scanAllNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val apkResult = apkScanner.scanApkFiles(_uiState.value.customFolderUri)
            val shotResult = screenshotScanner.scanScreenshots(_uiState.value.scheduleConfig.screenshotMaxAgeDays)

            _uiState.update {
                it.copy(
                    isScanning = false,
                    scannedApks = apkResult.files,
                    totalApkBytes = apkResult.totalBytes,
                    folderDisplayName = apkResult.folderPath,
                    scannedScreenshots = shotResult.files,
                    expiredScreenshotCount = shotResult.expiredCount,
                    expiredScreenshotBytes = shotResult.expiredBytes,
                    totalScreenshotBytes = shotResult.totalBytes
                )
            }
        }
    }

    private fun scanApks() {
        viewModelScope.launch {
            val apkResult = apkScanner.scanApkFiles(_uiState.value.customFolderUri)
            _uiState.update {
                it.copy(
                    scannedApks = apkResult.files,
                    totalApkBytes = apkResult.totalBytes,
                    folderDisplayName = apkResult.folderPath
                )
            }
        }
    }

    private fun scanScreenshots(maxAgeDays: Int) {
        viewModelScope.launch {
            val shotResult = screenshotScanner.scanScreenshots(maxAgeDays)
            _uiState.update {
                it.copy(
                    scannedScreenshots = shotResult.files,
                    expiredScreenshotCount = shotResult.expiredCount,
                    expiredScreenshotBytes = shotResult.expiredBytes,
                    totalScreenshotBytes = shotResult.totalBytes
                )
            }
        }
    }

    fun cleanExpiredScreenshotsOnly() {
        val expired = _uiState.value.scannedScreenshots.filter { it.isExpired }
        if (expired.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCleaning = true) }
            val deleteResult = screenshotScanner.deleteScreenshots(expired)

            prefs.recordCleanupRun(
                apksDeleted = 0,
                screenshotsDeleted = deleteResult.deletedCount,
                bytesReclaimed = deleteResult.bytesReclaimed,
                isAutomated = false
            )

            val combined = CombinedCleanResult(
                apksDeleted = 0,
                apkBytes = 0L,
                screenshotsDeleted = deleteResult.deletedCount,
                screenshotBytes = deleteResult.bytesReclaimed
            )

            _uiState.update {
                it.copy(
                    isCleaning = false,
                    lastCleanResult = combined,
                    statusMessage = "Purged ${deleteResult.deletedCount} expired screenshot(s)"
                )
            }
            scanScreenshots(_uiState.value.scheduleConfig.screenshotMaxAgeDays)
        }
    }

    fun cleanApksOnly() {
        val apks = _uiState.value.scannedApks
        if (apks.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCleaning = true) }
            val deleteResult = apkScanner.deleteApkFiles(apks)

            prefs.recordCleanupRun(
                apksDeleted = deleteResult.deletedCount,
                screenshotsDeleted = 0,
                bytesReclaimed = deleteResult.bytesReclaimed,
                isAutomated = false
            )

            val combined = CombinedCleanResult(
                apksDeleted = deleteResult.deletedCount,
                apkBytes = deleteResult.bytesReclaimed,
                screenshotsDeleted = 0,
                screenshotBytes = 0L
            )

            _uiState.update {
                it.copy(
                    isCleaning = false,
                    lastCleanResult = combined,
                    statusMessage = "Cleaned ${deleteResult.deletedCount} APK file(s)"
                )
            }
            scanApks()
        }
    }

    fun cleanAllPending() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCleaning = true) }
            var apksCount = 0
            var apkBytes = 0L
            if (_uiState.value.scannedApks.isNotEmpty()) {
                val apkDel = apkScanner.deleteApkFiles(_uiState.value.scannedApks)
                apksCount = apkDel.deletedCount
                apkBytes = apkDel.bytesReclaimed
            }

            var shotCount = 0
            var shotBytes = 0L
            val expiredShots = _uiState.value.scannedScreenshots.filter { it.isExpired }
            if (expiredShots.isNotEmpty()) {
                val shotDel = screenshotScanner.deleteScreenshots(expiredShots)
                shotCount = shotDel.deletedCount
                shotBytes = shotDel.bytesReclaimed
            }

            val totalBytes = apkBytes + shotBytes
            prefs.recordCleanupRun(
                apksDeleted = apksCount,
                screenshotsDeleted = shotCount,
                bytesReclaimed = totalBytes,
                isAutomated = false
            )

            val combined = CombinedCleanResult(
                apksDeleted = apksCount,
                apkBytes = apkBytes,
                screenshotsDeleted = shotCount,
                screenshotBytes = shotBytes
            )

            _uiState.update {
                it.copy(
                    isCleaning = false,
                    lastCleanResult = combined,
                    statusMessage = "Cleaned $apksCount APK(s) and $shotCount screenshot(s)"
                )
            }
            scanAllNow()
        }
    }

    fun deleteSingleApk(apk: ApkFileInfo) {
        viewModelScope.launch {
            val result = apkScanner.deleteApkFiles(listOf(apk))
            if (result.deletedCount > 0) {
                prefs.recordCleanupRun(
                    apksDeleted = 1,
                    screenshotsDeleted = 0,
                    bytesReclaimed = result.bytesReclaimed,
                    isAutomated = false
                )
                scanApks()
            }
        }
    }

    fun deleteSingleScreenshot(shot: ScreenshotFileInfo) {
        viewModelScope.launch {
            val result = screenshotScanner.deleteScreenshots(listOf(shot))
            if (result.deletedCount > 0) {
                prefs.recordCleanupRun(
                    apksDeleted = 0,
                    screenshotsDeleted = 1,
                    bytesReclaimed = result.bytesReclaimed,
                    isAutomated = false
                )
                scanScreenshots(_uiState.value.scheduleConfig.screenshotMaxAgeDays)
            }
        }
    }

    fun onFolderSelected(uri: Uri?) {
        if (uri == null) return
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {}
        viewModelScope.launch {
            prefs.setCustomFolderUri(uri.toString())
            checkStorageAccess()
            scanApks()
        }
    }

    fun resetToDefaultDownloadFolder() {
        viewModelScope.launch {
            prefs.setCustomFolderUri(null)
            checkStorageAccess()
            scanApks()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            prefs.clearHistory()
        }
    }

    fun scanNow() = scanAllNow()

    fun cleanNow() = cleanAllPending()

    fun dismissStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun dismissCleanResultDialog() {
        _uiState.update { it.copy(lastCleanResult = null) }
    }
}
