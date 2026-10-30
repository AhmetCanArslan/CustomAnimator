package com.riyadmondol2006.customanimator.service

import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import com.riyadmondol2006.customanimator.utils.ButtonRemapperManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PowerAssistService : VoiceInteractionService() {

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReady() {
        super.onReady()
        setDisabledShowContext(
            VoiceInteractionSession.SHOW_WITH_ASSIST or VoiceInteractionSession.SHOW_WITH_SCREENSHOT
        )
        backgroundScope.launch { ButtonRemapperManager.keepPreviousRecognizer(this@PowerAssistService) }
    }

    override fun onDestroy() {
        backgroundScope.cancel()
        super.onDestroy()
    }
}
