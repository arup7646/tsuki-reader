package com.alix.tsuki.ui.screens.reader.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    minScale: Float = 1f,
    maxScale: Float = 4f,
    onTapLeft: () -> Unit = {},
    onTapRight: () -> Unit = {},
    onTapCenter: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)
        scale = newScale
        if (newScale > 1f) {
            val maxOffsetX = (newScale - 1f) * 600f
            val maxOffsetY = (newScale - 1f) * 900f
            offset = Offset(
                x = (offset.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                y = (offset.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
            )
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .transformable(state = transformState)
            .pointerInput(scale) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.2f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            // Center zoom on tap location
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            offset = Offset(
                                x = (centerX - tapOffset.x) * 1.5f,
                                y = (centerY - tapOffset.y) * 1.5f
                            )
                        }
                    },
                    onTap = { tapOffset ->
                        val screenWidth = size.width
                        val leftThreshold = screenWidth * 0.22f
                        val rightThreshold = screenWidth * 0.78f

                        when {
                            tapOffset.x < leftThreshold -> onTapLeft()
                            tapOffset.x > rightThreshold -> onTapRight()
                            else -> onTapCenter()
                        }
                    }
                )
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
    ) {
        content()
    }
}
