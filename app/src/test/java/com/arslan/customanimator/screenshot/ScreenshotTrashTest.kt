package com.arslan.customanimator.screenshot

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScreenshotTrashTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun trashCommandMarksExactlyOneMediaRow() {
        val command = ScreenshotActions.trashCommand(42L).toList()

        assertEquals(
            listOf(
                "content", "update",
                "--user", "0",
                "--uri", "content://media/external/images/media/42",
                "--bind", "is_trashed:i:1"
            ),
            command
        )
        assertFalse(command.contains("--where"))
        assertFalse(command.contains("delete"))
    }

    @Test
    fun trashCommandRejectsAnIdThatIsNotARow() {
        assertThrows(IllegalArgumentException::class.java) { ScreenshotActions.trashCommand(0L) }
        assertThrows(IllegalArgumentException::class.java) { ScreenshotActions.trashCommand(-1L) }
    }

    @Test
    fun nothingIsTrashedWithoutShizuku() {
        val item = ScreenshotItem(
            id = 42L,
            uri = Uri.parse("content://media/external/images/media/42"),
            name = "Screenshot_42.png",
            dateAdded = 0L
        )

        assertEquals(ScreenshotTrashResult.NO_SHIZUKU, ScreenshotActions.moveToTrash(context, item))
    }
}
