package com.arslan.customanimator.service

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.widget.Toast
import com.arslan.customanimator.utils.ButtonRemapperManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.arslan.customanimator.utils.ShizukuHelper

class PowerAssistSessionService : VoiceInteractionSessionService() {

    override fun onNewSession(args: Bundle?): VoiceInteractionSession = PowerAssistSession(this)
}

private class PowerAssistSession(context: Context) : VoiceInteractionSession(context) {

    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setUiEnabled(false)
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        sessionScope.launch {
            val performed = withContext(Dispatchers.IO) { ButtonRemapperManager.performCustomLongPress(context) }
            if (!performed) {
                Toast.makeText(context, ShizukuHelper.failureMessage(context), Toast.LENGTH_SHORT).show()
            }
            hide()
        }
    }

    override fun onDestroy() {
        sessionScope.cancel()
        super.onDestroy()
    }
}
