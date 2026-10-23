package com.arslan.customanimator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.arslan.customanimator.data.ShortcutType

private const val IMAGE_MIME = "image/*"
private const val ANY_MIME = "*/*"

internal enum class ShortcutTile(
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int,
    val needsShizuku: Boolean = false
) {
    APP(Icons.Filled.Apps, R.string.shortcut_tile_app, R.string.shortcut_tile_app_desc),
    ACTIVITY(Icons.AutoMirrored.Filled.Launch, R.string.shortcut_tile_activity, R.string.shortcut_tile_activity_desc),
    APP_SHORTCUT(
        Icons.Filled.TouchApp,
        R.string.shortcut_tile_app_shortcut,
        R.string.shortcut_tile_app_shortcut_desc
    ),
    FILE(Icons.AutoMirrored.Filled.InsertDriveFile, R.string.shortcut_tile_file, R.string.shortcut_tile_file_desc),
    PHOTO(Icons.Filled.Image, R.string.shortcut_tile_photo, R.string.shortcut_tile_photo_desc),
    FOLDER(Icons.Filled.Folder, R.string.shortcut_tile_folder, R.string.shortcut_tile_folder_desc),
    LINK(Icons.Filled.Link, R.string.shortcut_tile_link, R.string.shortcut_tile_link_desc),
    COMMAND(Icons.Filled.Terminal, R.string.shortcut_tile_command, R.string.shortcut_tile_command_desc, needsShizuku = true)
}

@Composable
internal fun rememberShortcutSources(state: ShortcutsState, hasShizukuPermission: Boolean): (ShortcutTile) -> Unit {
    val openSetup = LocalOpenSetupGuide.current
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(state::onFilePicked)
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(state::onFolderPicked)
    }
    return { tile ->
        when (tile) {
            ShortcutTile.APP -> state.startAppPicker(AppPickTarget.APP)
            ShortcutTile.ACTIVITY -> state.startAppPicker(AppPickTarget.ACTIVITY)
            ShortcutTile.APP_SHORTCUT -> state.startAppPicker(AppPickTarget.APP_SHORTCUT)
            ShortcutTile.FILE -> filePicker.launch(arrayOf(ANY_MIME))
            ShortcutTile.PHOTO -> filePicker.launch(arrayOf(IMAGE_MIME))
            ShortcutTile.FOLDER -> folderPicker.launch(null)
            ShortcutTile.LINK -> state.startBlank(ShortcutType.LINK)
            ShortcutTile.COMMAND -> if (hasShizukuPermission) state.startBlank(ShortcutType.COMMAND) else openSetup()
        }
    }
}
