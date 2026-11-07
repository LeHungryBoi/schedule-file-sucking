package com.example.data.storage

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.ApkFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class ScanResult(
    val files: List<ApkFileInfo>,
    val totalBytes: Long,
    val folderPath: String,
    val isSaf: Boolean
)

data class DeleteResult(
    val deletedCount: Int,
    val bytesReclaimed: Long,
    val failedCount: Int
)

class ApkScanner(private val context: Context) {

    suspend fun scanApkFiles(customFolderUriString: String? = null): ScanResult = withContext(Dispatchers.IO) {
        val apks = mutableListOf<ApkFileInfo>()
        var folderDisplayName = "Downloads"
        var isSaf = false

        if (!customFolderUriString.isNullOrEmpty()) {
            // Scan via Storage Access Framework
            try {
                val treeUri = Uri.parse(customFolderUriString)
                val documentFolder = DocumentFile.fromTreeUri(context, treeUri)
                if (documentFolder != null && documentFolder.isDirectory) {
                    folderDisplayName = documentFolder.name ?: "Selected Folder"
                    isSaf = true
                    scanDocumentFileDirectory(documentFolder, apks)
                }
            } catch (_: Exception) {
                // Fallback to standard downloads
            }
        }

        // If not SAF or SAF returned nothing / failed, scan standard Downloads directory
        if (apks.isEmpty() && !isSaf) {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir != null && downloadDir.exists() && downloadDir.isDirectory) {
                folderDisplayName = downloadDir.absolutePath
                scanDirectFileDirectory(downloadDir, apks)
            }
        }

        val totalBytes = apks.sumOf { it.sizeBytes }
        ScanResult(
            files = apks.sortedByDescending { it.lastModified },
            totalBytes = totalBytes,
            folderPath = folderDisplayName,
            isSaf = isSaf
        )
    }

    private fun scanDirectFileDirectory(directory: File, output: MutableList<ApkFileInfo>) {
        val files = directory.listFiles() ?: return
        for (file in files) {
            if (file.isFile && file.name.endsWith(".apk", ignoreCase = true)) {
                output.add(
                    ApkFileInfo(
                        name = file.name,
                        path = file.absolutePath,
                        uri = null,
                        sizeBytes = file.length(),
                        lastModified = file.lastModified(),
                        isDocumentFile = false
                    )
                )
            }
        }
    }

    private fun scanDocumentFileDirectory(folder: DocumentFile, output: MutableList<ApkFileInfo>) {
        val files = folder.listFiles()
        for (doc in files) {
            val name = doc.name ?: continue
            if (doc.isFile && name.endsWith(".apk", ignoreCase = true)) {
                output.add(
                    ApkFileInfo(
                        name = name,
                        path = doc.uri.toString(),
                        uri = doc.uri,
                        sizeBytes = doc.length(),
                        lastModified = doc.lastModified(),
                        isDocumentFile = true
                    )
                )
            }
        }
    }

    suspend fun deleteApkFiles(files: List<ApkFileInfo>): DeleteResult = withContext(Dispatchers.IO) {
        var deletedCount = 0
        var bytesReclaimed = 0L
        var failedCount = 0

        for (item in files) {
            var success = false
            try {
                if (item.isDocumentFile && item.uri != null) {
                    val doc = DocumentFile.fromSingleUri(context, item.uri)
                    if (doc != null && doc.delete()) {
                        success = true
                    }
                } else {
                    val file = File(item.path)
                    if (file.exists() && file.delete()) {
                        success = true
                    }
                }
            } catch (_: Exception) {
                success = false
            }

            if (success) {
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
