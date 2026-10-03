package com.arslan.customanimator

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arslan.customanimator.data.InstalledAppInfo
import com.arslan.customanimator.data.ShortcutActivityInfo
import com.arslan.customanimator.data.ShortcutEntry
import com.arslan.customanimator.data.ShortcutHandlerInfo
import com.arslan.customanimator.data.ShortcutType
import com.arslan.customanimator.utils.InstalledAppsProvider
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.ShortcutMaker
import com.arslan.customanimator.utils.ShortcutStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

internal sealed interface ShortcutStep {
    data object Idle : ShortcutStep
    data class PickApp(val forActivity: Boolean) : ShortcutStep
    data class PickActivity(val app: InstalledAppInfo, val activities: List<ShortcutActivityInfo>) : ShortcutStep
    data class PickHandler(
        val entry: ShortcutEntry,
        val handlers: List<ShortcutHandlerInfo>,
        val defaultHandler: ComponentName?,
        val editing: Boolean
    ) : ShortcutStep
    data class Confirm(val entry: ShortcutEntry, val icon: Bitmap) : ShortcutStep
}

internal class ShortcutsState(private val context: Context, private val scope: CoroutineScope) {
    var shortcuts by mutableStateOf<List<ShortcutEntry>>(emptyList())
        private set
    var icons by mutableStateOf<Map<String, Bitmap>>(emptyMap())
        private set
    var apps by mutableStateOf<List<InstalledAppInfo>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isRoot by mutableStateOf(false)
        private set
    var step by mutableStateOf<ShortcutStep>(ShortcutStep.Idle)
        private set

    suspend fun load(hasShizukuPermission: Boolean) {
        val root = withContext(Dispatchers.IO) { hasShizukuPermission && ShizukuHelper.isRunningAsRoot() }
        refresh()
        isRoot = root
        isLoading = false
    }

    fun dismiss() {
        step = ShortcutStep.Idle
    }

    fun startAppPicker(forActivity: Boolean) {
        step = ShortcutStep.PickApp(forActivity)
        if (apps.isNotEmpty()) return
        scope.launch {
            apps = withContext(Dispatchers.IO) { InstalledAppsProvider.getLaunchableApps(context) }
        }
    }

    fun startBlank(type: ShortcutType) {
        scope.launch {
            val icon = withContext(Dispatchers.IO) { ShortcutMaker.glyphIcon(context, type) }
            step = ShortcutStep.Confirm(ShortcutEntry(id = newId(), type = type, label = ""), icon)
        }
    }

    fun onAppPicked(app: InstalledAppInfo) {
        val current = step as? ShortcutStep.PickApp ?: return
        scope.launch {
            step = if (current.forActivity) {
                ShortcutStep.PickActivity(
                    app,
                    withContext(Dispatchers.IO) { ShortcutMaker.listActivities(context, app.packageName) }
                )
            } else {
                val entry = ShortcutEntry(
                    id = newId(),
                    type = ShortcutType.APP,
                    label = app.label,
                    packageName = app.packageName
                )
                ShortcutStep.Confirm(entry, withContext(Dispatchers.IO) { iconOf(app.icon, ShortcutType.APP) })
            }
        }
    }

    fun onActivityPicked(app: InstalledAppInfo, activity: ShortcutActivityInfo) {
        val entry = ShortcutEntry(
            id = newId(),
            type = ShortcutType.ACTIVITY,
            label = activity.label,
            packageName = app.packageName,
            activityName = activity.name,
            viaShizuku = activity.needsShizuku
        )
        scope.launch {
            val icon = withContext(Dispatchers.IO) {
                iconOf(ShortcutMaker.activityIcon(context, app.packageName, activity.name), ShortcutType.ACTIVITY)
            }
            step = ShortcutStep.Confirm(entry, icon)
        }
    }

    fun onFilePicked(uri: Uri) {
        scope.launch {
            val picked = withContext(Dispatchers.IO) { handlerStepFor(uri) }
            if (picked == null) {
                toast(R.string.shortcut_file_not_persistable)
            } else {
                step = picked
            }
        }
    }

    fun onHandlerPicked(handler: ShortcutHandlerInfo?, remember: Boolean) {
        val current = step as? ShortcutStep.PickHandler ?: return
        val entry = current.entry.copy(packageName = handler?.packageName, activityName = handler?.activityName)
        scope.launch {
            if (remember) {
                withContext(Dispatchers.IO) {
                    ShortcutStore.setDefaultHandler(context, entry.mimeType, handler?.component())
                }
            }
            if (current.editing) {
                withContext(Dispatchers.IO) { ShortcutStore.update(context, entry) }
                refresh()
                step = ShortcutStep.Idle
            } else {
                val icon = withContext(Dispatchers.IO) { ShortcutMaker.fileIcon(context, entry, handler?.icon) }
                step = ShortcutStep.Confirm(entry, icon)
            }
        }
    }

    fun confirm(entry: ShortcutEntry) {
        val icon = (step as? ShortcutStep.Confirm)?.icon ?: return
        step = ShortcutStep.Idle
        scope.launch {
            val pinned = withContext(Dispatchers.IO) {
                ShortcutStore.save(context, entry, icon)
                ShortcutMaker.pin(context, entry, icon)
            }
            refresh()
            if (!pinned) toast(R.string.shortcut_pin_unsupported)
        }
    }

    fun launch(entry: ShortcutEntry) {
        scope.launch {
            val launched = withContext(Dispatchers.IO) { ShortcutMaker.launch(context, entry) }
            if (!launched) toast(ShortcutMaker.failureMessageRes(entry))
        }
    }

    fun pinAgain(entry: ShortcutEntry) {
        val icon = icons[entry.id] ?: return
        scope.launch {
            val pinned = withContext(Dispatchers.IO) { ShortcutMaker.pin(context, entry, icon) }
            if (!pinned) toast(R.string.shortcut_pin_unsupported)
        }
    }

    fun changeHandler(entry: ShortcutEntry) {
        scope.launch {
            step = withContext(Dispatchers.IO) {
                ShortcutStep.PickHandler(
                    entry = entry,
                    handlers = ShortcutMaker.listHandlers(context, entry),
                    defaultHandler = ShortcutStore.getDefaultHandler(context, entry.mimeType),
                    editing = true
                )
            }
        }
    }

    fun delete(entry: ShortcutEntry) {
        shortcuts = shortcuts.filterNot { it.id == entry.id }
        scope.launch {
            withContext(Dispatchers.IO) { ShortcutMaker.remove(context, entry.id) }
        }
    }

    private suspend fun refresh() {
        val (loaded, loadedIcons) = withContext(Dispatchers.IO) {
            val entries = ShortcutStore.getAll(context)
            entries to entries.mapNotNull { entry ->
                ShortcutStore.loadIcon(context, entry.id)?.let { entry.id to it }
            }.toMap()
        }
        shortcuts = loaded
        icons = loadedIcons
    }

    private fun handlerStepFor(uri: Uri): ShortcutStep.PickHandler? {
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            return null
        }
        val file = ShortcutMaker.describeFile(context, uri)
        val entry = ShortcutEntry(
            id = newId(),
            type = ShortcutType.FILE,
            label = file.name,
            uri = uri.toString(),
            mimeType = file.mimeType
        )
        return ShortcutStep.PickHandler(
            entry = entry,
            handlers = ShortcutMaker.listHandlers(context, entry),
            defaultHandler = ShortcutStore.getDefaultHandler(context, file.mimeType),
            editing = false
        )
    }

    private fun iconOf(drawable: Drawable?, type: ShortcutType): Bitmap =
        drawable?.let(ShortcutMaker::drawableIcon) ?: ShortcutMaker.glyphIcon(context, type)

    private fun newId(): String = UUID.randomUUID().toString()

    private fun toast(messageRes: Int) {
        Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
    }
}

internal fun ShortcutHandlerInfo.component(): ComponentName = ComponentName(packageName, activityName)
