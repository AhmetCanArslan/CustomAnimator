package com.arslan.customanimator.utils

import android.content.Context
import android.media.AudioManager

object SoundTileActions {

    private const val MEDIA_OUTPUT_ACTION =
        "com.android.systemui.action.LAUNCH_SYSTEM_MEDIA_OUTPUT_DIALOG"
    private const val MEDIA_OUTPUT_COMPONENT =
        "com.android.systemui/com.android.systemui.media.dialog.MediaOutputDialogReceiver"

    fun showVolumePanel(context: Context): Boolean {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return runCatching {
            audio.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_SAME,
                AudioManager.FLAG_SHOW_UI
            )
            true
        }.getOrDefault(false)
    }

    fun openMediaOutputDialog(): Boolean {
        return ShizukuHelper.executeShellCommand(
            arrayOf(
                "am", "broadcast",
                "-a", MEDIA_OUTPUT_ACTION,
                "-n", MEDIA_OUTPUT_COMPONENT
            )
        )
    }

    fun collapseShade(): Boolean {
        return ShizukuHelper.executeShellCommand(arrayOf("cmd", "statusbar", "collapse"))
    }

    fun needsShizuku(action: SoundTileAction): Boolean = action == SoundTileAction.MEDIA_OUTPUT

    fun perform(context: Context, action: SoundTileAction): Boolean = when (action) {
        SoundTileAction.VOLUME_PANEL -> showVolumePanel(context)
        SoundTileAction.MEDIA_OUTPUT -> openMediaOutputDialog()
        SoundTileAction.NONE -> true
    }
}
