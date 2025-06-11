package com.usmonie.compass.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.coroutines.flow.Flow

/**
 * JVM/Desktop implementation of PredictiveBackHandler using keyboard events
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
public actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (Flow<NavigationGesture>) -> Boolean,
    onBackPressed: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnBackPressed by rememberUpdatedState(onBackPressed)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current

    val screenWidth = remember(windowInfo.containerSize.width) {
        with(density) { windowInfo.containerSize.width.toDp().value }
    }

    val modifier = if (enabled) {
        Modifier
            .focusProperties { canFocus = true }
            .onKeyEvent { keyEvent ->
                when {
                    keyEvent.type == KeyEventType.KeyDown &&
                            (keyEvent.key == Key.Escape || keyEvent.key == Key.Backspace) -> {
                        // Create a simple back processor for keyboard events
                        val backProcessor = BackProcessor(
                            scope = scope,
                            isPredictive = false,
                            onBack = currentOnBack
                        )

                        // Send immediate back gesture for keyboard
                        backProcessor.send(
                            NavigationGesture.Start(
                                positionX = screenWidth / 2f,
                                positionY = 0f,
                                screenWidth = screenWidth,
                                edge = GestureEdge.LEFT_TO_RIGHT
                            )
                        )
                        backProcessor.send(NavigationGesture.End(screenWidth))
                        backProcessor.close()

                        currentOnBackPressed()
                        true
                    }

                    else -> false
                }
            }
    } else {
        Modifier
    }

    Box(modifier = modifier, content = content)
}