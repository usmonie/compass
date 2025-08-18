package com.usmonie.compass.core.navigation

import com.usmonie.compass.core.GraphId
import com.usmonie.compass.core.navigation.ScreenId

/**
 * A stack-based implementation for managing the navigation back stack.
 * Maintains a history of screens and provides operations to navigate through them.
 */
internal class NavBackstack {
    // Stack of screen destinations using mutable list
    private val stack = mutableListOf<ScreenDestination>()

    // Map to track nested graph hierarchies
    private val nestedGraphs = mutableMapOf<GraphId, NavBackstack>()

    // Current graph ID for nested navigation
    private var currentGraphId: GraphId? = null

    /**
     * Push a screen destination onto the stack
     */
    fun push(screen: ScreenDestination) {
        stack.add(screen)
    }

    /**
     * Pop and return the top screen from the stack
     * Returns null if stack is empty
     */
    fun pop(): ScreenDestination? {
        return if (stack.isNotEmpty()) {
            stack.removeAt(stack.size - 1)
        } else {
            null
        }
    }

    /**
     * Look at the top screen without removing it
     * Returns null if stack is empty
     */
    fun peek(): ScreenDestination? {
        return if (stack.isNotEmpty()) {
            stack.lastOrNull()
        } else {
            null
        }
    }

    /**
     * Check if stack can be popped (has more than one element)
     */
    fun canPop(): Boolean {
        return stack.size > 1
    }

    /**
     * Find the index of a screen with the given ID
     * Returns -1 if not found
     */
    fun findIndexOf(screenId: ScreenId): Int {
        return stack.indexOfFirst { it.id == screenId }
    }

    /**
     * Pop screens until reaching one with the specified ID
     * Returns true if the screen was found and stack was popped
     */
    fun popUntil(screenId: ScreenId): Boolean {
        val targetIndex = findIndexOf(screenId)
        if (targetIndex < 0) {
            return false
        }

        // Pop screens until we reach the target (but don't pop the target)
        while (stack.size > targetIndex + 1) {
            stack.removeAt(stack.size - 1)
        }
        return true
    }

    /**
     * Clear all screens from the stack
     */
    fun clear() {
        stack.clear()
    }

    /**
     * Get the current size of the stack
     */
    fun size(): Int {
        return stack.size
    }

    /**
     * Check if the stack contains a screen with the specified ID
     */
    fun contains(screenId: ScreenId): Boolean {
        return findIndexOf(screenId) >= 0
    }

    /**
     * Get a copy of all screens in the stack
     * Used for saving graph state
     */
    fun getScreensCopy(): List<ScreenDestination> {
        return stack.toList()
    }

    /**
     * Replace the entire stack with the provided screens
     * Used for restoring graph state
     */
    fun replaceStack(screens: List<ScreenDestination>) {
        stack.clear()
        stack.addAll(screens)
    }

    /**
     * Create a nested navigation backstack for a specific graph
     * Returns the existing one if already created
     */
    fun createNestedBackstack(graphId: GraphId): NavBackstack {
        return nestedGraphs.getOrPut(graphId) { NavBackstack() }
    }

    /**
     * Get a nested backstack for the given graph ID
     * Returns null if no such backstack exists
     */
    fun getNestedBackstack(graphId: GraphId): NavBackstack? {
        return nestedGraphs[graphId]
    }

    /**
     * Set the current active graph for nested navigation
     */
    fun setCurrentGraph(graphId: GraphId?) {
        this.currentGraphId = graphId
    }

    /**
     * Get the current active graph ID
     */
    fun getCurrentGraph(): GraphId? {
        return currentGraphId
    }

    /**
     * Find a screen with a specific UUID across the entire backstack hierarchy
     * Returns the screen and its containing backstack if found
     */
    fun findScreenByUuid(uuid: String): Pair<ScreenDestination, NavBackstack>? {
        // Check in current backstack
        val screen = stack.find { it.uuid == uuid }
        if (screen != null) {
            return Pair(screen, this)
        }

        // Check in nested backstacks
        nestedGraphs.values.forEach { nestedStack ->
            val result = nestedStack.findScreenByUuid(uuid)
            if (result != null) {
                return result
            }
        }

        return null
    }

    /**
     * Remove all nested graph backstacks
     */
    fun clearNestedGraphs() {
        nestedGraphs.clear()
        currentGraphId = null
    }
}