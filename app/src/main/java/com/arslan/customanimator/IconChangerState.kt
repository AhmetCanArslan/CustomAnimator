package com.arslan.customanimator

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arslan.customanimator.data.InstalledAppInfo
import com.arslan.customanimator.data.ShortcutIconImage
import com.arslan.customanimator.utils.IconChangerManager
import com.arslan.customanimator.utils.ShortcutMaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class IconChangerSheet { NONE, ICONS }

internal enum class IconChangerProgress { REBUILDING, INSTALLING }

internal class IconChangerConfirmation(
    val app: InstalledAppInfo,
    val keepsData: Boolean,
    val erasureAcknowledged: Boolean = false
) {
    val needsErasureCheck: Boolean get() = !keepsData && !erasureAcknowledged
}

internal class IconChangerState(private val context: Context, private val scope: CoroutineScope) {
    var installs by mutableIntStateOf(0)
        private set
    var app by mutableStateOf<InstalledAppInfo?>(null)
        private set
    var image by mutableStateOf<ShortcutIconImage?>(null)
        private set
    var imageChanged by mutableStateOf(false)
        private set
    var background by mutableIntStateOf(Color.WHITE)
    var scale by mutableFloatStateOf(IconChangerManager.MAX_SCALE)
    var sheet by mutableStateOf(IconChangerSheet.NONE)
    var confirmation by mutableStateOf<IconChangerConfirmation?>(null)
        private set
    var progress by mutableStateOf<IconChangerProgress?>(null)
        private set

    val canApply: Boolean get() = app != null && imageChanged && progress == null

    fun clearApp() {
        sheet = IconChangerSheet.NONE
        app = null
        imageChanged = false
    }

    fun onAppPicked(picked: InstalledAppInfo) {
        sheet = IconChangerSheet.NONE
        scope.launch {
            val icon = withContext(Dispatchers.IO) {
                ShortcutMaker.appIcon(context, picked.packageName)?.let(ShortcutMaker::drawableIcon)
            }
            app = picked
            image = icon?.let(::ShortcutIconImage)
            imageChanged = false
        }
    }

    fun onIconPicked(picked: ShortcutIconImage) {
        sheet = IconChangerSheet.NONE
        image = picked
        imageChanged = true
    }

    fun onImagePicked(uri: Uri) {
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) { ShortcutMaker.thumbnailIcon(context, uri) }
            if (bitmap == null) {
                toast(context.getString(R.string.icon_changer_image_failed))
            } else {
                onIconPicked(ShortcutIconImage(bitmap, legacy = true))
            }
        }
    }

    fun askToApply() {
        val target = app ?: return
        scope.launch {
            val blocker = withContext(Dispatchers.IO) { IconChangerManager.installBlocker(context, target.packageName) }
            if (blocker != null) {
                toast(context.getString(blocker.messageRes()))
                return@launch
            }
            val keepsData = withContext(Dispatchers.IO) { IconChangerManager.keepsData(context, target.packageName) }
            confirmation = IconChangerConfirmation(target, keepsData)
        }
    }

    fun confirm() {
        val current = confirmation ?: return
        if (current.needsErasureCheck) {
            confirmation = IconChangerConfirmation(current.app, current.keepsData, erasureAcknowledged = true)
        } else {
            apply()
        }
    }

    fun dismissConfirmation() {
        confirmation = null
    }

    private fun apply() {
        val target = confirmation?.app ?: return
        val picked = image ?: return
        confirmation = null
        progress = IconChangerProgress.REBUILDING
        scope.launch {
            val outcome = withContext(Dispatchers.IO + NonCancellable) {
                IconChangerManager.changeIcon(
                    context,
                    target.packageName,
                    IconChangerManager.foregroundLayer(picked, scale),
                    background
                ) { step -> progress = step.progress() }
            }
            progress = null
            toast(context.getString(outcome.messageRes()))
            if (outcome != IconChangerManager.Outcome.DONE) return@launch
            maybeShowInterstitial(context)
            clearApp()
            installs++
        }
    }

    fun export() {
        val target = app ?: return
        val picked = image ?: return
        progress = IconChangerProgress.REBUILDING
        scope.launch {
            val path = withContext(Dispatchers.IO) {
                IconChangerManager.exportApk(
                    context,
                    target.packageName,
                    IconChangerManager.foregroundLayer(picked, scale),
                    background
                )
            }
            progress = null
            if (path == null) {
                toast(context.getString(R.string.debloater_export_failed))
            } else {
                toast(context.getString(R.string.debloater_export_done, path))
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

private fun IconChangerManager.Step.progress(): IconChangerProgress = when (this) {
    IconChangerManager.Step.REBUILDING -> IconChangerProgress.REBUILDING
    IconChangerManager.Step.INSTALLING -> IconChangerProgress.INSTALLING
}

private fun IconChangerManager.Outcome.messageRes(): Int = when (this) {
    IconChangerManager.Outcome.DONE -> R.string.icon_changer_done
    IconChangerManager.Outcome.BUILD_FAILED -> R.string.icon_changer_build_failed
    IconChangerManager.Outcome.NO_SHIZUKU -> R.string.icon_changer_no_shizuku
    IconChangerManager.Outcome.INSTALL_BLOCKED -> R.string.icon_changer_install_blocked
    IconChangerManager.Outcome.ORIGINAL_RESTORED -> R.string.icon_changer_restored
    IconChangerManager.Outcome.APP_LOST -> R.string.icon_changer_lost
}
