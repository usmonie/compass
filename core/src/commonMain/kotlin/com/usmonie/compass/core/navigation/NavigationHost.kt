package com.usmonie.compass.core.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.Flow

/**
 * CompositionLocal containing the current NavController
 */
public val LocalNavController: androidx.compose.runtime.ProvidableCompositionLocal<NavController> =
    staticCompositionLocalOf {
        error("No NavController provided in composition hierarchy")
    }

/**
 * Helper to track navigation state preservation
 */
private object NavigationStateTracker {
    private val screenInstances = mutableMapOf<String, Int>()
    private var instanceCounter = 1

    fun logNavigationScreenCreation(screenId: String, uuid: String) {
        val instanceId = instanceCounter++
        val key = "$screenId:$uuid"
        screenInstances[key] = instanceId
        println("NAVIGATION: Screen CREATED - $screenId (uuid: $uuid) - Instance #$instanceId")
    }

    fun logNavigationScreenReused(screenId: String, uuid: String) {
        val key = "$screenId:$uuid"
        val instanceId = screenInstances[key]
        if (instanceId != null) {
            println("NAVIGATION: Screen REUSED - $screenId (uuid: $uuid) - Instance #$instanceId")
        } else {
            logNavigationScreenCreation(screenId, uuid)
        }
    }

    fun logNavAnimationState(state: NavigationAnimationState) {
        println("NAVIGATION: Animation state changed to $state")
    }
}

/**
 * Main composable that hosts the navigation system.
 *
 * @param navController The NavController managing the navigation logic
 * @param modifier Optional Modifier for the root Box
 * @param gestureEnabled Whether back gestures are enabled
 * @param backgroundColor Background color/brush for the host
 */
@Composable
public fun NavigationHost(
    navController: NavController,
    modifier: Modifier = Modifier,
    gestureEnabled: Boolean = true,
    backgroundColor: Brush = Brush.verticalGradient(listOf(Color.White)),
) {
    CompositionLocalProvider(LocalNavController provides navController) {
        val currentDestination by navController.currentDestination.collectAsState()
        val canGoBack by navController.canGoBack.collectAsState()
        val animState by navController.navAnimationState.collectAsState()

        // Track animation state changes
        androidx.compose.runtime.LaunchedEffect(animState) {
            NavigationStateTracker.logNavAnimationState(animState)
        }

        BackGestureHandler(
            enabled = canGoBack && gestureEnabled,
            onBack = { flow -> navController.handleBackGesture(flow) },
            onBackPressed = { navController.popBackStack() },
        ) {
            Box(modifier = modifier.background(backgroundColor)) {
                currentDestination?.let { current ->
                    // Track screen state preservations
                    NavigationStateTracker.logNavigationScreenReused(current.id.id, current.uuid)

                    AnimatedContent(
                        targetState = current,
                        transitionSpec = { getTransition(animState) },
                        modifier = Modifier.fillMaxSize()
                    ) { destination ->
                        navController.SaveableStateProvider(destination) {
                            destination.Content()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Handler for back gestures that integrates with the navigation system
 */
@Composable
private fun BackGestureHandler(
    enabled: Boolean,
    onBack: suspend (Flow<NavigationGesture>) -> Boolean,
    onBackPressed: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    PredictiveBackHandler(
        enabled = enabled,
        onBack = onBack,
        onBackPressed = onBackPressed,
        content = content
    )
}

/**
 * Returns the appropriate transition based on navigation state
 */
private fun getTransition(animState: NavigationAnimationState): ContentTransform {
    return when (animState) {
        NavigationAnimationState.NONE ->
            fadeIn(tween(0)) togetherWith fadeOut(tween(0))

        NavigationAnimationState.NAVIGATING_FORWARD ->
            fadeIn(tween(300)) togetherWith fadeOut(tween(300))

        NavigationAnimationState.NAVIGATING_BACKWARD ->
            fadeIn(tween(300)) togetherWith fadeOut(tween(300))
    }
}

@Composable
public expect fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (Flow<NavigationGesture>) -> Boolean,
    onBackPressed: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
)