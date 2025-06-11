package com.usmonie.compass.core.navigation

import androidx.compose.runtime.saveable.SaveableStateRegistry
import com.usmonie.compass.core.navigation.ScreenId

/**
 * Registry for managing and persisting state for screens in the navigation system.
 * Allows screens to save and restore their state during navigation and process lifecycle events.
 */
internal class StateRegistry {
    // Map of screen IDs to their saved state registries
    private val screenStates = mutableMapOf<String, Map<String, List<Any?>>>()

    /**
     * Save state for a screen
     *
     * @param screenId The screen ID
     * @param uuid The unique instance ID of the screen
     * @param state The state map to save
     */
    fun saveState(screenId: ScreenId, uuid: String, state: Map<String, List<Any?>>) {
        val key = generateKey(screenId, uuid)
        screenStates[key] = state
    }

    /**
     * Restore state for a screen
     *
     * @param screenId The screen ID
     * @param uuid The unique instance ID of the screen
     * @return The saved state map, or null if no state exists
     */
    fun restoreState(screenId: ScreenId, uuid: String): Map<String, List<Any?>>? {
        val key = generateKey(screenId, uuid)
        return screenStates[key]
    }

    /**
     * Check if state exists for a screen
     *
     * @param screenId The screen ID
     * @param uuid The unique instance ID of the screen
     * @return True if state exists for this screen instance
     */
    fun hasState(screenId: ScreenId, uuid: String): Boolean {
        val key = generateKey(screenId, uuid)
        return screenStates.containsKey(key)
    }

    /**
     * Remove state for a screen
     *
     * @param screenId The screen ID
     * @param uuid The unique instance ID of the screen
     */
    fun removeState(screenId: ScreenId, uuid: String) {
        val key = generateKey(screenId, uuid)
        screenStates.remove(key)
    }

    /**
     * Clear all saved state
     */
    fun clear() {
        screenStates.clear()
    }

    /**
     * Generate a unique key for a screen instance
     */
    private fun generateKey(screenId: ScreenId, uuid: String): String {
        return "${screenId.id}-$uuid"
    }

    /**
     * Create a registry consumer that can be used to save state
     * when a SaveableStateRegistry emits updates
     *
     * @param screenId The screen ID
     * @param uuid The unique instance ID of the screen
     */
    fun createRegistryConsumer(
        screenId: ScreenId,
        uuid: String
    ): (Map<String, List<Any?>>) -> Unit {
        val key = generateKey(screenId, uuid)
        return { state ->
            if (state.isNotEmpty()) {
                screenStates[key] = state
            }
        }
    }

    /**
     * Provides a SaveableStateRegistry for a screen to use in Compose
     *
     * @param screenId The screen ID
     * @param uuid The unique instance ID of the screen
     * @return A SaveableStateRegistry for the screen
     */
    fun createSaveableStateRegistry(
        screenId: ScreenId,
        uuid: String
    ): SaveableStateRegistry {
        val key = generateKey(screenId, uuid)
        val restoredState = screenStates[key]

        return SaveableStateRegistry(
            restoredValues = restoredState,
            canBeSaved = { value -> value is Int || value is String || value is Boolean || value is Float || value is Double }
        )
    }
}