package com.arslan.customanimator.service

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.arslan.customanimator.R
import com.arslan.customanimator.data.ShortcutEntry
import com.arslan.customanimator.utils.ButtonRemapperManager
import com.arslan.customanimator.utils.ButtonTrigger
import com.arslan.customanimator.utils.ShortcutMaker
import com.arslan.customanimator.utils.ShortcutStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShortcutLaunchActivity : Activity() {

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val entry = intent?.let(::resolveEntry)
        if (entry == null) {
            Toast.makeText(this, getString(R.string.shortcut_missing), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (entry.opensDirectly) {
            finishLaunch(entry, ShortcutMaker.startDirectly(this, entry))
            return
        }

        backgroundScope.launch {
            val launched = ShortcutMaker.launch(this@ShortcutLaunchActivity, entry)
            withContext(Dispatchers.Main) { finishLaunch(entry, launched) }
        }
    }

    private fun finishLaunch(entry: ShortcutEntry, launched: Boolean) {
        if (!launched) {
            Toast.makeText(applicationContext, ShortcutMaker.failureMessage(this, entry), Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun resolveEntry(intent: Intent): ShortcutEntry? {
        intent.getStringExtra(ShortcutMaker.EXTRA_SHORTCUT_ID)?.let { return ShortcutStore.get(this, it) }
        val trigger = ButtonTrigger.fromName(intent.getStringExtra(ShortcutMaker.EXTRA_BUTTON_TRIGGER)) ?: return null
        return ButtonRemapperManager.getAction(this, trigger)
    }
}
