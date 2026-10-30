package com.riyadmondol2006.customanimator

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.riyadmondol2006.customanimator.utils.ApkExtractorManager
import com.riyadmondol2006.customanimator.utils.SaveToDeviceManager
import androidx.compose.material3.ButtonDefaults

@Composable
internal fun SaveToDeviceRow() {
    FolderChoiceRow(
        icon = Icons.Filled.SaveAlt,
        title = stringResource(R.string.save_to_device),
        explanation = stringResource(R.string.save_to_device_desc),
        unsetLabel = stringResource(R.string.save_to_device_ask),
        resetLabel = stringResource(R.string.save_to_device_ask),
        read = SaveToDeviceManager::getDefaultFolder,
        write = SaveToDeviceManager::setDefaultFolder
    )
}

@Composable
internal fun ApkExportFolderRow() {
    FolderChoiceRow(
        icon = Icons.Filled.Folder,
        title = stringResource(R.string.save_to_device_choose_folder),
        explanation = stringResource(R.string.apk_extractor_folder_desc),
        unsetLabel = ApkExtractorManager.DEFAULT_FOLDER,
        resetLabel = stringResource(R.string.apk_extractor_folder_default, ApkExtractorManager.DEFAULT_FOLDER),
        read = ApkExtractorManager::exportTree,
        write = ApkExtractorManager::setExportTree
    )
}

@Composable
private fun FolderChoiceRow(
    icon: ImageVector,
    title: String,
    explanation: String,
    unsetLabel: String,
    resetLabel: String,
    read: (Context) -> Uri?,
    write: (Context, Uri?) -> Unit
) {
    val context = LocalContext.current
    var folder by remember { mutableStateOf(read(context)) }
    var showDialog by remember { mutableStateOf(false) }
    val location = folder?.let(SaveToDeviceManager::folderLabel) ?: unsetLabel

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { picked ->
        if (picked != null) {
            write(context, picked)
            folder = picked
        }
    }

    NavigationRow(
        icon = icon,
        title = title,
        description = stringResource(R.string.save_to_device_location, location),
        onClick = { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = { Text(explanation) },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        folderPicker.launch(folder)
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.save_to_device_choose_folder))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        write(context, null)
                        folder = null
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(resetLabel)
                }
            }
        )
    }
}
