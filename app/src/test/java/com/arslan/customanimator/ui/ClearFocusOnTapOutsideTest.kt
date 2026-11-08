package com.arslan.customanimator.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.arslan.customanimator.ui.components.clearFocusOnTapOutside
import com.arslan.customanimator.ui.theme.CustomAnimatorTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClearFocusOnTapOutsideTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private var clicks = 0

    private fun setContent() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
        ActivityScenario.launch(ComponentActivity::class.java).onActivity { activity ->
            activity.setContent {
                CustomAnimatorTheme {
                    Column(modifier = Modifier.fillMaxSize().clearFocusOnTapOutside()) {
                        OutlinedTextField(value = "", onValueChange = {}, modifier = Modifier.testTag(FIRST))
                        OutlinedTextField(value = "", onValueChange = {}, modifier = Modifier.testTag(SECOND))
                        Button(onClick = { clicks++ }, modifier = Modifier.testTag(BUTTON)) { Text("Button") }
                        Text("Blank", modifier = Modifier.height(80.dp).testTag(BLANK))
                    }
                }
            }
        }
    }

    @Test
    fun tappingAButtonClearsFocusAndStillClicks() {
        setContent()
        composeTestRule.onNodeWithTag(FIRST).performClick().assertIsFocused()
        composeTestRule.onNodeWithTag(BUTTON).performClick()
        composeTestRule.onNodeWithTag(FIRST).assertIsNotFocused()
        assertEquals(1, clicks)
    }

    @Test
    fun tappingBlankSpaceClearsFocus() {
        setContent()
        composeTestRule.onNodeWithTag(SECOND).performClick().assertIsFocused()
        composeTestRule.onNodeWithTag(BLANK).performClick()
        composeTestRule.onNodeWithTag(SECOND).assertIsNotFocused()
        composeTestRule.onNodeWithTag(FIRST).assertIsNotFocused()
    }

    @Test
    fun tappingTheFocusedFieldKeepsFocus() {
        setContent()
        composeTestRule.onNodeWithTag(FIRST).performClick().assertIsFocused()
        composeTestRule.onNodeWithTag(FIRST).performClick().assertIsFocused()
    }

    @Test
    fun tappingAnotherFieldMovesFocusToIt() {
        setContent()
        composeTestRule.onNodeWithTag(FIRST).performClick().assertIsFocused()
        composeTestRule.onNodeWithTag(SECOND).performClick().assertIsFocused()
        composeTestRule.onNodeWithTag(FIRST).assertIsNotFocused()
    }

    private companion object {
        const val FIRST = "first"
        const val SECOND = "second"
        const val BUTTON = "button"
        const val BLANK = "blank"
    }
}
