package com.usmonie.compass.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.LocalNavigationEventDispatcherOwner
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.SinglePaneSceneStrategy
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner

/**
 * Navigation host using androidx.navigation3
 */
@Composable
public fun CompassNavHost(
    startDestination: ScreenId,
    modifier: Modifier = Modifier,
    builder: CompassNavigationBuilder.() -> Unit
) {
    val backStack = remember { mutableStateListOf(startDestination) }
    val graphBuilder = remember { CompassNavigationBuilder() }
    
    // Build the graph
    graphBuilder.apply(builder)
    
    // Create a navigation event dispatcher for androidx.navigation3
    val navigationEventDispatcher = remember { NavigationEventDispatcher() }
    val dispatcherOwner = remember { 
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher: NavigationEventDispatcher = navigationEventDispatcher
        }
    }
    
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides dispatcherOwner) {
        NavDisplay(
            backStack = backStack,
            modifier = modifier,
            sceneStrategy = SinglePaneSceneStrategy(),
            entryProvider = { screenId ->
                NavEntry(screenId) {
                    val screenRenderer = graphBuilder.screens[screenId]
                    if (screenRenderer != null) {
                        screenRenderer(backStack)
                    }
                }
            }
        )
    }
}

/**
 * Navigation builder for defining screens
 */
public class CompassNavigationBuilder {
    internal val screens = mutableMapOf<ScreenId, @Composable (androidx.compose.runtime.snapshots.SnapshotStateList<ScreenId>) -> Unit>()
    
    /**
     * Add a screen to the navigation graph
     */
    public fun screen(
        screenId: ScreenId,
        content: @Composable (navigator: CompassNavigator) -> Unit
    ) {
        screens[screenId] = { backStack ->
            val navigator = CompassNavigator(backStack)
            content(navigator)
        }
    }
}

/**
 * Navigator for imperative navigation operations
 */
public class CompassNavigator(
    private val backStack: androidx.compose.runtime.snapshots.SnapshotStateList<ScreenId>
) {
    /**
     * Navigate to a screen
     */
    public fun navigate(destination: ScreenId) {
        backStack.add(destination)
    }
    
    /**
     * Pop back to previous screen
     */
    public fun popBackStack(): Boolean {
        return if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            true
        } else {
            false
        }
    }
    
    /**
     * Pop to a specific screen
     */
    public fun popTo(destination: ScreenId, inclusive: Boolean = false): Boolean {
        val targetIndex = backStack.lastIndexOf(destination)
        if (targetIndex < 0) return false
        
        val removeFromIndex = if (inclusive) targetIndex else targetIndex + 1
        if (removeFromIndex >= backStack.size) return false
        
        repeat(backStack.size - removeFromIndex) {
            backStack.removeAt(backStack.lastIndex)
        }
        return true
    }
    
    /**
     * Clear back stack and navigate to destination
     */
    public fun clearAndNavigate(destination: ScreenId) {
        backStack.clear()
        backStack.add(destination)
    }
}