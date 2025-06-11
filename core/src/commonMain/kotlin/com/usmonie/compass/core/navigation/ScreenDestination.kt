package com.usmonie.compass.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.usmonie.compass.core.NavigationResult
import com.usmonie.compass.core.SharedElement
import com.usmonie.compass.core.randomUUID
import com.usmonie.compass.core.navigation.ScreenId

/**
 * Base class representing a destination screen in the navigation system.
 * Provides clearer lifecycle management and simplified transitions.
 */
@Immutable
public abstract class ScreenDestination(
    public val id: ScreenId,
    public val storeInBackStack: Boolean = true
) {
    /**
     * Unique identifier for this instance of the screen
     */
    public val uuid: String = randomUUID()

    /**
     * Animation for entering this screen when navigating forward
     */
    public open val enterTransition: AnimatedContentTransitionScope<ScreenDestination>.() -> EnterTransition =
        {
            EnterTransition.None
        }

    /**
     * Animation for exiting this screen when navigating forward
     */
    public open val exitTransition: AnimatedContentTransitionScope<ScreenDestination>.() -> ExitTransition =
        {
            ExitTransition.None
        }

    /**
     * Animation for entering this screen when navigating backward
     */
    public open val popEnterTransition: AnimatedContentTransitionScope<ScreenDestination>.() -> EnterTransition =
        {
            EnterTransition.None
        }

    /**
     * Animation for exiting this screen when navigating backward
     */
    public open val popExitTransition: AnimatedContentTransitionScope<ScreenDestination>.() -> ExitTransition =
        {
            ExitTransition.None
        }

    /**
     * Shared elements for transitions between screens
     */
    public open val sharedElements: List<SharedElement> = emptyList()

    /**
     * Handler for screen results
     */
    public open val resultHandler: ((NavigationResult) -> Unit)? = null

    /**
     * Gesture handler for back gestures
     */
    public open val gestureHandler: GestureHandler = GestureHandler.None

    /**
     * Z-index for layering screens during transitions
     */
    internal var zIndex: Float = 0f

    /**
     * The content to be displayed
     */
    @Composable
    public abstract fun Content()

    /**
     * Called when this screen is about to be entered
     */
    public open fun onEnter() {}

    /**
     * Called when this screen is about to be exited
     */
    public open fun onExit() {}

    /**
     * Called when this screen instance is being removed and will not be reused
     */
    public open fun onCleared() {}

    /**
     * Called when receiving a navigation result
     */
    public open suspend fun onResult(result: NavigationResult) {
        resultHandler?.invoke(result)
    }

    /**
     * Helper function to create shared elements for this screen
     */
    protected fun createSharedElement(
        key: String,
        targetScreenId: ScreenId
    ): SharedElement = SharedElement(
        key = key,
        screenFromId = id,
        screenToId = targetScreenId
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScreenDestination) return false

        if (id != other.id) return false
        if (storeInBackStack != other.storeInBackStack) return false
        if (uuid != other.uuid) return false
        if (sharedElements != other.sharedElements) return false
        if (gestureHandler != other.gestureHandler) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + storeInBackStack.hashCode()
        result = 31 * result + uuid.hashCode()
        result = 31 * result + sharedElements.hashCode()
        result = 31 * result + gestureHandler.hashCode()
        return result
    }
}

/**
 * Configuration for gesture handling on screens
 */
public sealed class GestureHandler {
    /**
     * No gesture handling for this screen
     */
    public data object None : GestureHandler()

    /**
     * Handle back gestures with specified edge padding
     */
    public data class Handling(val edgePadding: Float) : GestureHandler()
}