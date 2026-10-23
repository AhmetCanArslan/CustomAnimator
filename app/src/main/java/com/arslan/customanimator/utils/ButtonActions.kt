package com.arslan.customanimator.utils

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.view.KeyEvent
import androidx.annotation.StringRes
import com.arslan.customanimator.R

data class ButtonAction(val type: Type, val value: String) {
    enum class Type { QUICK, TOGGLE, APP, COMMAND }
}

object ButtonActions {

    enum class Quick(val key: String, @StringRes val nameRes: Int, val run: (Context) -> Boolean) {
        FLASHLIGHT("flashlight", R.string.button_action_flashlight, { TorchToggle.toggle(it) }),
        SCREENSHOT("screenshot", R.string.button_action_screenshot, { sendKey(KeyEvent.KEYCODE_SYSRQ) }),
        PLAY_PAUSE(
            "play_pause", R.string.button_action_play_pause,
            { sendMediaKey(it, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }
        ),
        NEXT_TRACK(
            "next_track", R.string.button_action_next_track,
            { sendMediaKey(it, KeyEvent.KEYCODE_MEDIA_NEXT) }
        ),
        PREVIOUS_TRACK(
            "previous_track", R.string.button_action_previous_track,
            { sendMediaKey(it, KeyEvent.KEYCODE_MEDIA_PREVIOUS) }
        ),
        VOLUME_PANEL(
            "volume_panel", R.string.sound_tile_action_volume_panel,
            { SoundTileActions.showVolumePanel(it) }
        ),
        NOTIFICATIONS(
            "notifications", R.string.button_action_notifications,
            { ShizukuHelper.executeShellCommand(arrayOf("cmd", "statusbar", "expand-notifications")) }
        ),
        QUICK_SETTINGS(
            "quick_settings", R.string.button_action_quick_settings,
            { ShizukuHelper.executeShellCommand(arrayOf("cmd", "statusbar", "expand-settings")) }
        ),
        RECENT_APPS("recent_apps", R.string.button_action_recent_apps, { sendKey(KeyEvent.KEYCODE_APP_SWITCH) }),
        FREE_MEMORY("free_memory", R.string.button_action_free_memory, { MemoryBooster.boost() }),
        RESTART_SYSTEM_UI(
            "restart_system_ui", R.string.restart_system_ui,
            { DeveloperOptionsManager.restartSystemUi() }
        );

        companion object {
            fun fromKey(key: String?): Quick? = entries.firstOrNull { it.key == key }
        }
    }

    fun perform(context: Context, action: ButtonAction): Boolean = when (action.type) {
        ButtonAction.Type.QUICK -> runQuick(context, action.value)
        ButtonAction.Type.TOGGLE -> toggle(action.value)
        ButtonAction.Type.APP -> launchApp(context, action.value)
        ButtonAction.Type.COMMAND -> runShell(action.value)
    }

    fun performInBackground(context: Context, action: ButtonAction): Boolean =
        if (action.type == ButtonAction.Type.APP) launchAppFromShell(context, action.value) else perform(context, action)

    fun label(context: Context, action: ButtonAction): String = when (action.type) {
        ButtonAction.Type.QUICK -> Quick.fromKey(action.value)?.let { context.getString(it.nameRes) }
        ButtonAction.Type.TOGGLE -> ToggleTilePresets.byKey(action.value)?.let { context.getString(it.nameRes) }
        ButtonAction.Type.APP -> appLabel(context, action.value)
        ButtonAction.Type.COMMAND -> action.value
    } ?: action.value

    fun runQuick(context: Context, key: String?): Boolean = Quick.fromKey(key)?.run?.invoke(context) ?: false

    fun toggle(key: String?): Boolean {
        val preset = key?.let(ToggleTilePresets::byKey) ?: return false
        val state = ShizukuHelper.executeShellCommandWithOutput(arrayOf("sh", "-c", preset.readCommand))
        if (!state.isSuccess) return false
        val active = state.output.trim().lines().firstOrNull()?.trim() == preset.onValue.trim()
        return runShell(if (active) preset.offCommand else preset.onCommand)
    }

    private fun runShell(command: String): Boolean =
        command.isNotBlank() &&
            ShizukuHelper.executeShellCommandWithOutput(arrayOf("sh", "-c", command)).isSuccess

    private fun sendKey(keyCode: Int): Boolean =
        ShizukuHelper.executeShellCommand(arrayOf("input", "keyevent", keyCode.toString()))

    private fun sendMediaKey(context: Context, keyCode: Int): Boolean {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return try {
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun launchApp(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun launchAppFromShell(context: Context, packageName: String): Boolean {
        val component = context.packageManager.getLaunchIntentForPackage(packageName)?.component ?: return false
        return ShizukuHelper.executeShellCommand(arrayOf("am", "start", "-n", component.flattenToShortString()))
    }

    private fun appLabel(context: Context, packageName: String): String? {
        return try {
            val packageManager = context.packageManager
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            null
        }
    }
}
