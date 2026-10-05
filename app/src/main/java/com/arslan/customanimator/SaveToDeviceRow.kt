package com.arslan.customanimator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.arslan.customanimator.utils.SaveToDeviceManager

@Composable
internal fun SaveToDeviceRow() {
    val context = LocalContext.current
    var folder by remember { mutableStateOf(SaveToDeviceManager.getDefaultFolder(context)) }
    var showDialog by remember { mutableStateOf(false) }
    val location = folder?.let(SaveToDeviceManager::folderLabel) ?: stringResource(R.string.save_to_device_ask)

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { picked ->
        if (picked != null) {
            SaveToDeviceManager.setDefaultFolder(context, picked)
            folder = picked
        }
    }

    NavigationRow(
        icon = Icons.Filled.SaveAlt,
        title = stringResource(R.string.save_to_device),
        description = stringResource(R.string.save_to_device_location, location),
        onClick = { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.save_to_device)) },
            text = { Text(stringResource(R.string.save_to_device_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        folderPicker.launch(folder)
                    }
                ) {
                    Text(stringResource(R.string.save_to_device_choose_folder))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        SaveToDeviceManager.setDefaultFolder(context, null)
                        folder = null
                    }
                ) {
                    Text(stringResource(R.string.save_to_device_ask))
                }
            }
        )
    }
}
