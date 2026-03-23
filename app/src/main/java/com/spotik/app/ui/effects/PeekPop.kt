package com.spotik.app.ui.effects

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * "Peek & Pop" press effect — scales up and lifts composable on touch.
 * Works alongside [clickable] thanks to `requireUnconsumed = false`.
 *
 * @param scaleTo  target scale (1.08 = 8 % bigger)
 * @param elevationTo  shadow elevation in px while pressed
 */
@Composable
fun Modifier.peekPop(
    scaleTo: Float = 1.08f,
    elevationTo: Float = 10f,
): Modifier {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleTo else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "peekScale",
    )
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) elevationTo else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "peekElev",
    )

    return this
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                waitForUpOrCancellation()
                isPressed = false
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            shadowElevation = elevation
        }
}

