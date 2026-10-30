package com.riyadmondol2006.customanimator

import android.content.res.Resources
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riyadmondol2006.customanimator.data.ApkInstallerPackage
import com.riyadmondol2006.customanimator.ui.components.AppCard
import com.riyadmondol2006.customanimator.ui.components.IconBadge
import com.riyadmondol2006.customanimator.ui.components.ScreenTopBar
import com.riyadmondol2006.customanimator.utils.ApkInstallerManager
import com.riyadmondol2006.customanimator.utils.LocalShizukuPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ANY_FILE = "*/*"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkInstallerScreen(
    onBack: () -> Unit,
    hasShizukuPermission: Boolean = LocalShizukuPermission.current,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val openSetup = LocalOpenSetupGuide.current
    val coroutineScope = rememberCoroutineScope()

    var selected by remember { mutableStateOf<ApkInstallerPackage?>(null) }
    var options by remember { mutableStateOf(ApkInstallerManager.enabledOptions(context)) }
    var busyMessageRes by remember { mutableStateOf<Int?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        busyMessageRes = R.string.apk_installer_reading
        coroutineScope.launch {
            val loaded = withContext(Dispatchers.IO) { ApkInstallerManager.load(context, uris) }
            busyMessageRes = null
            selected = loaded
            if (loaded == null) {
                Toast.makeText(context, resources.getString(R.string.apk_installer_invalid), Toast.LENGTH_LONG).show()
            }
        }
    }
    val choose: () -> Unit = { picker.launch(arrayOf(ANY_FILE)) }

    val install: (ApkInstallerPackage) -> Unit = { app ->
        busyMessageRes = R.string.icon_changer_step_installing
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO + NonCancellable) {
                ApkInstallerManager.install(context, app, options)
            }
            busyMessageRes = null
            Toast.makeText(context, result.message(resources, app), Toast.LENGTH_LONG).show()
            if (result.outcome != ApkInstallerManager.Outcome.DONE) return@launch
            selected = null
            withContext(Dispatchers.IO) { ApkInstallerManager.discard(context) }
        }
    }

    busyMessageRes?.let { BusyDialog(stringResource(it)) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.apk_installer),
                onBack = onBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!hasShizukuPermission) {
                item {
                    SetupNudgeCard(
                        message = stringResource(R.string.developer_needs_shizuku),
                        onOpenSetup = openSetup
                    )
                }
            }

            item {
                InfoCard(
                    dismissKey = "apk_installer_info",
                    texts = listOf(stringResource(R.string.apk_installer_info))
                )
            }

            item {
                val app = selected
                if (app == null) {
                    Button(onClick = choose, modifier = Modifier.fillMaxWidth(), shapes = ButtonDefaults.shapes()) {
                        Text(stringResource(R.string.apk_installer_choose))
                    }
                } else {
                    AppCard(contentPadding = 0.dp) {
                        ApkInstallerPackageRow(app = app, onClick = choose)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            sectionTitle(R.string.apk_installer_options)

            cardRows(
                *ApkInstallerManager.options().map { option ->
                    @Composable {
                        val text = optionText(option)
                        ToggleRow(
                            icon = text.icon,
                            title = stringResource(text.titleRes),
                            description = stringResource(text.descriptionRes),
                            checked = option in options,
                            onCheckedChange = { enabled ->
                                options = if (enabled) options + option else options - option
                                ApkInstallerManager.setOption(context, option, enabled)
                            }
                        )
                    }
                }.toTypedArray()
            )

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { selected?.let(install) },
                    enabled = hasShizukuPermission && selected != null && busyMessageRes == null,
                    modifier = Modifier.fillMaxWidth(),
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(stringResource(R.string.apk_installer_install))
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ApkInstallerPackageRow(app: ApkInstallerPackage, onClick: () -> Unit) {
    val context = LocalContext.current
    val size = remember(app.sizeBytes) { Formatter.formatShortFileSize(context, app.sizeBytes) }
    val detailsRes = if (app.isSplit) R.string.apk_extractor_row_split else R.string.apk_extractor_row
    val lines = listOfNotNull(
        app.packageName,
        stringResource(detailsRes, app.versionName, size),
        app.installedVersion?.let { stringResource(R.string.apk_installer_installed_version, it) }
    )
    PickerRow(title = app.label, subtitle = lines.joinToString("\n"), onClick = onClick) {
        val icon = app.icon
        if (icon == null) {
            IconBadge(icon = Icons.Filled.Android, size = 40.dp)
        } else {
            val bitmap = remember(icon) { icon.asImageBitmap() }
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(40.dp))
        }
    }
}

private class ApkInstallerOptionText(val icon: ImageVector, val titleRes: Int, val descriptionRes: Int)

private fun optionText(option: ApkInstallerManager.Option) = when (option) {
    ApkInstallerManager.Option.GRANT_PERMISSIONS -> ApkInstallerOptionText(
        Icons.Filled.VerifiedUser,
        R.string.apk_installer_grant,
        R.string.apk_installer_grant_desc
    )
    ApkInstallerManager.Option.ALLOW_DOWNGRADE -> ApkInstallerOptionText(
        Icons.Filled.SettingsBackupRestore,
        R.string.apk_installer_downgrade,
        R.string.apk_installer_downgrade_desc
    )
    ApkInstallerManager.Option.PLAY_STORE_INSTALLER -> ApkInstallerOptionText(
        Icons.Filled.Shop,
        R.string.apk_installer_play_store,
        R.string.apk_installer_play_store_desc
    )
    ApkInstallerManager.Option.ALLOW_LOW_TARGET_SDK -> ApkInstallerOptionText(
        Icons.Filled.History,
        R.string.apk_installer_low_sdk,
        R.string.apk_installer_low_sdk_desc
    )
}

private fun ApkInstallerManager.Result.message(resources: Resources, app: ApkInstallerPackage): String = when {
    outcome == ApkInstallerManager.Outcome.DONE -> resources.getString(R.string.apk_installer_done, app.label)
    outcome == ApkInstallerManager.Outcome.NO_SHIZUKU -> resources.getString(R.string.icon_changer_no_shizuku)
    detail.isBlank() -> resources.getString(R.string.icon_changer_install_blocked)
    else -> resources.getString(R.string.apk_installer_failed, detail)
}
