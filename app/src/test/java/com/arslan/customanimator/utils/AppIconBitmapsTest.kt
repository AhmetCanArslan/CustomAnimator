package com.arslan.customanimator.utils

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AppIconBitmapsTest {

    private fun icon(width: Int, height: Int): Drawable = ShapeDrawable().apply {
        intrinsicWidth = width
        intrinsicHeight = height
    }

    @Test
    fun smallIconsKeepTheirSize() {
        val bitmap = AppIconBitmaps.rasterize(icon(96, 48))
        assertEquals(96, bitmap.width)
        assertEquals(48, bitmap.height)
    }

    @Test
    fun oversizedIconsAreScaledDownKeepingTheirShape() {
        val bitmap = AppIconBitmaps.rasterize(icon(1024, 512))
        assertEquals(192, bitmap.width)
        assertEquals(96, bitmap.height)
    }

    @Test
    fun iconsWithoutAnIntrinsicSizeStillRender() {
        val bitmap = AppIconBitmaps.rasterize(ColorDrawable(0))
        assertEquals(1, bitmap.width)
        assertEquals(1, bitmap.height)
    }

    @Test
    fun loadedIconIsServedFromTheCacheAfterwards() {
        val drawable = icon(64, 64)
        assertNull(AppIconBitmaps.cached(drawable))
        val loaded = runBlocking { AppIconBitmaps.load(drawable) }
        assertSame(loaded, AppIconBitmaps.cached(drawable))
        assertSame(loaded, runBlocking { AppIconBitmaps.load(drawable) })
    }

    @Test
    fun unknownPackageHasNoIcon() {
        val context: Context = ApplicationProvider.getApplicationContext()
        assertNull(runBlocking { AppIconBitmaps.load(context, "com.example.missing") })
        assertNull(AppIconBitmaps.cached("com.example.missing"))
    }

    @Test
    fun packageIconIsServedFromTheCacheUntilEvicted() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val packageName = "com.example.icon"
        shadowOf(context.packageManager).setApplicationIcon(packageName, icon(64, 64))
        val loaded = runBlocking { AppIconBitmaps.load(context, packageName) }
        assertNotNull(loaded)
        assertSame(loaded, AppIconBitmaps.cached(packageName))
        AppIconBitmaps.evict(packageName)
        assertNull(AppIconBitmaps.cached(packageName))
    }
}
