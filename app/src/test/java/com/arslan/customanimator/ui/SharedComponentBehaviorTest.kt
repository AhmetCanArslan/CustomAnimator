package com.arslan.customanimator.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.arslan.customanimator.ActionRow
import com.arslan.customanimator.InfoCard
import com.arslan.customanimator.NavigationRow
import com.arslan.customanimator.R
import com.arslan.customanimator.ToggleRow
import com.arslan.customanimator.ui.theme.CustomAnimatorTheme
import com.arslan.customanimator.utils.InfoNoticeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SharedComponentBehaviorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun infoCardDisappearsWhenDismissedAndStaysDismissed() {
        val key = "behaviour_info_card"
        val message = context.getString(R.string.game_mode_info)

        composeTestRule.setContent {
            CustomAnimatorTheme {
                InfoCard(dismissKey = key, texts = listOf(message))
            }
        }

        composeTestRule.onNodeWithText(message).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.close)).performClick()
        composeTestRule.onNodeWithText(message).assertDoesNotExist()
        assertTrue(InfoNoticeManager.isDismissed(context, key))
    }

    @Test
    fun dismissedInfoNeverComesBackOnANewComposition() {
        val key = "behaviour_info_card_persisted"
        InfoNoticeManager.dismiss(context, key)
        val message = context.getString(R.string.app_threading_disclaimer)

        composeTestRule.setContent {
            CustomAnimatorTheme {
                InfoCard(dismissKey = key, texts = listOf(message))
            }
        }

        composeTestRule.onNodeWithText(message).assertDoesNotExist()
    }

    @Test
    fun dismissingOneNoticeKeepsTheOthersVisible() {
        InfoNoticeManager.dismiss(context, "behaviour_first")
        val second = context.getString(R.string.hwui_restart_note)

        composeTestRule.setContent {
            CustomAnimatorTheme {
                InfoCard(dismissKey = "behaviour_second", texts = listOf(second))
            }
        }

        composeTestRule.onNodeWithText(second).assertIsDisplayed()
    }

    @Test
    fun toggleRowReportsUserChanges() {
        var checked = false
        composeTestRule.setContent {
            var state by mutableStateOf(checked)
            CustomAnimatorTheme {
                ToggleRow(
                    icon = Icons.Filled.Bolt,
                    title = "Force GPU rendering",
                    description = "description",
                    checked = state,
                    onCheckedChange = {
                        state = it
                        checked = it
                    }
                )
            }
        }

        composeTestRule.onNode(isToggleable()).assertIsOff()
        composeTestRule.onNode(isToggleable()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isToggleable()).assertIsOn()
        assertTrue(checked)
    }

    @Test
    fun disabledRowsIgnoreTaps() {
        var navigationClicks = 0
        composeTestRule.setContent {
            CustomAnimatorTheme {
                NavigationRow(
                    icon = Icons.Filled.Bolt,
                    title = "Disabled row",
                    description = "description",
                    enabled = false,
                    onClick = { navigationClicks++ }
                )
            }
        }

        composeTestRule.onNodeWithText("Disabled row").performClick()
        composeTestRule.waitForIdle()
        assertEquals(0, navigationClicks)
    }

    @Test
    fun applyButtonIsDisabledUntilThereIsSomethingToApply() {
        var clicks = 0
        composeTestRule.setContent {
            CustomAnimatorTheme {
                ActionRow(
                    icon = Icons.Filled.Bolt,
                    title = context.getString(R.string.app_threading_reapply),
                    description = context.getString(R.string.app_threading_reapply_desc),
                    buttonLabel = context.getString(R.string.app_threading_apply_with_ad),
                    enabled = false,
                    onClick = { clicks++ }
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.app_threading_apply_with_ad))
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText(context.getString(R.string.app_threading_apply_with_ad)).performClick()
        composeTestRule.waitForIdle()
        assertEquals(0, clicks)
    }
}
