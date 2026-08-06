package com.arslan.customanimator.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.IntOffset

object Motion {
    val emphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val standardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val decelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    const val durationFast = 150
    const val durationMedium = 280
    const val durationSlow = 450

    fun <T> bouncy(): SpringSpec<T> = spring(
        dampingRatio = 0.62f,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> snappy(): SpringSpec<T> = spring(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMedium
    )

    fun <T> gentle(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun offsetSpring(): FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessMediumLow
    )

    fun emphasized(durationMillis: Int = durationMedium) =
        tween<Float>(durationMillis = durationMillis, easing = emphasizedEasing)
}

fun AnimatedContentTransitionScope<*>.horizontalPagerTransition(forward: Boolean): ContentTransform {
    val direction = if (forward) 1 else -1
    return (slideInHorizontally(Motion.offsetSpring()) { width -> direction * width / 3 } +
        fadeIn(tween(Motion.durationMedium, easing = Motion.emphasizedEasing)) +
        scaleIn(tween(Motion.durationMedium, easing = Motion.emphasizedEasing), initialScale = 0.94f)
        ) togetherWith (
        slideOutHorizontally(Motion.offsetSpring()) { width -> -direction * width / 6 } +
            fadeOut(tween(Motion.durationFast)) +
            scaleOut(tween(Motion.durationMedium, easing = Motion.emphasizedEasing), targetScale = 0.96f)
        )
}

fun AnimatedContentTransitionScope<*>.forwardNavTransition(): ContentTransform =
    (slideInHorizontally(Motion.offsetSpring()) { width -> width / 2 } +
        fadeIn(tween(Motion.durationMedium))) togetherWith
        (fadeOut(tween(Motion.durationFast)) +
            scaleOut(tween(Motion.durationMedium), targetScale = 0.92f))

@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.965f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = Motion.bouncy(),
        label = "pressScale"
    )
    return this.scale(scale)
}

@Composable
fun rememberInteraction(): MutableInteractionSource = remember { MutableInteractionSource() }
