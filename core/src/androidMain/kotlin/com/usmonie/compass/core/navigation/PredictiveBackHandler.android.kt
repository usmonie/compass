package com.usmonie.compass.core.navigation

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.coroutines.flow.Flow

/**
 * Android implementation of PredictiveBackHandler using Android's BackEventCompat API
 */
@Composable
public actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (Flow<NavigationGesture>) -> Boolean,
    onBackPressed: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(content = content)

    val currentOnBack by rememberUpdatedState(onBack)
    val scope = rememberCoroutineScope()

    val screenWidth = LocalWindowInfo.current.containerSize.width.toFloat()

    val callback = remember {
        object : OnBackPressedCallback(enabled) {
            var backProcessor: BackProcessor? = null

            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                super.handleOnBackStarted(backEvent)
                backProcessor?.cancel()

                // Create a new back processor for this gesture
                backProcessor = BackProcessor(
                    scope = scope,
                    isPredictive = backEvent.progress > 0f || backEvent.touchX > 0f || backEvent.touchY > 0f,
                    onBack = currentOnBack
                )
            }

            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                super.handleOnBackProgressed(backEvent)

                backProcessor?.send(
                    NavigationGesture.Sliding(
                        positionX = backEvent.touchX,
                        positionY = backEvent.touchY,
                        screenWidth = screenWidth,
                        edge = if (backEvent.swipeEdge == BackEventCompat.EDGE_LEFT) {
                            GestureEdge.LEFT_TO_RIGHT
                        } else {
                            GestureEdge.RIGHT_TO_LEFT
                        }
                    )
                )
            }

            override fun handleOnBackPressed() {
                backProcessor?.let { processor ->
                    if (!processor.isPredictive) {
                        processor.cancel()
                        backProcessor = null
                    } else {
                        processor.send(NavigationGesture.End(screenWidth))
                    }
                }

                if (backProcessor == null) {
                    // Create a new processor for this button press
                    backProcessor = BackProcessor(
                        scope = scope,
                        isPredictive = false,
                        onBack = currentOnBack
                    )
                    onBackPressed()
                }

                backProcessor?.close()
            }

            override fun handleOnBackCancelled() {
                super.handleOnBackCancelled()
                backProcessor?.send(NavigationGesture.End(screenWidth))
            }
        }
    }

    LaunchedEffect(enabled) {
        callback.isEnabled = enabled
    }

    val backDispatcher = checkNotNull(LocalOnBackPressedDispatcherOwner.current) {
        "No OnBackPressedDispatcherOwner provided via LocalOnBackPressedDispatcherOwner"
    }.onBackPressedDispatcher

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, backDispatcher) {
        backDispatcher.addCallback(lifecycleOwner, callback)
        onDispose { callback.remove() }
    }
}