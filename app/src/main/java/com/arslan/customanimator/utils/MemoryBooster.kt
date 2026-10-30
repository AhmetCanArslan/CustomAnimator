package com.arslan.customanimator.utils

object MemoryBooster {

    fun boost(): Boolean {
        val killed = ShizukuHelper.executeShellCommand(arrayOf("am", "kill-all"))
        compact()
        return killed
    }

    fun compact(): Boolean {
        return ShizukuHelper.executeShellCommand(arrayOf("am", "compact", "all", "full"))
    }
}
