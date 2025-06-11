package com.usmonie.compass.core.navigation

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.flow.Flow

/**
 * iOS implementation of PredictiveBackHandler using native gesture detection
 */
@Composable
public actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (Flow<NavigationGesture>) -> Boolean,
    onBackPressed: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val currentOnBack by rememberUpdatedState(onBack)
    val scope = rememberCoroutineScope()

    val modifier = if (enabled) {
        Modifier.pointerInput(Unit) {
            var backProcessor: BackProcessor? = null

            detectHorizontalDragGestures(
                onDragStart = { offset ->
                    backProcessor?.cancel()
                    backProcessor = BackProcessor(
                        scope = scope,
                        isPredictive = true,
                        onBack = currentOnBack
                    )
                    backProcessor?.send(
                        NavigationGesture.Start(
                            positionX = offset.x,
                            positionY = offset.y,
                            screenWidth = size.width.toFloat(),
                            edge = GestureEdge.LEFT_TO_RIGHT
                        )
                    )
                },
                onDragEnd = {
                    backProcessor?.send(NavigationGesture.End(size.width.toFloat()))
                    backProcessor?.close()
                },
                onDragCancel = {
                    backProcessor?.send(NavigationGesture.End(size.width.toFloat()))
                    backProcessor?.close()
                },
                onHorizontalDrag = { change, amount ->
                    change.consume()
                    val currentX = change.position.x.coerceIn(0f, size.width.toFloat())
                    backProcessor?.send(
                        NavigationGesture.Sliding(
                            positionX = currentX,
                            positionY = change.position.y,
                            screenWidth = size.width.toFloat(),
                            edge = GestureEdge.LEFT_TO_RIGHT
                        )
                    )
                }
            )
        }
    } else Modifier

    Box(modifier = modifier, content = content)
}