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
import kotlinx.coroutines.flow.Flow

/**
 * WASM-JS implementation of PredictiveBackHandler using keyboard events
 * Simplified version without browser history API (not fully supported in WASM yet)
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

    // Use a default screen width for WASM
    val screenWidth = remember { 1920f }

    // Handle keyboard events only for WASM
    val keyboardModifier = if (enabled) {
        Modifier
            .focusProperties { canFocus = true }
            .onKeyEvent { keyEvent ->
                when {
                    keyEvent.type == KeyEventType.KeyDown &&
                            (keyEvent.key == Key.Escape || keyEvent.key == Key.Backspace) -> {
                        val backProcessor = BackProcessor(
                            scope = scope,
                            isPredictive = false,
                            onBack = currentOnBack
                        )

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

    Box(modifier = keyboardModifier, content = content)
}