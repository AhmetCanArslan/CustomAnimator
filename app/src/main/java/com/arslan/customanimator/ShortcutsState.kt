package com.arslan.customanimator

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arslan.customanimator.data.AppShortcutInfo
import com.arslan.customanimator.data.InstalledAppInfo
import com.arslan.customanimator.data.ShortcutActivityInfo
import com.arslan.customanimator.data.ShortcutEntry
import com.arslan.customanimator.data.ShortcutHandlerInfo
import com.arslan.customanimator.data.ShortcutIconImage
import com.arslan.customanimator.data.ShortcutType
import com.arslan.customanimator.utils.AppShortcutManager
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.ShortcutMaker
import com.arslan.customanimator.utils.ShortcutStore
import com.arslan.customanimator.widget.ShortcutWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

internal enum class AppPickTarget { APP, ACTIVITY, APP_SHORTCUT }

internal sealed interface ShortcutStep {
    data object Idle : ShortcutStep
    data object PickScreen : ShortcutStep
    data class PickAction(val withSources: Boolean) : ShortcutStep
    data class EnterValue(val entry: ShortcutEntry) : ShortcutStep
    data class PickApp(val target: AppPickTarget) : ShortcutStep
    data class PickActivity(val app: InstalledAppInfo, val activities: List<ShortcutActivityInfo>) : ShortcutStep
    data class PickAppShortcut(
        val app: InstalledAppInfo,
        val shortcuts: List<AppShortcutInfo>,
        val icons: Map<String, ShortcutIconImage>
    ) : ShortcutStep
    data class PickHandler(
        val entry: ShortcutEntry,
        val handlers: List<ShortcutHandlerInfo>,
        val defaultHandler: ComponentName?,
        val editing: Boolean
    ) : ShortcutStep
    data class Confirm(val entry: ShortcutEntry, val icon: Bitmap, val editing: Boolean = false) : ShortcutStep
}

internal class ShortcutsState(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onPicked: ((ShortcutEntry) -> Unit)? = null
) {
    val canAddWidget = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    var current by mutableStateOf<ShortcutEntry?>(null)
        private set

    var shortcuts by mutableStateOf<List<ShortcutEntry>>(emptyList())
        private set
    var icons by mutableStateOf<Map<String, Bitmap>>(emptyMap())
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

    fun startAppPicker(target: AppPickTarget) {
        step = ShortcutStep.PickApp(target)
    }

    fun startScreenPicker() {
        step = ShortcutStep.PickScreen
    }

    fun startActionPicker(withSources: Boolean, selected: ShortcutEntry? = null) {
        current = selected
        step = ShortcutStep.PickAction(withSources)
    }

    fun startBlank(type: ShortcutType) {
        presentWithGlyph(ShortcutEntry(id = newId(), type = type, label = ""))
    }

    fun onScreenPicked(screen: SearchEntry) {
        presentWithGlyph(
            ShortcutEntry(
                id = newId(),
                type = ShortcutType.APP_SCREEN,
                label = context.getString(screen.titleRes),
                screen = screen.target.key
            )
        )
    }

    fun onActionPicked(entry: ShortcutEntry) {
        presentWithGlyph(entry)
    }

    fun onValueEntered(entry: ShortcutEntry) {
        step = ShortcutStep.Idle
        onPicked?.invoke(entry)
    }

    private fun presentWithGlyph(entry: ShortcutEntry) {
        present(entry) { ShortcutMaker.glyphIcon(context, entry) }
    }

    private fun present(entry: ShortcutEntry, icon: () -> Bitmap) {
        val pick = onPicked
        when {
            pick == null -> scope.launch {
                step = ShortcutStep.Confirm(entry, withContext(Dispatchers.IO) { icon() })
            }
            entry.valueLabelRes() != null -> step = ShortcutStep.EnterValue(entry)
            else -> {
                step = ShortcutStep.Idle
                pick(entry)
            }
        }
    }

    fun onAppPicked(app: InstalledAppInfo) {
        val picking = step as? ShortcutStep.PickApp ?: return
        when (picking.target) {
            AppPickTarget.APP -> presentApp(app)
            AppPickTarget.ACTIVITY -> scope.launch {
                step = withContext(Dispatchers.IO) {
                    ShortcutStep.PickActivity(app, ShortcutMaker.listActivities(context, app.packageName))
                }
            }
            AppPickTarget.APP_SHORTCUT -> scope.launch {
                step = withContext(Dispatchers.IO) { appShortcutStep(app) }
            }
        }
    }

    fun onAppShortcutPicked(app: InstalledAppInfo, shortcut: AppShortcutInfo, icon: ShortcutIconImage?) {
        val entry = ShortcutEntry(
            id = newId(),
            type = ShortcutType.APP_SHORTCUT,
            label = shortcut.label,
            packageName = app.packageName,
            shortcutId = shortcut.id,
            viaShizuku = shortcut.needsShizuku,
            legacyIcon = icon?.legacy ?: false
        )
        present(entry) { icon?.bitmap ?: iconOf(ShortcutMaker.appIcon(context, app.packageName), entry) }
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
        present(entry) { iconOf(ShortcutMaker.activityIcon(context, app.packageName, activity.name), entry) }
    }

    fun onFilePicked(uri: Uri) {
        scope.launch {
            val picked = withContext(Dispatchers.IO) { fileEntry(uri)?.let { handlerStepFor(it, editing = false) } }
            if (picked == null) {
                toast(R.string.shortcut_file_not_persistable)
            } else {
                step = picked
            }
        }
    }

    fun onFolderPicked(treeUri: Uri) {
        scope.launch {
            step = withContext(Dispatchers.IO) { handlerStepFor(folderEntry(treeUri), editing = false) }
        }
    }

    fun onHandlerPicked(handler: ShortcutHandlerInfo?, remember: Boolean) {
        val picking = step as? ShortcutStep.PickHandler ?: return
        val entry = picking.entry.copy(packageName = handler?.packageName, activityName = handler?.activityName)
        scope.launch {
            if (remember) {
                withContext(Dispatchers.IO) {
                    ShortcutStore.setDefaultHandler(context, entry.mimeType, handler?.component())
                }
            }
            if (picking.editing) {
                withContext(Dispatchers.IO) { ShortcutStore.update(context, entry) }
                refresh()
                step = ShortcutStep.Idle
            } else {
                present(entry) { ShortcutMaker.fileIcon(context, entry, handler?.icon) }
            }
        }
    }

    fun edit(entry: ShortcutEntry) {
        val icon = icons[entry.id] ?: return
        step = ShortcutStep.Confirm(entry, icon, editing = true)
    }

    fun confirm(entry: ShortcutEntry, icon: Bitmap, asWidget: Boolean) {
        val current = step as? ShortcutStep.Confirm ?: return
        step = ShortcutStep.Idle
        scope.launch {
            val placed = withContext(Dispatchers.IO) {
                ShortcutStore.save(context, entry, icon)
                if (current.editing) {
                    ShortcutMaker.updatePinned(context, entry, icon)
                    updateWidgets(entry.id)
                    true
                } else {
                    place(entry, icon, asWidget)
                }
            }
            refresh()
            if (!placed) toast(if (asWidget) R.string.shortcut_widget_unsupported else R.string.shortcut_pin_unsupported)
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

    fun addWidget(entry: ShortcutEntry) {
        scope.launch {
            val requested = withContext(Dispatchers.IO) { pinWidget(entry.id) }
            if (!requested) toast(R.string.shortcut_widget_unsupported)
        }
    }

    private fun place(entry: ShortcutEntry, icon: Bitmap, asWidget: Boolean): Boolean =
        if (asWidget) pinWidget(entry.id) else ShortcutMaker.pin(context, entry, icon)

    private fun pinWidget(shortcutId: String): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && ShortcutWidgetProvider.requestPin(context, shortcutId)

    private fun updateWidgets(shortcutId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ShortcutWidgetProvider.update(context, shortcutId)
    }

    fun changeHandler(entry: ShortcutEntry) {
        scope.launch {
            step = withContext(Dispatchers.IO) { handlerStepFor(entry, editing = true) }
        }
    }

    fun delete(entry: ShortcutEntry) {
        shortcuts = shortcuts.filterNot { it.id == entry.id }
        scope.launch {
            withContext(Dispatchers.IO) {
                ShortcutMaker.remove(context, entry.id)
                updateWidgets(entry.id)
            }
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

    private fun presentApp(app: InstalledAppInfo) {
        val entry = ShortcutEntry(
            id = newId(),
            type = ShortcutType.APP,
            label = app.label,
            packageName = app.packageName
        )
        present(entry) { iconOf(ShortcutMaker.appIcon(context, app.packageName), entry) }
    }

    private suspend fun appShortcutStep(app: InstalledAppInfo): ShortcutStep {
        val shortcuts = AppShortcutManager.listShortcuts(context, app.packageName)
        val icons = shortcuts.mapNotNull { shortcut ->
            AppShortcutManager.loadIcon(context, app.packageName, shortcut)?.let { shortcut.id to it }
        }.toMap()
        return ShortcutStep.PickAppShortcut(app, shortcuts, icons)
    }

    private fun fileEntry(uri: Uri): ShortcutEntry? {
        try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            return null
        }
        val file = ShortcutMaker.describeFile(context, uri)
        return ShortcutEntry(
            id = newId(),
            type = ShortcutType.FILE,
            label = file.name,
            uri = uri.toString(),
            mimeType = file.mimeType
        )
    }

    private fun folderEntry(treeUri: Uri): ShortcutEntry {
        val folder = ShortcutMaker.describeFolder(context, treeUri)
        return ShortcutEntry(
            id = newId(),
            type = ShortcutType.FOLDER,
            label = folder.name,
            uri = ShortcutMaker.folderUri(treeUri).toString(),
            mimeType = folder.mimeType
        )
    }

    private fun handlerStepFor(entry: ShortcutEntry, editing: Boolean): ShortcutStep.PickHandler =
        ShortcutStep.PickHandler(
            entry = entry,
            handlers = ShortcutMaker.listHandlers(context, entry),
            defaultHandler = ShortcutStore.getDefaultHandler(context, entry.mimeType),
            editing = editing
        )

    private fun iconOf(drawable: Drawable?, entry: ShortcutEntry): Bitmap =
        drawable?.let(ShortcutMaker::drawableIcon) ?: ShortcutMaker.glyphIcon(context, entry)

    private fun newId(): String = UUID.randomUUID().toString()

    private fun toast(messageRes: Int) {
        Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
    }
}

internal fun ShortcutHandlerInfo.component(): ComponentName = ComponentName(packageName, activityName)
