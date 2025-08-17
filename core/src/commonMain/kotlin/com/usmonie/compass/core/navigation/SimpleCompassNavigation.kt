package com.usmonie.compass.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Simplified navigation host that works without androidx.navigation3 complexity
 * This demonstrates the new architecture while avoiding runtime issues
 */
@Composable
public fun SimpleCompassNavHost(
    startDestination: ScreenId,
    modifier: Modifier = Modifier,
    builder: SimpleCompassNavigationBuilder.() -> Unit
) {
    val backStack = remember { mutableStateListOf(startDestination) }
    val graphBuilder = remember { SimpleCompassNavigationBuilder() }
    
    // Build the graph
    graphBuilder.apply(builder)
    
    // Get current screen and render it
    val currentScreen = backStack.lastOrNull() ?: startDestination
    val screenRenderer = graphBuilder.screens[currentScreen]
    
    if (screenRenderer != null) {
        val navigator = SimpleCompassNavigator(backStack)
        screenRenderer(navigator)
    }
}

/**
 * Navigation builder for defining screens
 */
public class SimpleCompassNavigationBuilder {
    internal val screens = mutableMapOf<ScreenId, @Composable (SimpleCompassNavigator) -> Unit>()
    
    /**
     * Add a screen to the navigation graph
     */
    public fun screen(
        screenId: ScreenId,
        content: @Composable (navigator: SimpleCompassNavigator) -> Unit
    ) {
        screens[screenId] = content
    }
}

/**
 * Navigator for imperative navigation operations
 */
public class SimpleCompassNavigator(
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
    
    /**
     * Current back stack for debugging
     */
    public val currentBackStack: List<ScreenId> get() = backStack.toList()
}
