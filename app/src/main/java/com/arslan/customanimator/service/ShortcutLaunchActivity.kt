package com.arslan.customanimator.service

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import com.arslan.customanimator.R
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

        val entry = intent?.getStringExtra(ShortcutMaker.EXTRA_SHORTCUT_ID)?.let { ShortcutStore.get(this, it) }
        if (entry == null) {
            Toast.makeText(this, getString(R.string.shortcut_missing), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        backgroundScope.launch {
            val launched = ShortcutMaker.launch(this@ShortcutLaunchActivity, entry)
            withContext(Dispatchers.Main) {
                if (!launched) {
                    Toast.makeText(
                        applicationContext,
                        getString(ShortcutMaker.failureMessageRes(entry)),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                finish()
            }
        }
    }
}
