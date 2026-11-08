package com.arslan.customanimator.service

import android.util.Log
import com.arslan.customanimator.utils.SettingsDatabaseManager
import kotlin.concurrent.thread
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

class AppShizukuProvider : ShizukuProvider() {

    override fun onCreate(): Boolean {
        val appContext = context?.applicationContext ?: return super.onCreate()
        Shizuku.addBinderReceivedListenerSticky {
            thread {
                try {
                    SettingsDatabaseManager.applyPendingShizukuSettings(appContext)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to reapply settings database values through Shizuku", e)
                }
            }
        }
        return super.onCreate()
    }

    private companion object {
        const val TAG = "AppShizukuProvider"
    }
}
