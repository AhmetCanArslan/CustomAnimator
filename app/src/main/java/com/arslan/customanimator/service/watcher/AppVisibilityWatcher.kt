package com.arslan.customanimator.service.watcher

import android.content.Context
import kotlinx.coroutines.CoroutineScope

interface AppVisibilityWatcher {

    val isEnabled: Boolean

    fun refresh(context: Context)

    fun onAppForegrounded(context: Context, packageName: String, scope: CoroutineScope)

    fun onAppBackgrounded(context: Context, packageName: String, scope: CoroutineScope)

    fun onWatchStopped(context: Context) {}

    fun recoverStaleState(context: Context) {}
}
