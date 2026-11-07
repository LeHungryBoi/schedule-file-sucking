package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StorageAccessCard(
    hasStorageAccess: Boolean,
    customFolderUri: String?,
    folderDisplayName: String,
    onFolderSelected: (Uri?) -> Unit,
    onResetToDefault: () -> Unit,
    onPermissionChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        onFolderSelected(uri)
    }

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onPermissionChanged()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .testTag("storage_access_card"),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasStorageAccess) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            )
                            .border(
                                1.dp,
                                if (hasStorageAccess) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasStorageAccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (hasStorageAccess) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = if (hasStorageAccess) "Storage Access Active" else "Storage Permission Needed",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Download target: $folderDisplayName",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            if (hasStorageAccess) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                            .testTag("change_folder_button"),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick Custom Folder", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                    }

                    if (!customFolderUri.isNullOrEmpty()) {
                        OutlinedButton(
                            onClick = onResetToDefault,
                            modifier = Modifier
                                .height(34.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                .testTag("reset_folder_button"),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "To allow APK cleanups without user prompts on Android 11+, grant All Files Access or choose a folder.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .testTag("grant_all_files_button"),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Grant All Files Access", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
                        }
                    } else {
                        Button(
                            onClick = {
                                legacyPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .testTag("grant_storage_button"),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Grant Storage Permission", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
                        }
                    }

                    OutlinedButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .testTag("select_folder_saf_button"),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Or Select Directory via SAF Picker", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                    }
                }
            }
        }
    }
}
