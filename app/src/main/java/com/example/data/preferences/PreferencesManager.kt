package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.CleanupHistoryItem
import com.example.data.model.IntervalUnit
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "apk_sweeper_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        private val KEY_AUTOMATION_ENABLED = booleanPreferencesKey("automation_enabled")
        private val KEY_SCHEDULE_MODE = stringPreferencesKey("schedule_mode")
        private val KEY_INTERVAL_VALUE = intPreferencesKey("interval_value")
        private val KEY_INTERVAL_UNIT = stringPreferencesKey("interval_unit")
        private val KEY_DAY_OF_WEEK = intPreferencesKey("day_of_week")
        private val KEY_TARGET_HOUR = intPreferencesKey("target_hour")
        private val KEY_TARGET_MINUTE = intPreferencesKey("target_minute")
        private val KEY_REQUIRE_BATTERY_NOT_LOW = booleanPreferencesKey("require_battery_not_low")
        private val KEY_REQUIRE_CHARGING = booleanPreferencesKey("require_charging")
        private val KEY_NOTIFY_ON_CLEANUP = booleanPreferencesKey("notify_on_cleanup")
        private val KEY_CUSTOM_FOLDER_URI = stringPreferencesKey("custom_folder_uri")

        private val KEY_LAST_CLEANUP_TIMESTAMP = longPreferencesKey("last_cleanup_timestamp")
        private val KEY_TOTAL_APKS_DELETED = intPreferencesKey("total_apks_deleted")
        private val KEY_TOTAL_BYTES_RECLAIMED = longPreferencesKey("total_bytes_reclaimed")
        private val KEY_HISTORY_JSON = stringPreferencesKey("history_json")
    }

    val scheduleConfigFlow: Flow<ScheduleConfig> = context.dataStore.data.map { prefs ->
        val enabled = prefs[KEY_AUTOMATION_ENABLED] ?: false
        val modeStr = prefs[KEY_SCHEDULE_MODE] ?: ScheduleMode.INTERVAL.name
        val mode = try { ScheduleMode.valueOf(modeStr) } catch (_: Exception) { ScheduleMode.INTERVAL }
        val intervalValue = prefs[KEY_INTERVAL_VALUE] ?: 1
        val unitStr = prefs[KEY_INTERVAL_UNIT] ?: IntervalUnit.DAYS.name
        val unit = try { IntervalUnit.valueOf(unitStr) } catch (_: Exception) { IntervalUnit.DAYS }
        val dayOfWeek = prefs[KEY_DAY_OF_WEEK] ?: Calendar.SUNDAY
        val targetHour = prefs[KEY_TARGET_HOUR] ?: 3
        val targetMinute = prefs[KEY_TARGET_MINUTE] ?: 0
        val requireBattery = prefs[KEY_REQUIRE_BATTERY_NOT_LOW] ?: true
        val requireCharging = prefs[KEY_REQUIRE_CHARGING] ?: false
        val notify = prefs[KEY_NOTIFY_ON_CLEANUP] ?: true

        ScheduleConfig(
            isEnabled = enabled,
            mode = mode,
            intervalValue = intervalValue,
            intervalUnit = unit,
            dayOfWeek = dayOfWeek,
            targetHour = targetHour,
            targetMinute = targetMinute,
            requireBatteryNotLow = requireBattery,
            requireCharging = requireCharging,
            notifyOnCleanup = notify
        )
    }

    val customFolderUriFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_FOLDER_URI]
    }

    val statsFlow: Flow<StatsData> = context.dataStore.data.map { prefs ->
        StatsData(
            lastCleanupTimestamp = prefs[KEY_LAST_CLEANUP_TIMESTAMP] ?: 0L,
            totalApksDeleted = prefs[KEY_TOTAL_APKS_DELETED] ?: 0,
            totalBytesReclaimed = prefs[KEY_TOTAL_BYTES_RECLAIMED] ?: 0L
        )
    }

    val historyFlow: Flow<List<CleanupHistoryItem>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_HISTORY_JSON] ?: "[]"
        parseHistoryJson(jsonStr)
    }

    suspend fun setAutomationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTOMATION_ENABLED] = enabled
        }
    }

    suspend fun updateScheduleConfig(config: ScheduleConfig) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTOMATION_ENABLED] = config.isEnabled
            prefs[KEY_SCHEDULE_MODE] = config.mode.name
            prefs[KEY_INTERVAL_VALUE] = config.intervalValue
            prefs[KEY_INTERVAL_UNIT] = config.intervalUnit.name
            prefs[KEY_DAY_OF_WEEK] = config.dayOfWeek
            prefs[KEY_TARGET_HOUR] = config.targetHour
            prefs[KEY_TARGET_MINUTE] = config.targetMinute
            prefs[KEY_REQUIRE_BATTERY_NOT_LOW] = config.requireBatteryNotLow
            prefs[KEY_REQUIRE_CHARGING] = config.requireCharging
            prefs[KEY_NOTIFY_ON_CLEANUP] = config.notifyOnCleanup
        }
    }

    suspend fun setCustomFolderUri(uriString: String?) {
        context.dataStore.edit { prefs ->
            if (uriString != null) {
                prefs[KEY_CUSTOM_FOLDER_URI] = uriString
            } else {
                prefs.remove(KEY_CUSTOM_FOLDER_URI)
            }
        }
    }

    suspend fun recordCleanupRun(
        filesDeleted: Int,
        bytesReclaimed: Long,
        isAutomated: Boolean
    ) {
        context.dataStore.edit { prefs ->
            val now = System.currentTimeMillis()
            prefs[KEY_LAST_CLEANUP_TIMESTAMP] = now
            val currentDeleted = prefs[KEY_TOTAL_APKS_DELETED] ?: 0
            prefs[KEY_TOTAL_APKS_DELETED] = currentDeleted + filesDeleted
            val currentBytes = prefs[KEY_TOTAL_BYTES_RECLAIMED] ?: 0L
            prefs[KEY_TOTAL_BYTES_RECLAIMED] = currentBytes + bytesReclaimed

            // Append to history (keep top 20 items)
            val currentHistory = parseHistoryJson(prefs[KEY_HISTORY_JSON] ?: "[]").toMutableList()
            currentHistory.add(
                0,
                CleanupHistoryItem(
                    id = now.toString(),
                    timestamp = now,
                    filesDeleted = filesDeleted,
                    bytesReclaimed = bytesReclaimed,
                    isAutomated = isAutomated
                )
            )
            val trimmed = currentHistory.take(20)
            prefs[KEY_HISTORY_JSON] = serializeHistoryJson(trimmed)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { prefs ->
            prefs[KEY_HISTORY_JSON] = "[]"
            prefs[KEY_TOTAL_APKS_DELETED] = 0
            prefs[KEY_TOTAL_BYTES_RECLAIMED] = 0L
        }
    }

    private fun parseHistoryJson(jsonStr: String): List<CleanupHistoryItem> {
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<CleanupHistoryItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CleanupHistoryItem(
                        id = obj.optString("id", i.toString()),
                        timestamp = obj.optLong("timestamp", 0L),
                        filesDeleted = obj.optInt("filesDeleted", 0),
                        bytesReclaimed = obj.optLong("bytesReclaimed", 0L),
                        isAutomated = obj.optBoolean("isAutomated", true)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeHistoryJson(items: List<CleanupHistoryItem>): String {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("filesDeleted", item.filesDeleted)
                put("bytesReclaimed", item.bytesReclaimed)
                put("isAutomated", item.isAutomated)
            }
            array.put(obj)
        }
        return array.toString()
    }
}

data class StatsData(
    val lastCleanupTimestamp: Long,
    val totalApksDeleted: Int,
    val totalBytesReclaimed: Long
)
