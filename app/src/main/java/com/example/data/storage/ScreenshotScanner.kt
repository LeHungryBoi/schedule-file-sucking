package com.example.data.storage

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.ScreenshotFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ScreenshotScanResult(
    val files: List<ScreenshotFileInfo>,
    val expiredCount: Int,
    val expiredBytes: Long,
    val totalBytes: Long,
    val maxAgeDays: Int
)

class ScreenshotScanner(private val context: Context) {

    suspend fun scanScreenshots(maxAgeDays: Int): ScreenshotScanResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val screenshotItems = mutableMapOf<String, ScreenshotFileInfo>()

        // 1. Query MediaStore
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_MODIFIED
            )

            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} ASC"
            val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            context.contentResolver.query(
                queryUri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Screenshot_${id}.png"
                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                    val size = cursor.getLong(sizeCol)

                    // Check if image belongs to Screenshots
                    val isScreenshotPath = path.contains("screenshot", ignoreCase = true) ||
                            path.contains("screen_shot", ignoreCase = true) ||
                            path.contains("screencapture", ignoreCase = true) ||
                            name.startsWith("screenshot", ignoreCase = true) ||
                            name.startsWith("screencap", ignoreCase = true)

                    if (!isScreenshotPath) continue

                    var dateTaken = cursor.getLong(dateTakenCol)
                    if (dateTaken <= 0L) {
                        val dateAdded = cursor.getLong(dateAddedCol)
                        val dateMod = cursor.getLong(dateModCol)
                        dateTaken = when {
                            dateAdded > 0L -> dateAdded * 1000L
                            dateMod > 0L -> dateMod * 1000L
                            else -> now
                        }
                    }

                    val contentUri = ContentUris.withAppendedId(queryUri, id)
                    val ageMillis = (now - dateTaken).coerceAtLeast(0L)
                    val ageDays = ageMillis / (1000.0 * 60 * 60 * 24)
                    val maxAgeMillis = maxAgeDays.toLong() * 24L * 60L * 60L * 1000L
                    val isExpired = ageMillis >= maxAgeMillis
                    val timeUntilExpiryMillis = if (isExpired) 0L else (maxAgeMillis - ageMillis)

                    val item = ScreenshotFileInfo(
                        id = id,
                        name = name,
                        uri = contentUri,
                        path = path,
                        sizeBytes = size,
                        dateTaken = dateTaken,
                        ageDays = ageDays,
                        isExpired = isExpired,
                        timeUntilExpiryMillis = timeUntilExpiryMillis
                    )
                    val key = if (path.isNotEmpty()) path else contentUri.toString()
                    screenshotItems[key] = item
                }
            }
        } catch (_: Exception) {
            // MediaStore query error fallback
        }

        // 2. Direct directory scan fallback for system directories
        scanDirectFolder(
            folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Screenshots"),
            now = now,
            maxAgeDays = maxAgeDays,
            output = screenshotItems
        )
        scanDirectFolder(
            folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Screenshots"),
            now = now,
            maxAgeDays = maxAgeDays,
            output = screenshotItems
        )

        // Sort: Expired first, then oldest to newest (Leaderboard ranking)
        val sortedList = screenshotItems.values.toList().sortedWith(
            compareByDescending<ScreenshotFileInfo> { it.isExpired }
                .thenByDescending { it.ageDays }
        )

        val expiredList = sortedList.filter { it.isExpired }
        val expiredBytes = expiredList.sumOf { it.sizeBytes }
        val totalBytes = sortedList.sumOf { it.sizeBytes }

        ScreenshotScanResult(
            files = sortedList,
            expiredCount = expiredList.size,
            expiredBytes = expiredBytes,
            totalBytes = totalBytes,
            maxAgeDays = maxAgeDays
        )
    }

    private fun scanDirectFolder(
        folder: File,
        now: Long,
        maxAgeDays: Int,
        output: MutableMap<String, ScreenshotFileInfo>
    ) {
        if (!folder.exists() || !folder.isDirectory) return
        val files = folder.listFiles() ?: return

        for (file in files) {
            if (!file.isFile) continue
            val ext = file.extension.lowercase()
            if (ext != "png" && ext != "jpg" && ext != "jpeg" && ext != "webp") continue

            val path = file.absolutePath
            if (output.containsKey(path)) continue

            val dateTaken = file.lastModified()
            val ageMillis = (now - dateTaken).coerceAtLeast(0L)
            val ageDays = ageMillis / (1000.0 * 60 * 60 * 24)
            val maxAgeMillis = maxAgeDays.toLong() * 24L * 60L * 60L * 1000L
            val isExpired = ageMillis >= maxAgeMillis
            val timeUntilExpiryMillis = if (isExpired) 0L else (maxAgeMillis - ageMillis)

            val uri = Uri.fromFile(file)
            val item = ScreenshotFileInfo(
                id = file.hashCode().toLong(),
                name = file.name,
                uri = uri,
                path = path,
                sizeBytes = file.length(),
                dateTaken = dateTaken,
                ageDays = ageDays,
                isExpired = isExpired,
                timeUntilExpiryMillis = timeUntilExpiryMillis
            )
            output[path] = item
        }
    }

    suspend fun deleteScreenshots(files: List<ScreenshotFileInfo>): DeleteResult = withContext(Dispatchers.IO) {
        var deletedCount = 0
        var bytesReclaimed = 0L
        var failedCount = 0

        for (item in files) {
            var deleted = false
            // 1. Try content resolver delete
            try {
                if (item.uri.scheme == "content") {
                    val count = context.contentResolver.delete(item.uri, null, null)
                    if (count > 0) {
                        deleted = true
                    }
                }
            } catch (_: Exception) {
                // Ignore and try direct File deletion
            }

            // 2. Fallback to direct file deletion
            if (!deleted && item.path.isNotEmpty()) {
                try {
                    val file = File(item.path)
                    if (file.exists() && file.delete()) {
                        deleted = true
                    }
                } catch (_: Exception) {
                    deleted = false
                }
            }

            if (deleted) {
                deletedCount++
                bytesReclaimed += item.sizeBytes
            } else {
                failedCount++
            }
        }

        DeleteResult(
            deletedCount = deletedCount,
            bytesReclaimed = bytesReclaimed,
            failedCount = failedCount
        )
    }
}
