package com.arslan.customanimator.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter

data class BoostSnapshot(val availableRamBytes: Long, val availableStorageBytes: Long)

object BoostStats {

    fun snapshot(context: Context): BoostSnapshot =
        BoostSnapshot(availableRam(context), availableStorage())

    fun formatSize(context: Context, bytes: Long): String =
        Formatter.formatShortFileSize(context, if (bytes < 0) 0 else bytes)

    private fun availableRam(context: Context): Long {
        return try {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            manager.getMemoryInfo(info)
            info.availMem
        } catch (e: Exception) {
            0L
        }
    }

    private fun availableStorage(): Long {
        return try {
            val stat = StatFs(Environment.getDataDirectory().absolutePath)
            stat.availableBytes
        } catch (e: Exception) {
            0L
        }
    }
}
