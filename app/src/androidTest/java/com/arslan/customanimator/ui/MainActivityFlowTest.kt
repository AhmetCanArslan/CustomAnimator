package com.arslan.customanimator.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arslan.customanimator.HomeScreen
import com.arslan.customanimator.HomeTab
import com.arslan.customanimator.MainActivity
import com.arslan.customanimator.R
import com.arslan.customanimator.utils.SettingsManager
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MainActivityFlowTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(object : ExternalResource() {
            override fun before() {
                SettingsManager.setLastScreen(context, HomeScreen.MAIN.name)
                SettingsManager.setLastTab(context, HomeTab.ANIMATION.name)
            }
        })
        .around(composeTestRule)

    private fun string(id: Int) = context.getString(id)

    private fun exists(text: String) =
        composeTestRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    private fun settle(steps: Int = 20) {
        repeat(steps) { composeTestRule.mainClock.advanceTimeBy(100) }
    }

    private fun clickIfPresent(id: Int) {
        if (exists(string(id))) {
            composeTestRule.onNodeWithText(string(id)).performClick()
            settle()
        }
    }

    private fun waitForText(id: Int, attempts: Int = 40): Boolean {
        repeat(attempts) {
            if (exists(string(id))) return true
            settle(5)
        }
        return exists(string(id))
    }

    @Before
    fun openTheHomeScreen() {
        composeTestRule.mainClock.autoAdvance = false
        settle()
        clickIfPresent(R.string.onboarding_skip)
        clickIfPresent(R.string.onboarding_disclaimer_consent)
        clickIfPresent(R.string.onboarding_setup_later)
        assertTrue("home screen never appeared", waitForText(R.string.nav_animation))
    }

    @Test
    fun everyTopTabOpensWithoutCrashing() {
        listOf(R.string.nav_animation, R.string.nav_width, R.string.optimizer_title, R.string.game_mode)
            .forEach { tab ->
                composeTestRule.onNodeWithText(string(tab)).performClick()
                settle()
                composeTestRule.onNodeWithText(string(tab)).assertIsDisplayed()
            }
    }

    @Test
    fun tabSelectionSurvivesRecreation() {
        composeTestRule.onNodeWithText(string(R.string.optimizer_title)).performClick()
        settle()
        composeTestRule.activityRule.scenario.recreate()
        assertTrue("top nav bar never came back", waitForText(R.string.optimizer_title))
        composeTestRule.onNodeWithText(string(R.string.optimizer_title)).assertIsDisplayed()
    }
}
