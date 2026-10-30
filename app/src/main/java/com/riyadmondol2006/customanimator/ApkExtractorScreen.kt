package com.riyadmondol2006.customanimator

import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riyadmondol2006.customanimator.data.ApkExtractorApp
import com.riyadmondol2006.customanimator.data.ApkExtractorFormat
import com.riyadmondol2006.customanimator.data.InstalledAppInfo
import com.riyadmondol2006.customanimator.ui.components.AppCard
import com.riyadmondol2006.customanimator.ui.components.IconBadge
import com.riyadmondol2006.customanimator.utils.ApkExtractorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.riyadmondol2006.customanimator.ui.components.ScreenTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkExtractorScreen(
    onBack: () -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()

    val appList = rememberAppListState()
    var isSaving by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<Pair<ApkExtractorApp, ApkExtractorFormat>?>(null) }
    var formatChoiceApp by remember { mutableStateOf<ApkExtractorApp?>(null) }

    val export: (ApkExtractorApp, ApkExtractorFormat) -> Unit = { app, format ->
        isSaving = true
        coroutineScope.launch {
            val folder = withContext(Dispatchers.IO) { ApkExtractorManager.extract(context, app, format) }
            isSaving = false
            val message = if (folder != null) {
                resources.getString(R.string.debloater_export_done, folder)
            } else {
                resources.getString(R.string.apk_extractor_failed)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val pending = pendingExport
        pendingExport = null
        if (pending != null) {
            if (granted) {
                export(pending.first, pending.second)
            } else {
                Toast.makeText(context, resources.getString(R.string.apk_extractor_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }
    val startExport: (ApkExtractorApp, ApkExtractorFormat) -> Unit = { app, format ->
        if (ApkExtractorManager.needsStoragePermission(context)) {
            pendingExport = app to format
            storagePermission.launch(ApkExtractorManager.STORAGE_PERMISSION)
        } else {
            export(app, format)
        }
    }
    val onAppClick: (InstalledAppInfo) -> Unit = { picked ->
        coroutineScope.launch {
            val app = withContext(Dispatchers.IO) { ApkExtractorManager.describe(context, picked) }
            if (app == null) {
                Toast.makeText(context, resources.getString(R.string.apk_extractor_failed), Toast.LENGTH_SHORT).show()
                return@launch
            }
            val formats = ApkExtractorManager.formats(app)
            if (formats.size == 1) startExport(app, formats.single()) else formatChoiceApp = app
        }
    }

    if (isSaving) {
        BusyDialog(stringResource(R.string.apk_extractor_saving))
    }

    formatChoiceApp?.let { app ->
        ApkExtractorFormatSheet(
            app = app,
            onPick = { format ->
                formatChoiceApp = null
                startExport(app, format)
            },
            onDismiss = { formatChoiceApp = null }
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.apk_extractor),
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
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                AppCard(contentPadding = 0.dp) {
                    ApkExportFolderRow()
                }
            }

            item {
                InfoCard(
                    dismissKey = "apk_extractor_info",
                    texts = listOf(stringResource(R.string.apk_extractor_info))
                )
            }

            appList(appList) { app ->
                ApkExtractorRow(app = app, onClick = { onAppClick(app) })
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ApkExtractorRow(app: InstalledAppInfo, onClick: () -> Unit) {
    val context = LocalContext.current
    val details by produceState(ApkExtractorManager.cachedDetails(app), app) {
        if (value == null) value = withContext(Dispatchers.IO) { ApkExtractorManager.describe(context, app) }
    }
    PickerRow(
        title = app.label,
        subtitle = details?.let { apkDetails(it) } ?: app.packageName,
        onClick = onClick
    ) {
        AppIcon(packageName = app.packageName, modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun apkDetails(app: ApkExtractorApp): String {
    val context = LocalContext.current
    val size = remember(app.sizeBytes) { Formatter.formatShortFileSize(context, app.sizeBytes) }
    val detailsRes = if (app.isSplit) R.string.apk_extractor_row_split else R.string.apk_extractor_row
    return stringResource(detailsRes, app.versionName, size)
}

@Composable
private fun ApkExtractorFormatSheet(
    app: ApkExtractorApp,
    onPick: (ApkExtractorFormat) -> Unit,
    onDismiss: () -> Unit
) {
    PickerSheet(
        title = app.label,
        subtitle = stringResource(R.string.apk_extractor_format_hint),
        onDismiss = onDismiss
    ) {
        items(ApkExtractorManager.formats(app)) { format ->
            val option = formatOption(format)
            PickerRow(
                title = stringResource(option.titleRes),
                subtitle = stringResource(option.descriptionRes),
                onClick = { onPick(format) }
            ) {
                IconBadge(icon = option.icon, size = 40.dp)
            }
        }
    }
}

private class ApkExtractorFormatOption(val icon: ImageVector, val titleRes: Int, val descriptionRes: Int)

private fun formatOption(format: ApkExtractorFormat) = when (format) {
    ApkExtractorFormat.BUNDLE -> ApkExtractorFormatOption(
        Icons.Filled.Inventory2,
        R.string.apk_extractor_format_bundle,
        R.string.apk_extractor_format_bundle_desc
    )
    ApkExtractorFormat.BASE -> ApkExtractorFormatOption(
        Icons.Filled.Android,
        R.string.apk_extractor_format_base,
        R.string.apk_extractor_format_base_desc
    )
    ApkExtractorFormat.SEPARATE -> ApkExtractorFormatOption(
        Icons.Filled.Folder,
        R.string.apk_extractor_format_separate,
        R.string.apk_extractor_format_separate_desc
    )
}
