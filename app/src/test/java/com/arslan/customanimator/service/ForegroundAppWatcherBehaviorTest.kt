package com.arslan.customanimator.service

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.arslan.customanimator.service.watcher.AppThreadingWatcher
import com.arslan.customanimator.service.watcher.AutoForceStopWatcher
import com.arslan.customanimator.service.watcher.PerAppRefreshRateWatcher
import com.arslan.customanimator.service.watcher.PerAppWidthWatcher
import com.arslan.customanimator.service.watcher.PermissionDisablerWatcher
import com.arslan.customanimator.utils.AppThreadingConfig
import com.arslan.customanimator.utils.AppThreadingManager
import com.arslan.customanimator.utils.AutoForceStopManager
import com.arslan.customanimator.utils.PerAppRefreshRateManager
import com.arslan.customanimator.utils.PerAppWidthManager
import com.arslan.customanimator.utils.PermissionDisablerManager
import com.arslan.customanimator.utils.RefreshRateManager
import com.arslan.customanimator.utils.RevokedPermissionsStore
import com.arslan.customanimator.utils.SettingsManager
import com.arslan.customanimator.utils.ThreadAffinityMode
import com.arslan.customanimator.utils.ThreadPriority
import com.arslan.customanimator.utils.UsageAccessHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class ForegroundAppWatcherBehaviorTest {

    private lateinit var context: Context
    private val target = "com.example.target"
    private val other = "com.example.other"
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        resetEverything()
        ShadowLog.clear()
    }

    @After
    fun tearDown() {
        resetEverything()
    }

    private fun resetEverything() {
        AutoForceStopManager(context).setSelectedPackages(emptySet())
        PermissionDisablerManager(context).setSelectedPackages(emptySet())
        PerAppWidthManager(context).clearAll()
        PerAppRefreshRateManager(context).clearAll()
        AppThreadingManager(context).clearAll()
        RevokedPermissionsStore(context).clearAll()
        Settings.Secure.putString(context.contentResolver, "display_density_forced", null)
        ForegroundAppWatcherService.watchers.forEach {
            it.refresh(context)
            it.onWatchStopped(context)
        }
        shadowOf(context as android.app.Application).clearStartedServices()
    }

    private fun forcedDensity(): Int? = SettingsManager.getForcedDensity(context.contentResolver)

    private fun logsFor(tag: String): List<String> =
        ShadowLog.getLogsForTag(tag).map { it.msg }

    private fun awaitLog(tag: String, needle: String, timeoutMs: Long = 5000): String {
        val deadline = System.nanoTime() + timeoutMs * 1_000_000
        while (System.nanoTime() < deadline) {
            logsFor(tag).firstOrNull { it.contains(needle) }?.let { return it }
            Thread.sleep(20)
        }
        throw AssertionError("No log for tag=$tag containing '$needle'. Got: ${logsFor(tag)}")
    }

    private fun assertNoLog(tag: String, needle: String) {
        Thread.sleep(200)
        assertTrue(
            "Unexpected log for tag=$tag containing '$needle': ${logsFor(tag)}",
            logsFor(tag).none { it.contains(needle) }
        )
    }

    @Test
    fun everyWatcherIsDisabledWhenNothingIsConfigured() {
        ForegroundAppWatcherService.watchers.forEach {
            assertFalse("${it.javaClass.simpleName} should be disabled", it.isEnabled)
        }
    }

    @Test
    fun eachFeatureEnablesExactlyItsOwnWatcher() {
        AutoForceStopManager(context).setSelectedPackages(setOf(target))
        ForegroundAppWatcherService.watchers.forEach { it.refresh(context) }
        assertEquals(listOf(AutoForceStopWatcher), ForegroundAppWatcherService.watchers.filter { it.isEnabled })

        AutoForceStopManager(context).setSelectedPackages(emptySet())
        PerAppWidthManager(context).setWidth(target, 400)
        ForegroundAppWatcherService.watchers.forEach { it.refresh(context) }
        assertEquals(listOf(PerAppWidthWatcher), ForegroundAppWatcherService.watchers.filter { it.isEnabled })

        PerAppWidthManager(context).clearAll()
        PerAppRefreshRateManager(context).setRate(target, 60f)
        ForegroundAppWatcherService.watchers.forEach { it.refresh(context) }
        assertEquals(listOf(PerAppRefreshRateWatcher), ForegroundAppWatcherService.watchers.filter { it.isEnabled })

        PerAppRefreshRateManager(context).clearAll()
        PermissionDisablerManager(context).setSelectedPackages(setOf(target))
        ForegroundAppWatcherService.watchers.forEach { it.refresh(context) }
        assertEquals(listOf(PermissionDisablerWatcher), ForegroundAppWatcherService.watchers.filter { it.isEnabled })

        PermissionDisablerManager(context).setSelectedPackages(emptySet())
        AppThreadingManager(context).setConfig(
            target,
            AppThreadingConfig(ThreadAffinityMode.BIG, ThreadPriority.HIGH)
        )
        ForegroundAppWatcherService.watchers.forEach { it.refresh(context) }
        assertEquals(listOf(AppThreadingWatcher), ForegroundAppWatcherService.watchers.filter { it.isEnabled })
    }

    @Test
    fun syncStartsTheSingleServiceWhenAnyFeatureIsOn() {
        PerAppWidthManager(context).setWidth(target, 400)
        ForegroundAppWatcherService.sync(context)
        val started = shadowOf(context as android.app.Application).nextStartedService
        assertNotNull("sync should start the watcher service", started)
        assertEquals(ForegroundAppWatcherService::class.java.name, started.component?.className)
    }

    @Test
    fun syncStopsTheServiceWhenTheLastFeatureIsTurnedOff() {
        PerAppWidthManager(context).setWidth(target, 400)
        ForegroundAppWatcherService.sync(context)
        shadowOf(context as android.app.Application).clearStartedServices()

        PerAppWidthManager(context).clearAll()
        ForegroundAppWatcherService.sync(context)
        assertNull(
            "sync must not start the service when nothing is enabled",
            shadowOf(context as android.app.Application).nextStartedService
        )
        val stopped = shadowOf(context as android.app.Application).nextStoppedService
        assertNotNull("sync should stop the watcher service", stopped)
        assertEquals(ForegroundAppWatcherService::class.java.name, stopped.component?.className)
    }

    @Test
    fun syncRefreshesWatcherStateBeforeDeciding() {
        AppThreadingManager(context).setConfig(
            target,
            AppThreadingConfig(ThreadAffinityMode.LITTLE, ThreadPriority.LOW)
        )
        ForegroundAppWatcherService.sync(context)
        assertTrue(AppThreadingWatcher.isEnabled)
        assertNotNull(shadowOf(context as android.app.Application).nextStartedService)
    }

    @Test
    fun perAppWidthAppliesOnForegroundAndRestoresOnBackground() {
        PerAppWidthManager(context).setWidth(target, 400)
        PerAppWidthWatcher.refresh(context)
        val baseline = forcedDensity()

        PerAppWidthWatcher.onAppForegrounded(context, target, scope)
        val applied = forcedDensity()
        assertNotNull("density should be forced while the app is foreground", applied)
        assertEquals(
            SettingsManager.densityForSmallestWidth(context, 400),
            applied
        )

        PerAppWidthWatcher.onAppBackgrounded(context, target, scope)
        assertEquals("baseline density must come back", baseline, forcedDensity())
    }

    @Test
    fun perAppWidthIgnoresPackagesWithoutAnOverride() {
        PerAppWidthManager(context).setWidth(target, 400)
        PerAppWidthWatcher.refresh(context)

        PerAppWidthWatcher.onAppForegrounded(context, other, scope)
        assertNull("unlisted app must not change density", forcedDensity())

        PerAppWidthWatcher.onAppBackgrounded(context, other, scope)
        assertNull(forcedDensity())
    }

    @Test
    fun perAppWidthDoesNotRestoreForAnUnrelatedBackgroundEvent() {
        PerAppWidthManager(context).setWidth(target, 400)
        PerAppWidthWatcher.refresh(context)

        PerAppWidthWatcher.onAppForegrounded(context, target, scope)
        val applied = forcedDensity()
        PerAppWidthWatcher.onAppBackgrounded(context, other, scope)
        assertEquals("another app backgrounding must not restore", applied, forcedDensity())
    }

    @Test
    fun perAppWidthDoesNotReapplyWhileTheSameAppStaysForeground() {
        PerAppWidthManager(context).setWidth(target, 400)
        PerAppWidthWatcher.refresh(context)

        PerAppWidthWatcher.onAppForegrounded(context, target, scope)
        Settings.Secure.putString(context.contentResolver, "display_density_forced", "999")
        PerAppWidthWatcher.onAppForegrounded(context, target, scope)
        assertEquals("repeat foreground must be a no-op", 999, forcedDensity())
    }

    @Test
    fun perAppWidthRestoresBaselineWhenTheWatchStops() {
        PerAppWidthManager(context).setWidth(target, 400)
        PerAppWidthWatcher.refresh(context)
        val baseline = forcedDensity()

        PerAppWidthWatcher.onAppForegrounded(context, target, scope)
        assertNotNull(forcedDensity())
        PerAppWidthWatcher.onWatchStopped(context)
        assertEquals(baseline, forcedDensity())
    }

    @Test
    fun perAppWidthWatchStoppedIsANoOpWhenNothingWasApplied() {
        PerAppWidthManager(context).setWidth(target, 400)
        PerAppWidthWatcher.refresh(context)
        Settings.Secure.putString(context.contentResolver, "display_density_forced", "420")

        PerAppWidthWatcher.onWatchStopped(context)
        assertEquals("stopping must not clobber a user-set density", 420, forcedDensity())
    }

    @Test
    fun perAppRefreshRateFailsSafelyWithoutShizuku() {
        PerAppRefreshRateManager(context).setRate(target, 60f)
        PerAppRefreshRateWatcher.refresh(context)

        PerAppRefreshRateWatcher.onAppForegrounded(context, target, scope)
        assertTrue(awaitLog("PerAppRefreshRateWatcher", "success=false").contains("rate=60.0"))
        assertNull(RefreshRateManager.getMinRate(context.contentResolver))

        PerAppRefreshRateWatcher.onAppBackgrounded(context, target, scope)
        assertNoLog("PerAppRefreshRateWatcher", "Restored")
    }

    @Test
    fun perAppRefreshRateIgnoresPackagesWithoutAnOverride() {
        PerAppRefreshRateManager(context).setRate(target, 60f)
        PerAppRefreshRateWatcher.refresh(context)

        PerAppRefreshRateWatcher.onAppForegrounded(context, other, scope)
        assertNoLog("PerAppRefreshRateWatcher", "Applied")
    }

    @Test
    fun autoForceStopWaitsForTheGracePeriodBeforeKilling() {
        AutoForceStopManager(context).setSelectedPackages(setOf(target))
        AutoForceStopWatcher.refresh(context)

        AutoForceStopWatcher.onAppBackgrounded(context, target, scope)
        AutoForceStopWatcher.onTick(context, scope)
        assertNoLog("AutoForceStopWatcher", "Force-stopped")

        Thread.sleep(1300)
        AutoForceStopWatcher.onTick(context, scope)
        assertTrue(awaitLog("AutoForceStopWatcher", "Force-stopped").contains(target))
    }

    @Test
    fun autoForceStopCancelsTheKillWhenTheAppComesBack() {
        AutoForceStopManager(context).setSelectedPackages(setOf(target))
        AutoForceStopWatcher.refresh(context)

        AutoForceStopWatcher.onAppBackgrounded(context, target, scope)
        AutoForceStopWatcher.onAppForegrounded(context, target, scope)
        Thread.sleep(1300)
        AutoForceStopWatcher.onTick(context, scope)
        assertNoLog("AutoForceStopWatcher", "Force-stopped")
    }

    @Test
    fun autoForceStopDoesNotKillTheSameAppTwiceInARow() {
        AutoForceStopManager(context).setSelectedPackages(setOf(target))
        AutoForceStopWatcher.refresh(context)

        AutoForceStopWatcher.onAppBackgrounded(context, target, scope)
        Thread.sleep(1300)
        AutoForceStopWatcher.onTick(context, scope)
        awaitLog("AutoForceStopWatcher", "Force-stopped")
        ShadowLog.clear()

        AutoForceStopWatcher.onAppBackgrounded(context, target, scope)
        Thread.sleep(1300)
        AutoForceStopWatcher.onTick(context, scope)
        assertNoLog("AutoForceStopWatcher", "Force-stopped")
    }

    @Test
    fun autoForceStopIgnoresUnselectedPackages() {
        AutoForceStopManager(context).setSelectedPackages(setOf(target))
        AutoForceStopWatcher.refresh(context)

        AutoForceStopWatcher.onAppBackgrounded(context, other, scope)
        Thread.sleep(1300)
        AutoForceStopWatcher.onTick(context, scope)
        assertNoLog("AutoForceStopWatcher", "Force-stopped")
    }

    @Test
    fun autoForceStopDropsPendingKillsWhenTheWatchStops() {
        AutoForceStopManager(context).setSelectedPackages(setOf(target))
        AutoForceStopWatcher.refresh(context)

        AutoForceStopWatcher.onAppBackgrounded(context, target, scope)
        AutoForceStopWatcher.onWatchStopped(context)
        Thread.sleep(1300)
        AutoForceStopWatcher.onTick(context, scope)
        assertNoLog("AutoForceStopWatcher", "Force-stopped")
    }

    @Test
    fun permissionDisablerWaitsForTheGracePeriodBeforeRevoking() {
        PermissionDisablerManager(context).setSelectedPackages(setOf(target))
        PermissionDisablerWatcher.refresh(context)

        PermissionDisablerWatcher.onAppBackgrounded(context, target, scope)
        PermissionDisablerWatcher.onTick(context, scope)
        assertNoLog("PermissionDisablerWatcher", "Revoked")

        Thread.sleep(1300)
        PermissionDisablerWatcher.onTick(context, scope)
        assertNoLog("PermissionDisablerWatcher", "Revoked")
    }

    @Test
    fun permissionDisablerKeepsRevokedStateWhenRegrantFails() {
        PermissionDisablerManager(context).setSelectedPackages(setOf(target))
        PermissionDisablerWatcher.refresh(context)
        val store = RevokedPermissionsStore(context)
        store.recordRevoked(target, listOf("android.permission.CAMERA"))

        PermissionDisablerWatcher.onAppForegrounded(context, target, scope)
        assertTrue(awaitLog("PermissionDisablerWatcher", "Regranted").contains("allSucceeded=false"))
        assertEquals(
            "state must survive a failed regrant so the app is not left permission-less",
            listOf("android.permission.CAMERA"),
            store.getRevoked(target)
        )
    }

    @Test
    fun permissionDisablerIgnoresUnselectedPackages() {
        PermissionDisablerManager(context).setSelectedPackages(setOf(target))
        PermissionDisablerWatcher.refresh(context)
        RevokedPermissionsStore(context).recordRevoked(other, listOf("android.permission.CAMERA"))

        PermissionDisablerWatcher.onAppForegrounded(context, other, scope)
        assertNoLog("PermissionDisablerWatcher", "Regranted")
    }

    @Test
    fun permissionDisablerDropsPendingRevokesWhenTheWatchStops() {
        PermissionDisablerManager(context).setSelectedPackages(setOf(target))
        PermissionDisablerWatcher.refresh(context)

        PermissionDisablerWatcher.onAppBackgrounded(context, target, scope)
        PermissionDisablerWatcher.onWatchStopped(context)
        Thread.sleep(1300)
        PermissionDisablerWatcher.onTick(context, scope)
        assertNoLog("PermissionDisablerWatcher", "Revoked")
    }

    @Test
    fun appThreadingOnlyTracksNonDefaultConfigs() {
        val manager = AppThreadingManager(context)
        manager.setConfig(target, AppThreadingConfig(ThreadAffinityMode.ALL, ThreadPriority.NORMAL))
        AppThreadingWatcher.refresh(context)
        assertFalse("a default config must not keep the service alive", AppThreadingWatcher.isEnabled)

        manager.setConfig(target, AppThreadingConfig(ThreadAffinityMode.BIG, ThreadPriority.HIGH))
        AppThreadingWatcher.refresh(context)
        assertTrue(AppThreadingWatcher.isEnabled)
    }

    @Test
    fun appThreadingDoesNothingWhenNoPidsCanBeRead() {
        AppThreadingManager(context).setConfig(
            target,
            AppThreadingConfig(ThreadAffinityMode.BIG, ThreadPriority.HIGH)
        )
        AppThreadingWatcher.refresh(context)

        AppThreadingWatcher.onAppForegrounded(context, target, scope)
        assertNoLog("AppThreadingWatcher", "Applied")
        AppThreadingWatcher.onAppBackgrounded(context, target, scope)
        AppThreadingWatcher.onWatchStopped(context)
    }

    @Test
    fun usageAccessIsNotSelfGrantedWithoutShizuku() {
        assertFalse(UsageAccessHelper.grantUsageAccess(context))
    }
}
