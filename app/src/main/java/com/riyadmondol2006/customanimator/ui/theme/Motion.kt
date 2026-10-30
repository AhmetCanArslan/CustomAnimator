package com.riyadmondol2006.customanimator.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

val AppMotionScheme: MotionScheme = MotionScheme.expressive()

object Motion {
    val emphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun expandIn(): EnterTransition =
        expandVertically(AppMotionScheme.defaultSpatialSpec()) + fadeIn(AppMotionScheme.defaultEffectsSpec())

    fun collapseOut(): ExitTransition =
        shrinkVertically(AppMotionScheme.fastSpatialSpec()) + fadeOut(AppMotionScheme.fastEffectsSpec())

    fun popIn(origin: TransformOrigin = TransformOrigin.Center): EnterTransition =
        scaleIn(AppMotionScheme.defaultSpatialSpec(), initialScale = 0.9f, transformOrigin = origin) +
            fadeIn(AppMotionScheme.defaultEffectsSpec())

    fun popOut(origin: TransformOrigin = TransformOrigin.Center): ExitTransition =
        scaleOut(AppMotionScheme.fastSpatialSpec(), targetScale = 0.96f, transformOrigin = origin) +
            fadeOut(AppMotionScheme.fastEffectsSpec())
}

fun AnimatedContentTransitionScope<*>.horizontalPagerTransition(forward: Boolean): ContentTransform {
    val direction = if (forward) 1 else -1
    val enter = slideInHorizontally(AppMotionScheme.defaultSpatialSpec()) { width -> direction * width / SHARED_AXIS_DIVISOR } +
        fadeIn(AppMotionScheme.defaultEffectsSpec())
    val exit = slideOutHorizontally(AppMotionScheme.fastSpatialSpec()) { width -> -direction * width / SHARED_AXIS_DIVISOR } +
        fadeOut(AppMotionScheme.fastEffectsSpec())
    return enter togetherWith exit
}

private const val SHARED_AXIS_DIVISOR = 6

@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
