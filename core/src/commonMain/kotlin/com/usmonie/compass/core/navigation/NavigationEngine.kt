@file:Suppress("UNCHECKED_CAST")

package com.usmonie.compass.core.navigation

import androidx.collection.MutableScatterMap
import androidx.collection.ObjectList
import androidx.collection.ScatterMap
import androidx.collection.emptyScatterMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.staticCompositionLocalOf
import com.usmonie.compass.core.Extra
import com.usmonie.compass.core.GraphId
import com.usmonie.compass.core.NavigationResult
import com.usmonie.compass.core.SharedElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext
import androidx.collection.emptyObjectList as androidEmptyObjectList

/**
 * Simple logger for tracking navigation state
 */
private object NavigationEngineLogger {
    fun logInfo(message: String) = println("[NavigationEngine] INFO: $message")
    fun logDebug(message: String) = println("[NavigationEngine] DEBUG: $message")
    fun logStateRestoration(graphId: GraphId, message: String) =
        println("[NavigationEngine] STATE_RESTORE: Graph ${graphId.id} - $message")

    fun logBackstackOperation(operation: String, details: String) =
        println("[NavigationEngine] BACKSTACK: $operation - $details")
}

/**
 * Core engine that powers the navigation system.
 * Handles all navigation operations, state management, and transitions.
 */
internal class NavigationEngine(
    initialGraphId: GraphId,
    extras: Extra? = null,
    params: ScatterMap<String, String> = emptyScatterMap(),
    private val graphFactories: MutableScatterMap<GraphId, DestinationGraphFactory>,
    private val graphs: MutableScatterMap<GraphId, DestinationGraph>
) : CoroutineScope {
    // Coroutine scope for navigation operations
    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Main

    // Mutex for thread-safe navigation operations
    private val navigationMutex = Mutex()

    // Simple logging helper
    private fun logDebug(message: String) = NavigationEngineLogger.logDebug(message)
    private fun logInfo(message: String) = NavigationEngineLogger.logInfo(message)
    private fun logError(message: String, error: Throwable? = null) {
        NavigationEngineLogger.logDebug(message)
        error?.printStackTrace()
    }

    // State flows exposed to NavController
    private val _currentDestination = MutableStateFlow<ScreenDestination?>(null)
    val currentDestination: StateFlow<ScreenDestination?> = _currentDestination

    private val _previousDestination = MutableStateFlow<ScreenDestination?>(null)
    val previousDestination: StateFlow<ScreenDestination?> = _previousDestination

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack

    private val _navigationAnimationState = MutableStateFlow(NavigationAnimationState.NONE)
    val navigationAnimationState: StateFlow<NavigationAnimationState> = _navigationAnimationState

    // Navigation back stack
    private val backStack = NavBackstack()

    // Store graph backstacks to preserve state when switching between graphs
    private val graphBackstacks = mutableMapOf<GraphId, GraphBackstackState>()

    // Current graph ID
    private var currentGraphId: GraphId? = null

    // Active gesture processor
    private var activeBackProcessor: BackProcessor? = null

    // Map of pending result callbacks
    private val resultCallbacks = mutableMapOf<String, suspend (NavigationResult) -> Unit>()

    // Store screen state keys to manage their lifecycle
    private val savedScreenStates = mutableSetOf<String>()

    init {
        logInfo("Initializing NavigationEngine with initialGraphId: ${initialGraphId.id}")
        logDebug("Available graph factories count: ${graphFactories.size}")
        logDebug("Available graphs count: ${graphs.size}")

        // Try to initialize with the initial graph's root screen
        try {
            // Try to get or create the initial graph
            val initialGraph = getOrCreateGraph(initialGraphId, params, extras, true)

            if (initialGraph != null) {
                logDebug("Found initial graph: ${initialGraphId.id}")
                currentGraphId = initialGraphId

                // Create root screen from the graph
                val rootScreen = initialGraph.rootScreenFactory(true, params, extras)

                // Set the root screen as current destination
                rootScreen.onEnter()
                _currentDestination.value = rootScreen
                backStack.push(rootScreen)
                logInfo("Setting initial root screen: ${rootScreen.id.id}")
            } else {
                logError("Could not initialize with graph: ${initialGraphId.id}")
            }
        } catch (e: Exception) {
            logError("Error initializing root screen", e)
        }
    }

    /**
     * Helper method to get or create a navigation graph
     */
    private fun getOrCreateGraph(
        graphId: GraphId,
        params: ScatterMap<String, String>,
        extras: Extra?,
        storeInBackStack: Boolean
    ): DestinationGraph? {
        // Check if the graph exists in the prebuilt graphs
        return graphs[graphId] ?: run {
            // If not, try to create it from a factory
            val factory = graphFactories[graphId]
            if (factory != null) {
                logDebug("Creating graph from factory: ${graphId.id}")
                factory.invoke(params, extras, storeInBackStack).also {
                    // Cache the created graph
                    graphs[graphId] = it
                }
            } else {
                logError("Graph not found: ${graphId.id}")
                null
            }
        }
    }

    // Navigation operations
    fun navigateTo(
        screenId: ScreenId,
        storeInBackStack: Boolean,
        params: ScatterMap<String, String>?,
        extras: Extra?,
        sharedElements: ObjectList<SharedElement>,
        replace: Boolean
    ): Boolean {
        logDebug("NavigateTo screen: ${screenId.id}, storeInBackStack: $storeInBackStack, replace: $replace")

        // Find the screen factory in the current graph
        val currentGraph = currentGraphId?.let { graphId -> graphs[graphId] }
        if (currentGraph == null) {
            logError("No current graph to navigate from")
            return false
        }

        // Look for screen factory in the current graph
        val screen =
            currentGraph.findScreen(screenId, storeInBackStack, params, extras, sharedElements)
        if (screen == null) {
            logError("Screen ${screenId.id} not found in current graph ${currentGraph.id.id}")
            return false
        }

        // Store current screen in back stack if needed
        val current = _currentDestination.value
        if (storeInBackStack && current != null && !replace) {
            current.onExit()
            backStack.push(current)
            savedScreenStates.add(current.uuid)
            logDebug("Added screen ${current.id.id} (uuid: ${current.uuid}) to savedScreenStates")
        } else if (replace && current != null) {
            // For replace, just don't push the current screen
            current.onExit()
            // Since we're replacing, we should clear the old screen's state
            // (this will be done by the SaveableStateHolder in the UI layer)
        }

        // Update state
        screen.onEnter()
        _previousDestination.value = _currentDestination.value
        _currentDestination.value = screen
        if (!replace) {
            backStack.push(screen)
            savedScreenStates.add(screen.uuid)
            logDebug("Added new screen ${screen.id.id} (uuid: ${screen.uuid}) to savedScreenStates")
        }
        _canGoBack.value = backStack.canPop()
        _navigationAnimationState.value = NavigationAnimationState.NAVIGATING_FORWARD

        logInfo("Successfully navigated to screen: ${screenId.id}")
        return true
    }

    fun navigateToGraph(
        graphId: GraphId,
        storeInBackStack: Boolean,
        params: ScatterMap<String, String>,
        extras: Extra?,
        sharedElements: ObjectList<SharedElement>,
        replace: Boolean
    ): Boolean {
        logDebug("NavigateToGraph: ${graphId.id}, storeInBackStack: $storeInBackStack, replace: $replace")
        try {
            // If we're already in this graph, do nothing
            if (currentGraphId == graphId) {
                logDebug("Already in graph ${graphId.id}, no navigation needed")
                return true
            }

            // Save current graph state before switching
            currentGraphId?.let { saveGraphState(it) }

            // Check if we have a saved state for this graph
            val savedState = graphBackstacks[graphId]

            if (savedState != null) {
                NavigationEngineLogger.logInfo("Restoring saved state for graph: ${graphId.id}")
                NavigationEngineLogger.logStateRestoration(
                    graphId,
                    "Restoring saved graph state with ${savedState.backstack.size} screens"
                )

                // Restore the graph state
                currentGraphId = graphId

                // Get the top screen from the saved backstack
                val topScreen = savedState.topScreen
                topScreen.onEnter()
                NavigationEngineLogger.logStateRestoration(
                    graphId,
                    "Restoring screen ${topScreen.id.id} (uuid: ${topScreen.uuid})"
                )

                // Update UI state
                _previousDestination.value = _currentDestination.value
                _currentDestination.value = topScreen

                // Restore the backstack
                backStack.replaceStack(savedState.backstack)
                NavigationEngineLogger.logBackstackOperation(
                    "RESTORED",
                    "Restored backstack with ${savedState.backstack.size} items for graph ${graphId.id}"
                )

                _canGoBack.value = backStack.canPop()
                _navigationAnimationState.value = NavigationAnimationState.NAVIGATING_FORWARD

                NavigationEngineLogger.logInfo("Successfully restored graph state: ${graphId.id}")
                return true
            }

            // No saved state, create a new graph state
            // Get or create the graph
            val graph = getOrCreateGraph(graphId, params, extras, storeInBackStack)

            if (graph == null) {
                logError("Failed to navigate: Graph ${graphId.id} not found in factories or prebuilt graphs")
                return false
            }

            // Get the root screen factory from the graph
            val rootScreen = graph.rootScreenFactory(storeInBackStack, params, extras)

            logInfo("Navigating to graph ${graphId.id} with root screen: ${rootScreen.id.id}")

            // Handle current screen
            val currentScreen = _currentDestination.value
            if (storeInBackStack && currentScreen != null && !replace) {
                currentScreen.onExit()
                // We don't push to backstack yet as we'll push the screen from the new graph
            } else if (replace && currentScreen != null) {
                currentScreen.onExit()
                backStack.pop() // Remove the current screen from backstack
            }

            // Update the current graph ID
            currentGraphId = graphId

            // Set the root screen as current destination
            rootScreen.onEnter()
            _previousDestination.value = _currentDestination.value
            _currentDestination.value = rootScreen
            backStack.clear() // Clear existing backstack when creating new graph state
            backStack.push(rootScreen)
            _canGoBack.value = backStack.canPop()
            _navigationAnimationState.value = NavigationAnimationState.NAVIGATING_FORWARD

            logInfo("Successfully navigated to graph: ${graphId.id}")
            return true
        } catch (e: Exception) {
            logError("Failed to navigate to graph: ${graphId.id}", e)
            return false
        }
    }

    fun <T : NavigationResult> navigateForResult(
        screenId: ScreenId,
        storeInBackStack: Boolean,
        params: ScatterMap<String, String>?,
        extras: Extra?,
        sharedElements: ObjectList<SharedElement>,
        onResult: suspend (T) -> Unit
    ): Boolean {
        logDebug("NavigateForResult to screen: ${screenId.id}")

        // Find the screen factory in the current graph
        val currentGraph = currentGraphId?.let { graphId -> graphs[graphId] }
        if (currentGraph == null) {
            logError("No current graph to navigate from")
            return false
        }

        // Look for screen factory in the current graph
        val screen =
            currentGraph.findScreen(screenId, storeInBackStack, params, extras, sharedElements)
        if (screen == null) {
            logError("Screen ${screenId.id} not found in current graph ${currentGraph.id.id}")
            return false
        }

        // Store the result callback
        resultCallbacks[screen.uuid] = onResult as suspend (NavigationResult) -> Unit

        // Store current screen in back stack if needed
        val current = _currentDestination.value
        if (storeInBackStack && current != null) {
            current.onExit()
            backStack.push(current)
        }

        // Update state
        screen.onEnter()
        _previousDestination.value = _currentDestination.value
        _currentDestination.value = screen
        backStack.push(screen)
        _canGoBack.value = backStack.canPop()
        _navigationAnimationState.value = NavigationAnimationState.NAVIGATING_FORWARD

        logInfo("Successfully navigated for result to screen: ${screenId.id}")
        return true
    }

    fun <T : NavigationResult> navigateToGraphForResult(
        graphId: GraphId,
        storeInBackStack: Boolean,
        params: ScatterMap<String, String>?,
        extras: Extra?,
        sharedElements: ObjectList<SharedElement>,
        onResult: suspend (T) -> Unit
    ): Boolean {
        logDebug("NavigateToGraphForResult: ${graphId.id}")

        try {
            // Get or create the graph
            val safeParams = params ?: emptyScatterMap()
            val graph = getOrCreateGraph(graphId, safeParams, extras, storeInBackStack)

            if (graph == null) {
                logError("Failed to navigate: Graph ${graphId.id} not found in factories or prebuilt graphs")
                return false
            }

            // Get the root screen factory
            val rootScreen = graph.rootScreenFactory(storeInBackStack, safeParams, extras)

            logInfo("Navigating to graph ${graphId.id} with root screen: ${rootScreen.id.id}")

            // Handle current screen
            val currentScreen = _currentDestination.value
            if (storeInBackStack && currentScreen != null) {
                currentScreen.onExit()
                // We don't push to backstack yet as we'll push the screen from the new graph
            }

            // Update the current graph ID
            currentGraphId = graphId

            // Store the result callback using the graph ID as key
            resultCallbacks[graphId.id] = onResult as suspend (NavigationResult) -> Unit

            // Set the root screen as current destination
            rootScreen.onEnter()
            _previousDestination.value = _currentDestination.value
            _currentDestination.value = rootScreen
            backStack.push(rootScreen)
            _canGoBack.value = backStack.canPop()
            _navigationAnimationState.value = NavigationAnimationState.NAVIGATING_FORWARD

            logInfo("Successfully navigated to graph for result: ${graphId.id}")
            return true
        } catch (e: Exception) {
            logError("Failed to navigate to graph for result: ${graphId.id}", e)
            return false
        }
    }

    fun handleDeepLink(
        deepLink: String,
        storeInBackStack: Boolean,
        extras: Extra?
    ): Boolean {
        logDebug("HandleDeepLink: $deepLink")
        logInfo("Deep link handling is not yet implemented: $deepLink")
        return false
    }

    fun popBackStack(): Boolean {
        logDebug("PopBackStack")
        try {
            if (!_canGoBack.value) {
                logDebug("Cannot pop back stack - no previous destination")
                return false
            }

            // Pop the current screen
            val currentScreen = _currentDestination.value
            if (currentScreen != null) {
                currentScreen.onExit()

                // Remove from back stack
                backStack.pop()

                // Since we're popping and not expecting to return to this screen,
                // we can mark it for state cleanup (actual cleanup happens in UI layer)
                if (backStack.findIndexOf(currentScreen.id) == -1) {
                    // The screen is no longer in the backstack, we can remove its state
                    NavigationEngineLogger.logDebug("Marking screen state for cleanup: ${currentScreen.id.id}")
                    clearSavedState(null, currentScreen)
                }
            }

            // Get the previous screen from back stack
            val previousScreen = backStack.peek()
            if (previousScreen != null) {
                previousScreen.onEnter()
                _previousDestination.value = _currentDestination.value
                _currentDestination.value = previousScreen
                _canGoBack.value = backStack.canPop()
                _navigationAnimationState.value = NavigationAnimationState.NAVIGATING_BACKWARD

                // Check if we've popped back to a different graph
                val graphForScreen = findGraphForScreen(previousScreen.id)
                if (graphForScreen != null) {
                    currentGraphId = graphForScreen
                }

                logInfo("Successfully popped back stack")
                return true
            } else {
                logError("Back stack is empty after pop")
                return false
            }
        } catch (e: Exception) {
            logError("Failed to pop back stack", e)
            return false
        }
    }

    fun popUntil(screenId: ScreenId): Boolean {
        logDebug("PopUntil screen: ${screenId.id}")

        try {
            // Check if we can go back at all
            if (!_canGoBack.value) {
                logDebug("Cannot pop back stack - no previous destination")
                return false
            }

            // Look for the target screen in the back stack
            val targetScreenIndex = backStack.findIndexOf(screenId)
            if (targetScreenIndex == -1) {
                logError("Screen ${screenId.id} not found in back stack")
                return false
            }

            // Current screen should exit
            val currentScreen = _currentDestination.value
            if (currentScreen != null) {
                currentScreen.onExit()
            }

            // Store the screens that will be popped for state cleanup
            val screensToCleanup = mutableListOf<ScreenDestination>()
            val currentBackStack = backStack.getScreensCopy()

            // Find screens between current and target that will be removed
            var foundTarget = false
            for (screen in currentBackStack.reversed()) {
                if (screen.id == screenId) {
                    foundTarget = true
                    break
                }
                screensToCleanup.add(screen)
            }

            // Pop screens until we reach the target
            backStack.popUntil(screenId)

            // Get the target screen from the back stack
            val targetScreen = backStack.peek()
            if (targetScreen != null) {
                targetScreen.onEnter()
                _previousDestination.value = _currentDestination.value
                _currentDestination.value = targetScreen
                _canGoBack.value = backStack.canPop()
                _navigationAnimationState.value = NavigationAnimationState.NAVIGATING_BACKWARD

                // Mark screens for cleanup
                for (screen in screensToCleanup) {
                    if (backStack.findIndexOf(screen.id) == -1) {
                        // Screen is no longer in backstack
                        NavigationEngineLogger.logDebug("Marking popped screen for cleanup: ${screen.id.id}")
                        // State will be cleaned up in the UI layer
                    }
                }

                // Check if we've popped back to a different graph
                val graphForScreen = findGraphForScreen(targetScreen.id)
                if (graphForScreen != null) {
                    currentGraphId = graphForScreen
                }

                logInfo("Successfully popped until screen: ${screenId.id}")
                return true
            } else {
                logError("Back stack is empty after popUntil")
                return false
            }
        } catch (e: Exception) {
            logError("Failed to pop until screen: ${screenId.id}", e)
            return false
        }
    }

    fun popGraph(): Boolean {
        logDebug("PopGraph")

        try {
            // Check if we can go back at all
            if (!_canGoBack.value) {
                logDebug("Cannot pop back stack - no previous destination")
                return false
            }

            // Current screen should exit
            val currentScreen = _currentDestination.value
            if (currentScreen != null) {
                currentScreen.onExit()
            }

            // Remember the current graph ID to identify screens to clean up
            val currentGraphId = this.currentGraphId

            // Store screens in current graph for state cleanup
            val screensToCleanup = if (currentGraphId != null) {
                backStack.getScreensCopy().filter {
                    findGraphForScreen(it.id) == currentGraphId
                }
            } else {
                emptyList()
            }

            // Find the first screen from a different graph
            var foundDifferentGraph = false
            var targetScreen: ScreenDestination? = null
            var newGraphId: GraphId? = null

            if (currentGraphId != null) {
                // Pop screens until we find one from a different graph
                while (!foundDifferentGraph && backStack.canPop()) {
                    backStack.pop()

                    targetScreen = backStack.peek()
                    if (targetScreen != null) {
                        val targetGraphId = findGraphForScreen(targetScreen.id)
                        if (targetGraphId != null && targetGraphId != currentGraphId) {
                            foundDifferentGraph = true
                            newGraphId = targetGraphId
                            break
                        }
                    } else {
                        break
                    }
                }
            }

            if (foundDifferentGraph && targetScreen != null) {
                // We found a screen from a different graph
                targetScreen.onEnter()
                _previousDestination.value = _currentDestination.value
                _currentDestination.value = targetScreen
                _canGoBack.value = backStack.canPop()
                _navigationAnimationState.value = NavigationAnimationState.NAVIGATING_BACKWARD

                // Mark screens from popped graph for cleanup
                for (screen in screensToCleanup) {
                    if (backStack.findIndexOf(screen.id) == -1) {
                        NavigationEngineLogger.logDebug("Marking graph screen for cleanup: ${screen.id.id}")
                        // State will be cleaned up in the UI layer
                    }
                }

                // Update current graph ID
                this.currentGraphId = newGraphId

                logInfo("Successfully popped graph")
                return true
            } else {
                logError("Could not find a previous graph to pop to")
                return false
            }
        } catch (e: Exception) {
            logError("Failed to pop graph", e)
            return false
        }
    }

    suspend fun <T : NavigationResult> popWithResult(result: T): Boolean {
        logDebug("PopWithResult: $result")

        return navigationMutex.withLock {
            try {
                // Check if we can go back at all
                if (!_canGoBack.value) {
                    logDebug("Cannot pop back stack - no previous destination")
                    return@withLock false
                }

                // Get current screen UUID for result callback lookup
                val currentScreen = _currentDestination.value
                if (currentScreen == null) {
                    logError("No current screen to pop from")
                    return@withLock false
                }

                // Pop the current screen
                currentScreen.onExit()
                backStack.pop()

                // Get the previous screen
                val previousScreen = backStack.peek()
                if (previousScreen == null) {
                    logError("No previous screen found after pop")
                    return@withLock false
                }

                // Find callbacks for both UUID and current graph
                val callback = resultCallbacks[currentScreen.uuid]
                val graphCallback = currentGraphId?.let { resultCallbacks[it.id] }

                // Update state first
                previousScreen.onEnter()
                _previousDestination.value = _currentDestination.value
                _currentDestination.value = previousScreen
                _canGoBack.value = backStack.canPop()
                _navigationAnimationState.value = NavigationAnimationState.NAVIGATING_BACKWARD

                // Check if we've popped back to a different graph
                val graphForScreen = findGraphForScreen(previousScreen.id)
                if (graphForScreen != null) {
                    currentGraphId = graphForScreen
                }

                // Deliver result to previous screen or graph
                try {
                    if (callback != null) {
                        launch {
                            callback(result)
                        }
                        resultCallbacks.remove(currentScreen.uuid)
                        logDebug("Delivered result to screen callback")
                    } else if (graphCallback != null) {
                        launch {
                            graphCallback(result)
                        }
                        currentGraphId?.let { resultCallbacks.remove(it.id) }
                        logDebug("Delivered result to graph callback")
                    } else {
                        // Try calling onResult directly on previous screen
                        previousScreen.onResult(result)
                        logDebug("Called onResult directly on previous screen")
                    }
                } catch (e: Exception) {
                    logError("Error delivering result", e)
                }

                logInfo("Successfully popped with result")
                return@withLock true
            } catch (e: Exception) {
                logError("Failed to pop with result", e)
                return@withLock false
            }
        }
    }

    fun registerGraph(graph: DestinationGraph) {
        logDebug("Registering graph: ${graph.id.id}")
        graphs[graph.id] = graph
        logDebug("Graph registered, total graphs: ${graphs.size}")
    }

    fun isInGraph(graphId: GraphId): Boolean {
        val result = currentGraphId == graphId
        logDebug("IsInGraph: ${graphId.id}, result: $result")
        return result
    }

    @Composable
    fun SaveableStateProvider(
        screen: ScreenDestination,
        stateHolder: SaveableStateHolder,
        content: @Composable () -> Unit
    ) {
        NavigationEngineLogger.logDebug("SaveableStateProvider for screen: ${screen.id.id} (uuid: ${screen.uuid})")

        // Use the screen's UUID as key to save/restore its state
        val key = screen.uuid

        // Register a DisposableEffect to clear state when screen is permanently removed
        DisposableEffect(key) {
            onDispose {
                // Don't clear immediately as we might want to return to this screen
                // We should only clear if explicitly removed from backstack
            }
        }

        CompositionLocalProvider(LocalRestorableComposition provides key) {
            // Use the SaveableStateHolder to preserve state across recompositions
            stateHolder.SaveableStateProvider(key = key) {
                content()
            }
        }
    }

    /**
     * Clean up resources when no longer needed
     */
    fun onCleared() {
        // Cancel any active processors
        activeBackProcessor?.cancel()
        activeBackProcessor = null

        // Clear result callbacks
        resultCallbacks.clear()

        // Clear the back stack
        backStack.clear()

        // Clear graph backstacks
        graphBackstacks.clear()

        // Clear saved screen states
        savedScreenStates.clear()

        // Cancel all coroutines
        coroutineContext.cancel()
    }

    /**
     * Clear saved state for a screen when it's permanently removed from the navigation stack
     */
    internal fun clearSavedState(stateHolder: SaveableStateHolder?, screen: ScreenDestination) {
        val key = screen.uuid
        if (stateHolder != null && savedScreenStates.contains(key)) {
            NavigationEngineLogger.logDebug("Clearing saved state for screen: ${screen.id.id} (uuid: $key)")
            // Only remove the state if the screen is no longer in the backstack
            if (backStack.findIndexOf(screen.id) == -1) {
                stateHolder.removeState(key)
                savedScreenStates.remove(key)
                NavigationEngineLogger.logDebug("State cleared for screen: ${screen.id.id}")
            } else {
                NavigationEngineLogger.logDebug("Screen ${screen.id.id} still in backstack, not clearing state")
            }
        }
    }

    /**
     * Save the current state of a graph when switching away from it
     */
    private fun saveGraphState(graphId: GraphId) {
        logDebug("Saving state for graph: ${graphId.id}")

        // Get the current screen and backstack
        val currentScreen = _currentDestination.value ?: return

        // Get a copy of the current backstack
        val backstackCopy = backStack.getScreensCopy()

        if (backstackCopy.isNotEmpty()) {
            // Store the graph state
            graphBackstacks[graphId] = GraphBackstackState(
                topScreen = currentScreen,
                backstack = backstackCopy
            )
            NavigationEngineLogger.logDebug("Saved graph state with ${backstackCopy.size} screens")
            NavigationEngineLogger.logStateRestoration(
                graphId,
                "Saved graph state with ${backstackCopy.size} screens, top screen: ${currentScreen.id.id}"
            )
        }
    }

    /**
     * Data class to store the state of a graph's backstack
     */
    private data class GraphBackstackState(
        val topScreen: ScreenDestination,
        val backstack: List<ScreenDestination>
    )

    /**
     * Helper method to create an empty ObjectList
     */
    private fun <T> emptyObjectList(): ObjectList<T> {
        // Use the AndroidX collection library's utility function
        return androidEmptyObjectList()
    }

    /**
     * Handle navigation gestures like swipe-to-go-back
     */
    suspend fun handleGesture(gestures: Flow<NavigationGesture>): Boolean {
        logDebug("HandleGesture")

        // Check if we can handle back navigation
        if (!_canGoBack.value) {
            logDebug("Cannot handle gesture - no back navigation possible")
            return false
        }

        // Create a new back processor if needed
        val processor = activeBackProcessor ?: BackProcessor(
            scope = this,
            isPredictive = true,
            onBack = { gestureFlow ->
                var gestureCompleted = false
                var shouldPopBackStack = false

                // Process the gesture flow
                gestureFlow.collect { gesture ->
                    when (gesture) {
                        is NavigationGesture.Start -> {
                            // Start tracking the gesture
                            _navigationAnimationState.value =
                                NavigationAnimationState.NAVIGATING_BACKWARD
                        }

                        is NavigationGesture.Sliding -> {
                            // Calculate progress based on position
                            val progress = when (gesture.edge) {
                                GestureEdge.LEFT_TO_RIGHT -> gesture.positionX / gesture.screenWidth
                                GestureEdge.RIGHT_TO_LEFT -> 1f - (gesture.positionX / gesture.screenWidth)
                            }.coerceIn(0f, 1f)

                            // Update animation state based on progress
                            if (progress > 0.5f) {
                                shouldPopBackStack = true
                            }
                        }

                        is NavigationGesture.End -> {
                            // Finalize the gesture
                            gestureCompleted = true
                        }
                    }
                }

                // If gesture completed and we should pop back stack
                if (gestureCompleted && shouldPopBackStack) {
                    popBackStack()
                    true
                } else {
                    // Reset animation state if not popping
                    _navigationAnimationState.value = NavigationAnimationState.NONE
                    false
                }
            }
        ).also {
            activeBackProcessor = it
        }

        // Process gestures
        try {
            gestures.collect { gesture ->
                processor.send(gesture)

                // If it's an end gesture, clear the processor
                if (gesture is NavigationGesture.End) {
                    activeBackProcessor = null
                }
            }
            return true
        } catch (e: Exception) {
            logError("Error processing gesture", e)
            activeBackProcessor = null
            return false
        }
    }

    /**
     * Helper method to find which graph contains a given screen
     */
    private fun findGraphForScreen(screenId: ScreenId): GraphId? {
        var result: GraphId? = null
        graphs.forEach { graphId, graph ->
            // Check if graph contains the screen
            val foundScreen =
                graph.findScreen(screenId, false, null, null, androidEmptyObjectList())
            if (foundScreen != null) {
                result = graphId
                return@forEach
            }
        }
        return result
    }
}

public val LocalRestorableComposition: ProvidableCompositionLocal<String> =
    staticCompositionLocalOf<String> { error("No restoration key provided") }
