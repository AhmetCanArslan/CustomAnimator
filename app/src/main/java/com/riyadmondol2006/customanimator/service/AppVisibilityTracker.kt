package com.riyadmondol2006.customanimator.service

data class VisibilityChange(
    val packageName: String,
    val foreground: Boolean
)

class AppVisibilityTracker(private val ownPackageName: String) {

    private val foregroundProcesses = HashMap<Int, Set<String>>()
    private val visibleCounts = HashMap<String, Int>()

    fun reset() {
        foregroundProcesses.clear()
        visibleCounts.clear()
    }

    fun onForegroundActivitiesChanged(pid: Int, packages: Set<String>, foreground: Boolean): List<VisibilityChange> {
        val shown = if (foreground) packages - ownPackageName else emptySet()
        val hidden = if (foreground) foregroundProcesses.put(pid, shown) else foregroundProcesses.remove(pid)
        return applyChange(hidden.orEmpty(), shown)
    }

    fun onProcessDied(pid: Int): List<VisibilityChange> =
        applyChange(foregroundProcesses.remove(pid).orEmpty(), emptySet())

    fun visiblePackages(): Set<String> = visibleCounts.keys.toSet()

    private fun applyChange(hidden: Set<String>, shown: Set<String>): List<VisibilityChange> {
        if (hidden.isEmpty() && shown.isEmpty()) return emptyList()
        val changes = ArrayList<VisibilityChange>()
        hidden.forEach { if (it !in shown && release(it)) changes.add(VisibilityChange(it, foreground = false)) }
        shown.forEach { if (it !in hidden && retain(it)) changes.add(VisibilityChange(it, foreground = true)) }
        return changes
    }

    private fun retain(packageName: String): Boolean {
        val count = visibleCounts[packageName] ?: 0
        visibleCounts[packageName] = count + 1
        return count == 0
    }

    private fun release(packageName: String): Boolean {
        val count = visibleCounts[packageName] ?: return false
        if (count > 1) {
            visibleCounts[packageName] = count - 1
            return false
        }
        visibleCounts.remove(packageName)
        return true
    }
}
