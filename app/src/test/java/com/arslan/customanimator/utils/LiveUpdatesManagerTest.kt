package com.arslan.customanimator.utils

import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LiveUpdatesManagerTest {

    private val all = LiveUpdatesManager.Options(progress = true, navigation = true)

    private fun source(
        category: String? = null,
        flags: Int = 0,
        progress: Int = 0,
        progressMax: Int = 0,
        indeterminate: Boolean = false,
        inNavigationApps: Boolean = false
    ) = LiveUpdatesManager.Source(category, flags, progress, progressMax, indeterminate, inNavigationApps)

    @Test
    fun progressNotificationsBecomeProgressUpdates() {
        assertEquals(LiveUpdatesManager.Kind.PROGRESS, LiveUpdatesManager.kindOf(source(progress = 3, progressMax = 10), all))
        assertEquals(LiveUpdatesManager.Kind.PROGRESS, LiveUpdatesManager.kindOf(source(indeterminate = true), all))
        assertNull(LiveUpdatesManager.kindOf(source(), all))
        assertNull(LiveUpdatesManager.kindOf(source(progressMax = 10), all.copy(progress = false)))
    }

    @Test
    fun navigationNeedsAnOngoingNotificationFromANavigationSource() {
        val ongoing = Notification.FLAG_ONGOING_EVENT
        assertEquals(
            LiveUpdatesManager.Kind.NAVIGATION,
            LiveUpdatesManager.kindOf(source(category = Notification.CATEGORY_NAVIGATION, flags = ongoing), all)
        )
        assertEquals(
            LiveUpdatesManager.Kind.NAVIGATION,
            LiveUpdatesManager.kindOf(source(flags = ongoing, inNavigationApps = true), all)
        )
        assertNull(LiveUpdatesManager.kindOf(source(category = Notification.CATEGORY_NAVIGATION), all))
        assertNull(LiveUpdatesManager.kindOf(source(flags = ongoing), all))
        assertNull(
            LiveUpdatesManager.kindOf(
                source(category = Notification.CATEGORY_NAVIGATION, flags = ongoing),
                all.copy(navigation = false)
            )
        )
    }

    @Test
    fun groupSummariesAndAlreadyPromotedNotificationsAreSkipped() {
        val summary = source(flags = Notification.FLAG_GROUP_SUMMARY, progressMax = 10)
        val promoted = source(flags = Notification.FLAG_PROMOTED_ONGOING, progressMax = 10)
        assertNull(LiveUpdatesManager.kindOf(summary, all))
        assertNull(LiveUpdatesManager.kindOf(promoted, all))
    }

    @Test
    fun percentIsClampedToTheRange() {
        assertEquals(30, LiveUpdatesManager.percentOf(source(progress = 3, progressMax = 10)))
        assertEquals(100, LiveUpdatesManager.percentOf(source(progress = 20, progressMax = 10)))
        assertEquals(0, LiveUpdatesManager.percentOf(source(progress = -5, progressMax = 10)))
        assertEquals(0, LiveUpdatesManager.percentOf(source(progress = 5)))
        assertEquals(50, LiveUpdatesManager.percentOf(source(progress = Int.MAX_VALUE / 2, progressMax = Int.MAX_VALUE - 1)))
    }

    @Test
    fun settingsRoundTrip() {
        val context: Context = ApplicationProvider.getApplicationContext()
        assertFalse(LiveUpdatesManager.isEnabled(context))
        assertTrue(LiveUpdatesManager.isProgressEnabled(context))
        assertTrue(LiveUpdatesManager.isNavigationEnabled(context))
        assertFalse(LiveUpdatesManager.isHideOriginalEnabled(context))

        LiveUpdatesManager.setEnabled(context, true)
        LiveUpdatesManager.setProgressEnabled(context, false)
        LiveUpdatesManager.setHideOriginalEnabled(context, true)
        assertTrue(LiveUpdatesManager.isEnabled(context))
        assertFalse(LiveUpdatesManager.isProgressEnabled(context))
        assertTrue(LiveUpdatesManager.isHideOriginalEnabled(context))

        assertEquals(setOf("com.maps"), LiveUpdatesManager.toggleNavigationApp(context, "com.maps"))
        assertEquals(setOf("com.maps"), LiveUpdatesManager.getNavigationApps(context))
        assertTrue(LiveUpdatesManager.toggleNavigationApp(context, "com.maps").isEmpty())
    }
}
