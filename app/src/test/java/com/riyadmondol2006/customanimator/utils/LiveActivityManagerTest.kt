package com.riyadmondol2006.customanimator.utils

import android.content.ComponentName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LiveActivityManagerTest {

    @Test
    fun prefersTopResumedActivityOverTaskResumedActivities() {
        val output = """
                mResumedActivity: ActivityRecord{1a2b u0 com.other/.SideActivity t12}
              topResumedActivity=ActivityRecord{3c4d u0 com.example.app/.ui.MainActivity t15}
        """.trimIndent()

        assertEquals(
            ComponentName("com.example.app", "com.example.app.ui.MainActivity"),
            LiveActivityManager.parseResumedActivity(output)
        )
    }

    @Test
    fun keepsFullyQualifiedClassNames() {
        val output = "  mResumedActivity: ActivityRecord{1a2b u0 com.example/org.lib.ExternalActivity t3}"

        assertEquals(
            ComponentName("com.example", "org.lib.ExternalActivity"),
            LiveActivityManager.parseResumedActivity(output)
        )
    }

    @Test
    fun returnsNullWithoutActivityLine() {
        assertNull(LiveActivityManager.parseResumedActivity(""))
        assertNull(LiveActivityManager.parseResumedActivity("Shizuku permission not granted"))
    }
}
