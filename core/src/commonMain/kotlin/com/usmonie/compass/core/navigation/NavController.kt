package com.usmonie.compass.core.navigation

import androidx.collection.ObjectList
import androidx.collection.ScatterMap
import androidx.collection.emptyObjectList
import androidx.collection.emptyScatterMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.usmonie.compass.core.Extra
import com.usmonie.compass.core.GraphId
import com.usmonie.compass.core.NavigationResult
import com.usmonie.compass.core.SharedElement
import kotlinx.coroutines.flow.StateFlow

/**
 * Main controller for navigation operations.
 * Provides a simplified API for navigating between screens and managing the back stack.
 */
@Stable
public class NavController internal constructor(
    private val engine: NavigationEngine
) {
    /**
     * Current destination on the screen
     */
    public val currentDestination: StateFlow<ScreenDestination?> = engine.currentDestination

    /**
     * Previous destination on the back stack (if any)
     */
    public val previousDestination: StateFlow<ScreenDestination?> = engine.previousDestination

    /**
     * Whether navigation back is possible from the current state
     */
    public val canGoBack: StateFlow<Boolean> = engine.canGoBack

    /**
     * Current navigation animation state
     */
    public val navAnimationState: StateFlow<NavigationAnimationState> =
        engine.navigationAnimationState

    // Track state restorations per screen
    private val stateRestorations = mutableMapOf<String, Int>()

    /**
     * Navigates to a screen by ID
     *
     * @param screenId ID of the destination screen
     * @param options Navigation options like animations, storing in backstack, etc.
     * @return true if navigation was successful
     */
    public fun navigate(
        screenId: ScreenId,
        options: NavOptions = NavOptions()
    ): Boolean = engine.navigateTo(
        screenId = screenId,
        storeInBackStack = options.storeInBackStack,
        params = options.params,
        extras = options.extras,
        sharedElements = options.sharedElements,
        replace = options.replace
    )

    /**
     * Navigates to a graph by ID
     *
     * @param graphId ID of the destination graph
     * @param options Navigation options like animations, storing in backstack, etc.
     * @return true if navigation was successful
     */
    public fun navigateToGraph(
        graphId: GraphId,
        options: NavOptions = NavOptions()
    ): Boolean = engine.navigateToGraph(
        graphId = graphId,
        storeInBackStack = options.storeInBackStack,
        params = options.params,
        extras = options.extras,
        sharedElements = options.sharedElements,
        replace = options.replace
    )

    /**
     * Navigates to a screen specified by a deep link
     *
     * @param deepLink Deep link URI
     * @param options Navigation options
     * @return true if navigation was successful
     */
    public fun navigate(
        deepLink: String,
        options: NavOptions = NavOptions()
    ): Boolean = engine.handleDeepLink(
        deepLink = deepLink,
        storeInBackStack = options.storeInBackStack,
        extras = options.extras
    )

    /**
     * Navigate to a screen with the expectation of a result when the screen is closed
     *
     * @param T The expected result type
     * @param screenId ID of the destination screen
     * @param options Navigation options
     * @param onResult Callback to handle the result
     * @return true if navigation was successful
     */
    public fun <T : NavigationResult> navigateForResult(
        screenId: ScreenId,
        options: NavOptions = NavOptions(),
        onResult: suspend (T) -> Unit
    ): Boolean = engine.navigateForResult(
        screenId = screenId,
        storeInBackStack = options.storeInBackStack,
        params = options.params,
        extras = options.extras,
        sharedElements = options.sharedElements,
        onResult = onResult
    )

    /**
     * Navigate to a graph with the expectation of a result
     *
     * @param T The expected result type
     * @param graphId ID of the destination graph
     * @param options Navigation options
     * @param onResult Callback to handle the result
     * @return true if navigation was successful
     */
    public fun <T : NavigationResult> navigateToGraphForResult(
        graphId: GraphId,
        options: NavOptions = NavOptions(),
        onResult: suspend (T) -> Unit
    ): Boolean = engine.navigateToGraphForResult(
        graphId = graphId,
        storeInBackStack = options.storeInBackStack,
        params = options.params,
        extras = options.extras,
        sharedElements = options.sharedElements,
        onResult = onResult
    )

    /**
     * Go back to the previous screen or graph
     *
     * @return true if navigation back was successful
     */
    public fun popBackStack(): Boolean {
        val result = engine.popBackStack()

        // The NavigationEngine marked screens for cleanup internally,
        // but NavController is responsible for actually clearing state
        // during next composition cycle

        return result
    }

    /**
     * Pop screens until reaching the specified screen ID
     *
     * @param screenId Target screen ID to stop at
     * @return true if the operation was successful
     */
    public fun popUntil(screenId: ScreenId): Boolean = engine.popUntil(screenId)

    /**
     * Pop the current navigation graph
     *
     * @return true if the operation was successful
     */
    public fun popGraph(): Boolean = engine.popGraph()

    /**
     * Return a result to a previous screen and pop the current screen
     *
     * @param result Result to return
     * @return true if the operation was successful
     */
    public suspend fun <T : NavigationResult> popWithResult(result: T): Boolean =
        engine.popWithResult(result)

    /**
     * Check if currently in the specified graph
     *
     * @param graphId ID of the graph to check
     * @return true if currently in the specified graph
     */
    public fun isInGraph(graphId: GraphId): Boolean = engine.isInGraph(graphId)

    /**
     * Get a saveable state provider for a screen
     */
    @Composable
    public fun SaveableStateProvider(
        screen: ScreenDestination,
        content: @Composable () -> Unit
    ) {
        // Track state saving/restoring for debugging purposes
        val key = "${screen.id.id}:${screen.uuid}"
        val count = stateRestorations[key] ?: 0
        stateRestorations[key] = count + 1
        
        println("NAV_CONTROLLER: SaveableStateProvider for ${screen.id.id} (uuid: ${screen.uuid}) - Restoration #${count + 1}")

        // Remember the state holder to manage screen lifecycle
        val stateHolder = rememberSaveableStateHolder()

        // Forward to engine with state holder
        engine.SaveableStateProvider(screen, stateHolder) {
            // Clean up state on permanent navigation away from screen
            if (previousDestination.value?.uuid == screen.uuid) {
                // Check if we're permanently navigated away from this screen
                // by verifying it's not in the back stack
                val current = currentDestination.value
                if (current != null && current.uuid != screen.uuid) {
                    engine.clearSavedState(stateHolder, screen)
                }
            }

            content()
        }
    }

    /**
     * Handle a gesture for back navigation
     *
     * @param flow The flow event
     * @return true if the gesture was handled
     */
    public suspend fun handleBackGesture(flow: kotlinx.coroutines.flow.Flow<NavigationGesture>): Boolean =
        engine.handleGesture(flow)

    public companion object {
        /**
         * Create a new NavController with the specified graph as the initial destination
         */
        public fun create(
            initialGraphId: GraphId,
            extras: Extra? = null,
            params: ScatterMap<String, String> = emptyScatterMap(),
            builder: NavGraphBuilder.() -> Unit
        ): NavController {
            val navGraphBuilder = NavGraphBuilder()
            navGraphBuilder.apply(builder)
            return NavController(
                NavigationEngine(
                    initialGraphId = initialGraphId,
                    extras = extras,
                    params = params,
                    graphFactories = navGraphBuilder.graphs,
                    graphs = navGraphBuilder.prebuiltGraphs
                )
            )
        }
    }
}

/**
 * Options for navigation operations
 */
public data class NavOptions(
    val storeInBackStack: Boolean = true,
    val params: ScatterMap<String, String> = emptyScatterMap(),
    val extras: Extra? = null,
    val sharedElements: ObjectList<SharedElement> = emptyObjectList(),
    val replace: Boolean = false
)

/**
 * State of navigation animation
 */
public enum class NavigationAnimationState {
    NONE,
    NAVIGATING_FORWARD,
    NAVIGATING_BACKWARD
}