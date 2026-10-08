package com.arslan.customanimator.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ButtonActionsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun quickActionKeysAreUniqueAndResolveBack() {
        val keys = ButtonActions.Quick.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        ButtonActions.Quick.entries.forEach { assertEquals(it, ButtonActions.Quick.fromKey(it.key)) }
        assertNull(ButtonActions.Quick.fromKey("teleport"))
    }

    @Test
    fun everyQuickActionAndToggleHasAReadableLabel() {
        ButtonActions.Quick.entries.forEach { quick ->
            val label = ButtonActions.label(context, ButtonAction(ButtonAction.Type.QUICK, quick.key))
            assertEquals(context.getString(quick.nameRes), label)
            assertTrue(label.isNotBlank())
        }
        ToggleTilePresets.all.forEach { preset ->
            val label = ButtonActions.label(context, ButtonAction(ButtonAction.Type.TOGGLE, preset.key))
            assertEquals(context.getString(preset.nameRes), label)
        }
    }

    @Test
    fun commandAndUnknownActionsAreLabelledByTheirValue() {
        assertEquals("svc wifi disable", ButtonActions.label(context, ButtonAction(ButtonAction.Type.COMMAND, "svc wifi disable")))
        assertEquals("com.example.missing", ButtonActions.label(context, ButtonAction(ButtonAction.Type.APP, "com.example.missing")))
        assertEquals("teleport", ButtonActions.label(context, ButtonAction(ButtonAction.Type.QUICK, "teleport")))
    }

    @Test
    fun shizukuBackedActionsFailSafelyWithoutShizuku() {
        val shizukuBacked = listOf(
            ButtonAction(ButtonAction.Type.QUICK, ButtonActions.Quick.SCREENSHOT.key),
            ButtonAction(ButtonAction.Type.QUICK, ButtonActions.Quick.NOTIFICATIONS.key),
            ButtonAction(ButtonAction.Type.QUICK, ButtonActions.Quick.QUICK_SETTINGS.key),
            ButtonAction(ButtonAction.Type.QUICK, ButtonActions.Quick.RECENT_APPS.key),
            ButtonAction(ButtonAction.Type.QUICK, ButtonActions.Quick.FREE_MEMORY.key),
            ButtonAction(ButtonAction.Type.QUICK, ButtonActions.Quick.RESTART_SYSTEM_UI.key),
            ButtonAction(ButtonAction.Type.TOGGLE, ToggleTilePresets.all.first().key),
            ButtonAction(ButtonAction.Type.COMMAND, "echo hi")
        )
        shizukuBacked.forEach { assertFalse(ButtonActions.perform(context, it)) }
    }

    @Test
    fun unknownOrEmptyActionsFail() {
        assertFalse(ButtonActions.perform(context, ButtonAction(ButtonAction.Type.QUICK, "teleport")))
        assertFalse(ButtonActions.perform(context, ButtonAction(ButtonAction.Type.TOGGLE, "teleport")))
        assertFalse(ButtonActions.perform(context, ButtonAction(ButtonAction.Type.APP, "com.example.missing")))
        assertFalse(ButtonActions.perform(context, ButtonAction(ButtonAction.Type.COMMAND, "  ")))
    }
}
