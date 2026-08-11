package com.arslan.customanimator.utils

object MemoryBooster {

    fun boost(): Boolean {
        val killed = ShizukuHelper.executeShellCommand(arrayOf("am", "kill-all"))
        ShizukuHelper.executeShellCommand(arrayOf("am", "compact", "all", "full"))
        return killed
    }
}
