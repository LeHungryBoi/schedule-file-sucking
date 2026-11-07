package com.example.data.model

import android.net.Uri

data class ApkFileInfo(
    val name: String,
    val path: String,
    val uri: Uri? = null,
    val sizeBytes: Long,
    val lastModified: Long,
    val isDocumentFile: Boolean = false
)
