package com.arslan.customanimator.utils

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.view.KeyEvent
import androidx.annotation.StringRes
import com.arslan.customanimator.R
import com.arslan.customanimator.screenshot.ScreenshotActions

data class ButtonAction(val type: Type, val value: String) {
    enum class Type { QUICK, TOGGLE, APP, COMMAND }
}

object ButtonActions {

    enum class Group(@StringRes val titleRes: Int) {
        NAVIGATION(R.string.button_action_group_navigation),
        MEDIA(R.string.button_action_group_media),
        DEVICE(R.string.button_action_group_device),
        APPS(R.string.button_action_group_apps)
    }

    enum class Quick(
        val key: String,
        @StringRes val nameRes: Int,
        val iconKey: String,
        val group: Group,
        val needsShizuku: Boolean,
        val run: (Context) -> Boolean
    ) {
        HOME("home", R.string.button_action_home, "home", Group.NAVIGATION, true, { sendKey(KeyEvent.KEYCODE_HOME) }),
        BACK("back", R.string.button_action_back, "arrow_back", Group.NAVIGATION, true, { sendKey(KeyEvent.KEYCODE_BACK) }),
        RECENT_APPS(
            "recent_apps", R.string.button_action_recent_apps, "layers", Group.NAVIGATION, true,
            { sendKey(KeyEvent.KEYCODE_APP_SWITCH) }
        ),
        NOTIFICATIONS(
            "notifications", R.string.button_action_notifications, "notifications", Group.NAVIGATION, true,
            { statusBar("expand-notifications") }
        ),
        QUICK_SETTINGS(
            "quick_settings", R.string.button_action_quick_settings, "tune", Group.NAVIGATION, true,
            { statusBar("expand-settings") }
        ),
        CLOSE_SHADE(
            "close_shade", R.string.button_action_close_shade, "expand_less", Group.NAVIGATION, true,
            { SoundTileActions.collapseShade() }
        ),
        LOCK_SCREEN(
            "lock_screen", R.string.button_action_lock_screen, "lock", Group.NAVIGATION, true,
            { sendKey(KeyEvent.KEYCODE_SLEEP) }
        ),
        ASSISTANT(
            "assistant", R.string.button_action_assistant, "mic", Group.NAVIGATION, true,
            { startAction("android.intent.action.VOICE_COMMAND") }
        ),
        SYSTEM_SETTINGS(
            "system_settings", R.string.button_action_system_settings, "settings", Group.NAVIGATION, true,
            { startAction("android.settings.SETTINGS") }
        ),
        PLAY_PAUSE(
            "play_pause", R.string.button_action_play_pause, "play_arrow", Group.MEDIA, false,
            { sendMediaKey(it, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }
        ),
        NEXT_TRACK(
            "next_track", R.string.button_action_next_track, "skip_next", Group.MEDIA, false,
            { sendMediaKey(it, KeyEvent.KEYCODE_MEDIA_NEXT) }
        ),
        PREVIOUS_TRACK(
            "previous_track", R.string.button_action_previous_track, "skip_previous", Group.MEDIA, false,
            { sendMediaKey(it, KeyEvent.KEYCODE_MEDIA_PREVIOUS) }
        ),
        STOP_MEDIA(
            "stop_media", R.string.button_action_stop_media, "stop", Group.MEDIA, false,
            { sendMediaKey(it, KeyEvent.KEYCODE_MEDIA_STOP) }
        ),
        VOLUME_UP(
            "volume_up", R.string.button_action_volume_up, "volume_up", Group.MEDIA, false,
            { adjustMediaVolume(it, AudioManager.ADJUST_RAISE) }
        ),
        VOLUME_DOWN(
            "volume_down", R.string.button_action_volume_down, "volume_down", Group.MEDIA, false,
            { adjustMediaVolume(it, AudioManager.ADJUST_LOWER) }
        ),
        MUTE_MEDIA(
            "mute_media", R.string.button_action_mute_media, "volume_off", Group.MEDIA, false,
            { adjustMediaVolume(it, AudioManager.ADJUST_TOGGLE_MUTE) }
        ),
        VOLUME_PANEL(
            "volume_panel", R.string.sound_tile_action_volume_panel, "graphic_eq", Group.MEDIA, false,
            { SoundTileActions.showVolumePanel(it) }
        ),
        MEDIA_OUTPUT(
            "media_output", R.string.button_action_media_output, "speaker_group", Group.MEDIA, true,
            { SoundTileActions.openMediaOutputDialog() }
        ),
        RING_VIBRATE(
            "ring_vibrate", R.string.button_action_ring_vibrate, "vibration", Group.MEDIA, false,
            { toggleVibrate(it) }
        ),
        FLASHLIGHT(
            "flashlight", R.string.button_action_flashlight, "flashlight_on", Group.DEVICE, false,
            { TorchToggle.toggle(it) }
        ),
        SCREENSHOT(
            "screenshot", R.string.button_action_screenshot, "screenshot", Group.DEVICE, true,
            { sendKey(KeyEvent.KEYCODE_SYSRQ) }
        ),
        COPY_SCREENSHOT(
            "copy_screenshot", R.string.button_action_copy_screenshot, "content_copy", Group.DEVICE, false,
            { context -> ScreenshotActions.newestScreenshot(context)?.let { ScreenshotActions.copyToClipboard(context, it) } ?: false }
        ),
        DELETE_SCREENSHOT(
            "delete_screenshot", R.string.button_action_delete_screenshot, "delete", Group.DEVICE, false,
            { context -> ScreenshotActions.newestScreenshot(context)?.let { ScreenshotActions.delete(context, it) } ?: false }
        ),
        CAMERA(
            "camera", R.string.button_action_camera, "photo_camera", Group.DEVICE, true,
            { startAction("android.media.action.STILL_IMAGE_CAMERA") }
        ),
        VIDEO_CAMERA(
            "video_camera", R.string.button_action_video_camera, "videocam", Group.DEVICE, true,
            { startAction("android.media.action.VIDEO_CAMERA") }
        ),
        KEEP_SCREEN_ON(
            "keep_screen_on", R.string.button_action_keep_screen_on, "coffee", Group.DEVICE, true,
            { CaffeineManager.setActive(it, it.contentResolver, !CaffeineManager.isActive(it.contentResolver)) }
        ),
        FORCE_STOP_APP(
            "force_stop_app", R.string.button_action_force_stop_app, "cancel", Group.APPS, true,
            { forceStopForegroundApp() }
        ),
        CLOSE_BACKGROUND_APPS(
            "close_background_apps", R.string.close_background_apps, "stop_circle", Group.APPS, true,
            { ShizukuHelper.hasShizukuPermission() && DeveloperOptionsManager.forceStopBackgroundApps(it) }
        ),
        FREE_MEMORY("free_memory", R.string.button_action_free_memory, "memory", Group.APPS, true, { MemoryBooster.boost() }),
        CLEAR_CACHES(
            "clear_caches", R.string.clear_all_app_caches, "cleaning_services", Group.APPS, true,
            { DeveloperOptionsManager.clearAllAppCaches() }
        ),
        RESTART_SYSTEM_UI(
            "restart_system_ui", R.string.restart_system_ui, "restart_alt", Group.APPS, true,
            { DeveloperOptionsManager.restartSystemUi() }
        );

        companion object {
            fun fromKey(key: String?): Quick? = entries.firstOrNull { it.key == key }
        }
    }

    private const val FOREGROUND_PACKAGE_SCRIPT =
        "dumpsys activity activities | grep -m1 topResumedActivity | sed -E 's/.* ([A-Za-z0-9_.]+)\\/.*/\\1/'"

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

    private fun statusBar(command: String): Boolean =
        ShizukuHelper.executeShellCommand(arrayOf("cmd", "statusbar", command))

    private fun startAction(action: String): Boolean =
        ShizukuHelper.executeShellCommand(arrayOf("am", "start", "--user", "current", "-a", action))

    private fun forceStopForegroundApp(): Boolean {
        val result = ShizukuHelper.executeShellCommandWithOutput(arrayOf("sh", "-c", FOREGROUND_PACKAGE_SCRIPT))
        val packageName = result.output.trim().takeIf { result.isSuccess && it.isNotEmpty() } ?: return false
        return DeveloperOptionsManager.forceStopApp(packageName)
    }

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

    private fun adjustMediaVolume(context: Context, direction: Int): Boolean {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return runCatching {
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            true
        }.getOrDefault(false)
    }

    private fun toggleVibrate(context: Context): Boolean {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val next = if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            AudioManager.RINGER_MODE_VIBRATE
        } else {
            AudioManager.RINGER_MODE_NORMAL
        }
        return runCatching {
            audio.ringerMode = next
            true
        }.getOrDefault(false)
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
