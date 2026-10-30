package com.riyadmondol2006.customanimator.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.riyadmondol2006.customanimator.data.ShortcutType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    fun everyQuickActionAndToggleBecomesALabelledEntryWithAnIcon() {
        ButtonActions.Quick.entries.forEach { quick ->
            val entry = ButtonActions.quickEntry(context, quick)
            assertEquals(ShortcutType.QUICK, entry.type)
            assertEquals(quick.key, entry.actionKey)
            assertEquals(context.getString(quick.nameRes), entry.label)
            assertEquals(quick.needsShizuku, entry.needsShizuku)
            assertFalse(entry.startsActivity)
            assertEquals(TerminalTileIcons.canonicalKey(quick.iconKey), quick.iconKey)
            assertNotNull(ButtonActions.iconRes(entry))
        }
        ToggleTilePresets.all.forEach { preset ->
            val entry = ButtonActions.toggleEntry(context, preset)
            assertEquals(ShortcutType.TOGGLE, entry.type)
            assertEquals(context.getString(preset.nameRes), entry.label)
            assertTrue(entry.needsShizuku)
            assertNotNull(ButtonActions.iconRes(entry))
        }
    }

    @Test
    fun everyQuickActionBelongsToAGroupThatIsNotEmpty() {
        ButtonActions.Group.entries.forEach { group ->
            assertTrue(ButtonActions.Quick.entries.any { it.group == group })
        }
    }

    @Test
    fun shizukuBackedActionsFailSafelyWithoutShizuku() {
        val shizukuBacked = ButtonActions.Quick.entries
            .filter { it.needsShizuku && it != ButtonActions.Quick.KEEP_SCREEN_ON }
            .map { ButtonActions.quickEntry(context, it) } +
            ButtonActions.toggleEntry(context, ToggleTilePresets.all.first())
        shizukuBacked.forEach { entry ->
            assertFalse(entry.label, runBlocking { ShortcutMaker.launch(context, entry) })
        }
    }

    @Test
    fun unknownActionsFail() {
        assertFalse(ButtonActions.runQuick(context, "teleport"))
        assertFalse(ButtonActions.runQuick(context, null))
        assertFalse(ButtonActions.toggle("teleport"))
        assertFalse(ButtonActions.toggle(null))
    }
}
