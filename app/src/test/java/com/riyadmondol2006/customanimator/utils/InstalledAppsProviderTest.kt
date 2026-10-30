package com.riyadmondol2006.customanimator.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class InstalledAppsProviderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() = InstalledAppsProvider.clearLaunchableAppsCache()

    @After
    fun tearDown() = InstalledAppsProvider.clearLaunchableAppsCache()

    private fun install(packageName: String, sourceDir: String) {
        val application = ApplicationInfo().apply {
            this.packageName = packageName
            this.sourceDir = sourceDir
            name = packageName
        }
        val activity = ActivityInfo().apply {
            this.packageName = packageName
            name = "$packageName.Main"
            applicationInfo = application
            exported = true
        }
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val packageManager = shadowOf(context.packageManager)
        packageManager.removeResolveInfosForIntent(launcher, packageName)
        packageManager.addResolveInfoForIntent(launcher, ResolveInfo().apply { activityInfo = activity })
    }

    @Test
    fun nothingIsCachedBeforeTheFirstLoad() {
        assertNull(InstalledAppsProvider.cachedLaunchableApps())
    }

    @Test
    fun unchangedAppsKeepTheirEntryAcrossLoads() {
        install("com.example.one", "/data/app/one/base.apk")

        val first = InstalledAppsProvider.getLaunchableApps(context)
        val second = InstalledAppsProvider.getLaunchableApps(context)

        assertEquals(listOf("com.example.one"), first.map { it.packageName })
        assertSame(first.single(), second.single())
        assertSame(second, InstalledAppsProvider.cachedLaunchableApps())
    }

    @Test
    fun reinstalledAppIsDescribedAgain() {
        install("com.example.one", "/data/app/one/base.apk")
        val first = InstalledAppsProvider.getLaunchableApps(context)

        install("com.example.one", "/data/app/one-2/base.apk")
        val second = InstalledAppsProvider.getLaunchableApps(context)

        assertNotSame(first.single(), second.single())
    }

    @Test
    fun clearingDropsTheCachedList() {
        install("com.example.one", "/data/app/one/base.apk")
        InstalledAppsProvider.getLaunchableApps(context)

        InstalledAppsProvider.clearLaunchableAppsCache()

        assertNull(InstalledAppsProvider.cachedLaunchableApps())
    }
}
