package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ApkFileInfo
import com.example.data.model.CleanupHistoryItem
import com.example.data.model.IntervalUnit
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleMode
import com.example.data.preferences.PreferencesManager
import com.example.data.preferences.StatsData
import com.example.data.storage.ApkScanner
import com.example.data.storage.DeleteResult
import com.example.worker.WorkManagerScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val scheduleConfig: ScheduleConfig = ScheduleConfig(),
    val customFolderUri: String? = null,
    val stats: StatsData = StatsData(0L, 0, 0L),
    val history: List<CleanupHistoryItem> = emptyList(),
    val scannedApks: List<ApkFileInfo> = emptyList(),
    val totalScannedBytes: Long = 0L,
    val folderDisplayName: String = "Downloads",
    val isScanning: Boolean = false,
    val isCleaning: Boolean = false,
    val lastCleanResult: DeleteResult? = null,
    val hasStorageAccess: Boolean = false,
    val statusMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val prefs = PreferencesManager(context)
    private val scanner = ApkScanner(context)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        observePreferences()
        checkStorageAccess()
        scanNow()
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
                scanNow()
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
        viewModelScope.launch {
            prefs.updateScheduleConfig(updated)
            WorkManagerScheduler.scheduleOrCancel(context, updated)
            _uiState.update {
                it.copy(
                    statusMessage = if (enabled) "Automation scheduled with WorkManager" else "Automation paused"
                )
            }
        }
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

    fun updateDayOfWeekSchedule(dayOfWeek: Int, hour: Int, minute: Int) {
        val updated = _uiState.value.scheduleConfig.copy(
            dayOfWeek = dayOfWeek,
            targetHour = hour,
            targetMinute = minute
        )
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

    fun scanNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val result = scanner.scanApkFiles(_uiState.value.customFolderUri)
            _uiState.update {
                it.copy(
                    isScanning = false,
                    scannedApks = result.files,
                    totalScannedBytes = result.totalBytes,
                    folderDisplayName = result.folderPath
                )
            }
        }
    }

    fun cleanNow() {
        val apks = _uiState.value.scannedApks
        if (apks.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCleaning = true) }
            val deleteResult = scanner.deleteApkFiles(apks)

            prefs.recordCleanupRun(
                filesDeleted = deleteResult.deletedCount,
                bytesReclaimed = deleteResult.bytesReclaimed,
                isAutomated = false
            )

            _uiState.update {
                it.copy(
                    isCleaning = false,
                    lastCleanResult = deleteResult,
                    statusMessage = "Cleaned ${deleteResult.deletedCount} files (${deleteResult.bytesReclaimed / (1024 * 1024)} MB freed)"
                )
            }

            // Rescan directory to refresh list
            scanNow()
        }
    }

    fun deleteSingleApk(apk: ApkFileInfo) {
        viewModelScope.launch {
            val result = scanner.deleteApkFiles(listOf(apk))
            if (result.deletedCount > 0) {
                prefs.recordCleanupRun(
                    filesDeleted = 1,
                    bytesReclaimed = result.bytesReclaimed,
                    isAutomated = false
                )
                scanNow()
            }
        }
    }

    fun onFolderSelected(uri: Uri?) {
        if (uri == null) return
        try {
            // Take persistable permission for SAF
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Some providers do not support persistable URIs
        }
        viewModelScope.launch {
            prefs.setCustomFolderUri(uri.toString())
            checkStorageAccess()
            scanNow()
        }
    }

    fun resetToDefaultDownloadFolder() {
        viewModelScope.launch {
            prefs.setCustomFolderUri(null)
            checkStorageAccess()
            scanNow()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            prefs.clearHistory()
        }
    }

    fun dismissStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun dismissCleanResultDialog() {
        _uiState.update { it.copy(lastCleanResult = null) }
    }
}
