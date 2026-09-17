package com.arslan.customanimator.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.arslan.customanimator.data.DebloatAppInfo
import com.arslan.customanimator.data.DebloatState
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NoShizukuResilienceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun shizukuIsReportedUnavailableWithoutABinder() {
        assertFalse(ShizukuHelper.isShizukuAvailable())
        assertFalse(ShizukuHelper.hasShizukuPermission())
    }

    @Test
    fun shellCommandsFailSafelyInsteadOfCrashing() {
        assertFalse(ShizukuHelper.executeShellCommand(arrayOf("echo", "hello")))
        val result = ShizukuHelper.executeShellCommandWithOutput(arrayOf("echo", "hello"))
        assertFalse(result.isSuccess)
        assertTrue(result.exitCode != 0)
    }

    @Test
    fun hwuiTweaksReportDefaultsAndRefuseToApply() {
        assertEquals(HwuiTweaksManager.RENDERER_DEFAULT, HwuiTweaksManager.getRenderer())
        assertEquals(HwuiTweaksManager.TEXTURE_CACHE_DEFAULT, HwuiTweaksManager.getTextureCacheSize())
        assertFalse(HwuiTweaksManager.isOverdrawDebugEnabled())
        assertFalse(HwuiTweaksManager.isForceGpuRenderingEnabled())

        assertFalse(HwuiTweaksManager.setRenderer(HwuiTweaksManager.RENDERER_SKIA_VK))
        assertFalse(HwuiTweaksManager.setTextureCacheSize(96))
        assertFalse(HwuiTweaksManager.setHwOverlaysDisabled(context, true))
        assertFalse(HwuiTweaksManager.areHwOverlaysDisabled(context))
    }

    @Test
    fun textureCacheOptionsStayOrderedAndSane() {
        val options = HwuiTweaksManager.textureCacheOptions
        assertEquals(HwuiTweaksManager.TEXTURE_CACHE_DEFAULT, options.first())
        assertEquals(options.sorted(), options)
        assertEquals(options.distinct(), options)
        assertTrue(options.drop(1).all { it in 8..256 })
    }

    @Test
    fun dozeWhitelistIsEmptyAndWritesFailWithoutShizuku() {
        assertTrue(DozeWhitelistManager.getWhitelist().isEmpty())
        assertTrue(DozeWhitelistManager.getWhitelistedPackages().isEmpty())
        assertFalse(DozeWhitelistManager.add("com.example.app"))
        assertFalse(DozeWhitelistManager.setWhitelisted("com.example.app", true))
    }

    @Test
    fun gameModeRefusesToTurnOnWithoutShizukuAndStaysOff() {
        assertFalse(GameModeController.canApply(context))
        assertFalse(GameModeController.isActive(context))
        assertFalse(GameModeController.setActive(context, true).succeeded)
        assertFalse(GameModeController.isActive(context))
    }

    @Test
    fun threadingApplyIsANoOpWithoutShizuku() {
        val manager = AppThreadingManager(context)
        manager.clearAll()
        manager.setConfig("com.example.game", AppThreadingConfig(ThreadAffinityMode.BIG, ThreadPriority.HIGH))
        val config = manager.getConfig("com.example.game")
        assertFalse(manager.apply("com.example.game", config).isSuccess)
        assertEquals(1, manager.getConfigs().size)
    }

    @Test
    fun debloaterListsNothingAndRefusesEveryChangeWithoutShizuku() {
        assertTrue(DebloatManager.loadApps(context).isEmpty())

        val target = DebloatAppInfo(
            packageName = "com.example.bloat",
            label = "Bloat",
            icon = null,
            apkPath = null,
            isSystemApp = true,
            state = DebloatState.ACTIVE,
            risk = DebloatCatalog.Risk.RECOMMENDED,
            group = DebloatCatalog.Group.PARTNER,
            isProtected = false,
            isRestorable = true,
            isChangedByApp = false,
            description = null
        )

        assertNull(DebloatManager.exportApk(target))
        assertFalse(DebloatManager.remove(context, target))
        assertFalse(DebloatManager.disable(context, target))
        assertFalse(DebloatManager.restore(context, target))
        assertEquals(0, DebloatManager.restoreAll(context))
        assertEquals(0, DebloatManager.changedPackageCount(context))
    }

    @Test
    fun debloaterNeverTouchesProtectedPackages() {
        val blocked = DebloatCatalog.protectedPackages(context)
        assertTrue(blocked.contains(context.packageName))
        assertTrue(blocked.contains("android"))
        assertTrue(blocked.contains("com.android.systemui"))
        assertTrue(blocked.contains("com.android.settings"))

        val protectedApp = DebloatAppInfo(
            packageName = "com.android.systemui",
            label = "System UI",
            icon = null,
            apkPath = null,
            isSystemApp = true,
            state = DebloatState.ACTIVE,
            risk = DebloatCatalog.Risk.UNSAFE,
            group = DebloatCatalog.Group.SYSTEM,
            isProtected = true,
            isRestorable = true,
            isChangedByApp = false,
            description = null
        )
        assertFalse(DebloatManager.remove(context, protectedApp))
        assertFalse(DebloatManager.disable(context, protectedApp))
    }

    @Test
    fun debloatCatalogClassifiesKnownAndUnknownPackages() {
        assertEquals(DebloatCatalog.Risk.RECOMMENDED, DebloatCatalog.classify("com.facebook.appmanager", true).risk)
        assertEquals(DebloatCatalog.Risk.RECOMMENDED, DebloatCatalog.classify("com.facebook.something", true).risk)
        assertEquals(DebloatCatalog.Risk.UNSAFE, DebloatCatalog.classify("com.google.android.gms", true).risk)
        assertEquals(DebloatCatalog.Risk.EXPERT, DebloatCatalog.classify("com.samsung.android.knox.foo", true).risk)
        assertEquals(DebloatCatalog.Risk.UNSAFE, DebloatCatalog.classify("com.unknown.system.thing", true).risk)
        assertEquals(DebloatCatalog.Risk.RECOMMENDED, DebloatCatalog.classify("com.unknown.user.thing", false).risk)
    }

    @Test
    fun remoteListIsAbsentUntilItIsDownloaded() {
        DebloatRemoteList.clear(context)
        assertFalse(DebloatRemoteList.isAvailable(context))
        assertEquals(0, DebloatRemoteList.entryCount(context))
        assertEquals(0L, DebloatRemoteList.updatedAtMs(context))
        assertTrue(DebloatRemoteList.load(context, setOf("com.example.bloat")).isEmpty())
        assertTrue(DebloatRemoteList.load(context, emptySet()).isEmpty())
    }

    @Test
    fun debloatStoreRemembersAndForgetsRemovedPackages() {
        val store = DebloatStore(context)
        store.clearAll()
        store.record("com.example.bloat", "Bloat", true)
        store.record("com.example.other", "Other", false)

        assertEquals(2, store.all().size)
        assertEquals("Bloat", store.labelFor("com.example.bloat"))
        assertTrue(store.all().first { it.packageName == "com.example.bloat" }.removed)
        assertFalse(store.all().first { it.packageName == "com.example.other" }.removed)

        store.forget("com.example.bloat")
        assertEquals(1, store.all().size)
        store.clearAll()
        assertTrue(store.all().isEmpty())
    }
}
