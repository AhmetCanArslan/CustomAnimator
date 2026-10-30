package com.riyadmondol2006.customanimator.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.riyadmondol2006.customanimator.ActionRow
import com.riyadmondol2006.customanimator.InfoCard
import com.riyadmondol2006.customanimator.InfoNote
import com.riyadmondol2006.customanimator.NavigationRow
import com.riyadmondol2006.customanimator.QuickActionRow
import com.riyadmondol2006.customanimator.R
import com.riyadmondol2006.customanimator.SetupNudgeCard
import com.riyadmondol2006.customanimator.ToggleRow
import com.riyadmondol2006.customanimator.ui.LayoutHarness.assertFitsEveryScreenSize
import org.junit.Rule
import org.junit.Test
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun string(id: Int) = context.getString(id)

    @Test
    fun sharedRowsSurviveNarrowScreensAndHugeFonts() {
        composeTestRule.assertFitsEveryScreenSize {
                    Card {
                        Column {
                            ToggleRow(
                                icon = Icons.Filled.Speed,
                                title = string(R.string.app_threading),
                                description = string(R.string.app_threading_disclaimer),
                                checked = true,
                                onCheckedChange = {}
                            )
                            NavigationRow(
                                icon = Icons.Filled.Bolt,
                                title = string(R.string.auto_force_stop),
                                description = string(R.string.auto_force_stop_desc),
                                onClick = {}
                            )
                            QuickActionRow(
                                icon = Icons.Filled.Bolt,
                                title = string(R.string.clear_all_app_caches),
                                description = string(R.string.clear_all_app_caches_desc),
                                enabled = true,
                                isRunning = false,
                                onClick = {}
                            )
                            ActionRow(
                                icon = Icons.Filled.Bolt,
                                title = string(R.string.app_threading),
                                description = string(R.string.app_threading_desc),
                                buttonLabel = string(R.string.apply_settings),
                                enabled = true,
                                onClick = {}
                            )
                        }
                    }
                }
    }

    @Test
    fun infoSurfacesSurviveNarrowScreensAndHugeFonts() {
        composeTestRule.assertFitsEveryScreenSize {
                    Column {
                        InfoCard(
                            dismissKey = "layout_probe_card",
                            texts = listOf(
                                string(R.string.game_mode_info),
                                string(R.string.game_mode_fixed_performance)
                            )
                        )
                        Card {
                            InfoNote(
                                text = string(R.string.hwui_restart_note),
                                dismissKey = "layout_probe_note"
                            )
                        }
                        SetupNudgeCard(
                            message = string(R.string.developer_needs_shizuku),
                            onOpenSetup = {}
                        )
                    }
                }
    }
}
