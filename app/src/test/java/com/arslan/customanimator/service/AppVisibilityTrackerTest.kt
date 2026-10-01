package com.arslan.customanimator.service

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppVisibilityTrackerTest {

    private val own = "com.arslan.customanimator"
    private val chat = "com.whatsapp"
    private val launcher = "com.android.launcher"
    private val other = "com.example.other"

    private lateinit var tracker: AppVisibilityTracker

    @Before
    fun setUp() {
        tracker = AppVisibilityTracker(own)
    }

    private fun shown(pid: Int, vararg packages: String) =
        tracker.onForegroundActivitiesChanged(pid, packages.toSet(), foreground = true)

    private fun hidden(pid: Int, vararg packages: String) =
        tracker.onForegroundActivitiesChanged(pid, packages.toSet(), foreground = false)

    private fun foreground(pkg: String) = listOf(VisibilityChange(pkg, foreground = true))

    private fun background(pkg: String) = listOf(VisibilityChange(pkg, foreground = false))

    private val none = emptyList<VisibilityChange>()

    @Test
    fun appComingToTheForegroundIsReportedOnce() {
        assertEquals(foreground(chat), shown(100, chat))
        assertEquals(none, shown(100, chat))
    }

    @Test
    fun leavingToTheLauncherBackgroundsTheApp() {
        shown(100, chat)
        assertEquals(foreground(launcher), shown(200, launcher))
        assertEquals(background(chat), hidden(100, chat))
    }

    @Test
    fun appStaysForegroundWhileAnotherOfItsProcessesIsVisible() {
        shown(100, chat)
        assertEquals(none, shown(101, chat))
        assertEquals(none, hidden(100, chat))
        assertEquals(background(chat), hidden(101, chat))
    }

    @Test
    fun appStillVisibleInSplitScreenIsNotBackgrounded() {
        shown(100, chat)
        assertEquals(foreground(other), shown(300, other))
        assertEquals(background(other), hidden(300, other))
        assertEquals(none, shown(100, chat))
    }

    @Test
    fun processDeathBackgroundsItsPackages() {
        shown(100, chat)
        assertEquals(background(chat), tracker.onProcessDied(100))
    }

    @Test
    fun deathOfAnUnknownProcessIsIgnored() {
        assertEquals(none, tracker.onProcessDied(999))
    }

    @Test
    fun hiddenEventForAnUnseenProcessIsIgnored() {
        assertEquals(none, hidden(100, chat))
    }

    @Test
    fun ownPackageIsNeverReported() {
        assertEquals(none, shown(400, own))
        assertEquals(none, hidden(400, own))
    }

    @Test
    fun sharedUidReportsEveryPackage() {
        val settings = "com.android.settings"
        val system = "android"
        assertEquals(
            setOf(VisibilityChange(settings, true), VisibilityChange(system, true)),
            shown(500, settings, system).toSet()
        )
        assertEquals(
            setOf(VisibilityChange(settings, false), VisibilityChange(system, false)),
            hidden(500, settings, system).toSet()
        )
    }

    @Test
    fun visiblePackagesListsEveryForegroundPackage() {
        shown(100, chat)
        shown(300, other)
        assertEquals(setOf(chat, other), tracker.visiblePackages())
        hidden(100, chat)
        assertEquals(setOf(other), tracker.visiblePackages())
    }

    @Test
    fun resetForgetsEverything() {
        shown(100, chat)
        tracker.reset()
        assertEquals(none, hidden(100, chat))
        assertEquals(foreground(chat), shown(100, chat))
    }
}
