package com.arslan.customanimator.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDatabaseManagerTest {

    @Test
    fun parsesKeyValueLinesSortedByKey() {
        val entries = SettingsDatabaseManager.parse("volume_ring=5\nadb_enabled=1\nempty_value=\n")
        assertEquals(
            listOf(
                SettingEntry("adb_enabled", "1"),
                SettingEntry("empty_value", ""),
                SettingEntry("volume_ring", "5")
            ),
            entries
        )
    }

    @Test
    fun keepsEqualsSignsInsideValues() {
        val entries = SettingsDatabaseManager.parse("activity_manager_constants=max_cached_processes=32,background_settle_time=60000")
        assertEquals(listOf(SettingEntry("activity_manager_constants", "max_cached_processes=32,background_settle_time=60000")), entries)
    }

    @Test
    fun joinsContinuationLinesOntoThePreviousValue() {
        val entries = SettingsDatabaseManager.parse("first=line one\nline two\n second part=x\nsecond=2")
        assertEquals(
            listOf(
                SettingEntry("first", "line one\nline two\n second part=x"),
                SettingEntry("second", "2")
            ),
            entries
        )
    }

    @Test
    fun ignoresLeadingGarbageAndEmptyOutput() {
        assertTrue(SettingsDatabaseManager.parse("").isEmpty())
        assertEquals(listOf(SettingEntry("a", "1")), SettingsDatabaseManager.parse("no separator here\na=1"))
    }

    @Test
    fun rejectsKeysWithWhitespaceOrEquals() {
        assertTrue(SettingsDatabaseManager.isValidKey("screen_off_timeout"))
        assertTrue(SettingsDatabaseManager.isValidKey("vendor.key:with-dots.1"))
        assertFalse(SettingsDatabaseManager.isValidKey(""))
        assertFalse(SettingsDatabaseManager.isValidKey("two words"))
        assertFalse(SettingsDatabaseManager.isValidKey("a=b"))
    }

    @Test
    fun filtersByKeyOrValueIgnoringCase() {
        val entries = listOf(SettingEntry("screen_brightness", "128"), SettingEntry("font_scale", "1.3"), SettingEntry("theme", "Screen"))
        assertEquals(entries, SettingsDatabaseManager.filter(entries, "  "))
        assertEquals(listOf(entries[0], entries[2]), SettingsDatabaseManager.filter(entries, "SCREEN"))
        assertEquals(listOf(entries[1]), SettingsDatabaseManager.filter(entries, "1.3"))
    }
}
