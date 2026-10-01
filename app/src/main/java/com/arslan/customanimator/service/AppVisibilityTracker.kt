package com.arslan.customanimator.service

data class VisibilityChange(
    val packageName: String,
    val foreground: Boolean
)

class AppVisibilityTracker(private val ownPackageName: String) {

    private val foregroundProcesses = mutableMapOf<Int, Set<String>>()

    fun reset() {
        foregroundProcesses.clear()
    }

    fun onForegroundActivitiesChanged(pid: Int, packages: Set<String>, foreground: Boolean): List<VisibilityChange> =
        update {
            if (foreground) {
                foregroundProcesses[pid] = packages - ownPackageName
            } else {
                foregroundProcesses.remove(pid)
            }
        }

    fun onProcessDied(pid: Int): List<VisibilityChange> = update { foregroundProcesses.remove(pid) }

    private fun update(mutation: () -> Unit): List<VisibilityChange> {
        val before = visiblePackages()
        mutation()
        val after = visiblePackages()
        return (before - after).map { VisibilityChange(it, foreground = false) } +
            (after - before).map { VisibilityChange(it, foreground = true) }
    }

    fun visiblePackages(): Set<String> = foregroundProcesses.values.flatten().toSet()
}
