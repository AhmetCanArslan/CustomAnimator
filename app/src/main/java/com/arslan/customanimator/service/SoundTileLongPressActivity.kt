package com.arslan.customanimator.service

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.arslan.customanimator.MainActivity
import com.arslan.customanimator.R
import com.arslan.customanimator.utils.ShizukuHelper
import com.arslan.customanimator.utils.SoundTileAction
import com.arslan.customanimator.utils.SoundTileActions
import com.arslan.customanimator.utils.SoundTilePrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SoundTileLongPressActivity : Activity() {

    private val EXTRA_COMPONENT_ID = "android.service.quicksettings.extra.COMPONENT_ID"

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val componentId = intent?.getParcelableExtra<ComponentName>(EXTRA_COMPONENT_ID)
        if (componentId != null && componentId.className != SoundTileService::class.java.name) {
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
}
