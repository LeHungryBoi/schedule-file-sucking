package com.example.data.model

import android.net.Uri

data class ScreenshotFileInfo(
    val id: Long,
    val name: String,
    val uri: Uri,
    val path: String,
    val sizeBytes: Long,
    val dateTaken: Long,
    val ageDays: Double,
    val isExpired: Boolean,
    val timeUntilExpiryMillis: Long
)
