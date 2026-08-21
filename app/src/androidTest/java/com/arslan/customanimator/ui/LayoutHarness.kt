package com.arslan.customanimator.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.arslan.customanimator.ui.theme.CustomAnimatorTheme
import org.junit.Assert.assertTrue

object LayoutHarness {

    private const val ROOT_TAG = "layout_harness_root"

    private val narrowWidths = listOf(320.dp, 360.dp, 411.dp)
    private val fontScales = listOf(1.0f, 1.3f, 2.0f)
    private const val STRICT_FONT_SCALE_LIMIT = 1.3f

    fun ComposeContentTestRule.assertFitsEveryScreenSize(
        allowedTruncations: List<String> = emptyList(),
        content: @Composable () -> Unit
    ) {
        val width = mutableStateOf(narrowWidths.first())
        val fontScale = mutableFloatStateOf(fontScales.first())

        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale.floatValue)
            ) {
                CustomAnimatorTheme {
                    Box(modifier = Modifier.width(width.value).testTag(ROOT_TAG)) {
                        Box(modifier = Modifier.fillMaxWidth()) { content() }
                    }
                }
            }
        }

        narrowWidths.forEach { candidateWidth ->
            fontScales.forEach { candidateScale ->
                runOnUiThread {
                    width.value = candidateWidth
                    fontScale.floatValue = candidateScale
                }
                waitForIdle()
                val label = "width=$candidateWidth fontScale=$candidateScale"
                assertNothingOverflowsHorizontally(label)
                if (candidateScale <= STRICT_FONT_SCALE_LIMIT) {
                    assertNoTextIsTruncated(label, allowedTruncations)
                }
            }
        }
    }

    private fun SemanticsNode.flatten(): List<SemanticsNode> =
        listOf(this) + children.flatMap { it.flatten() }

    fun ComposeContentTestRule.assertNothingOverflowsHorizontally(label: String) {
        waitForIdle()
        val root = onNodeWithTag(ROOT_TAG, useUnmergedTree = true).fetchSemanticsNode()
        val rootBounds = root.boundsInRoot
        val offenders = root.flatten().filter { node ->
            val bounds = node.boundsInRoot
            bounds.width > 0f &&
                (bounds.left < rootBounds.left - 1f || bounds.right > rootBounds.right + 1f)
        }
        assertTrue(
            "$label: ${offenders.size} node(s) spill outside the ${rootBounds.width}px viewport: " +
                offenders.joinToString { it.config.toString().take(120) },
            offenders.isEmpty()
        )
    }

    fun ComposeContentTestRule.assertNoTextIsTruncated(
        label: String,
        allowedTruncations: List<String> = emptyList()
    ) {
        waitForIdle()
        val root = onNodeWithTag(ROOT_TAG, useUnmergedTree = true).fetchSemanticsNode()
        val truncated = root.flatten().mapNotNull { node ->
            if (!node.config.contains(SemanticsActions.GetTextLayoutResult)) return@mapNotNull null
            val action = node.config[SemanticsActions.GetTextLayoutResult]
            val results = mutableListOf<TextLayoutResult>()
            action.action?.invoke(results)
            val layout = results.firstOrNull() ?: return@mapNotNull null
            val ellipsized = (0 until layout.lineCount).any { layout.isLineEllipsized(it) }
            val lineCapped = layout.lineCount >= layout.layoutInput.maxLines
            val needsMoreWidth = layout.multiParagraph.maxIntrinsicWidth > layout.multiParagraph.width + 0.5f
            if (ellipsized || (lineCapped && needsMoreWidth)) {
                layout.layoutInput.text.text.take(60)
            } else {
                null
            }
        }
        val unexpected = truncated.filterNot { text -> allowedTruncations.any { text.startsWith(it.take(60)) } }
        assertTrue("$label: truncated text: $unexpected", unexpected.isEmpty())
    }

}
