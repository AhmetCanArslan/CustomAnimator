package com.arslan.customanimator.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
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
        assertEquals(0, manager.applyAll())
        assertEquals(1, manager.getConfigs().size)
    }
}
