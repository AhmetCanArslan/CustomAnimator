package com.riyadmondol2006.customanimator.service

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.IntentCompat
import com.riyadmondol2006.customanimator.MainActivity
import com.riyadmondol2006.customanimator.R
import com.riyadmondol2006.customanimator.utils.ShizukuHelper
import com.riyadmondol2006.customanimator.utils.SoundTileAction
import com.riyadmondol2006.customanimator.utils.SoundTileActions
import com.riyadmondol2006.customanimator.utils.SoundTilePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SoundTileLongPressActivity : Activity() {

    private val EXTRA_COMPONENT_ID = "android.service.quicksettings.extra.COMPONENT_ID"

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isSoundTileLongPress()) {
            runCatching {
                startActivity(
                    Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            finish()
            return
        }

        val action = SoundTilePrefs(this).longPressAction

        when {
            action == SoundTileAction.NONE -> Unit
            SoundTileActions.needsShizuku(action) && !ShizukuHelper.hasShizukuPermission() ->
                Toast.makeText(this, getString(R.string.sound_tile_needs_shizuku), Toast.LENGTH_SHORT).show()
            action == SoundTileAction.VOLUME_PANEL -> SoundTileActions.showVolumePanel(this)
            else -> backgroundScope.launch { SoundTileActions.openMediaOutputDialog() }
        }

        finish()
    }

    private fun isSoundTileLongPress(): Boolean {
        val component = intent?.let { IntentCompat.getParcelableExtra(it, Intent.EXTRA_COMPONENT_NAME, ComponentName::class.java) }
            ?: intent?.let { IntentCompat.getParcelableExtra(it, EXTRA_COMPONENT_ID, ComponentName::class.java) }
            ?: return false
        return component.className == SoundTileService::class.java.name
    }
}
