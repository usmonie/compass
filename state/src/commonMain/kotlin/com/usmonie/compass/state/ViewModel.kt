package com.usmonie.compass.state

/**
 * Base interface for ViewModels in the Compass State Management library.
 *
 * This interface defines the basic lifecycle contract that all ViewModels must implement.
 * It ensures proper resource cleanup and lifecycle management for ViewModels.
 *
 * ViewModels implementing this interface should:
 * - Cancel any ongoing coroutines in onDispose()
 * - Clean up resources like listeners, subscriptions, or file handles
 * - Avoid memory leaks by properly disposing of references
 *
 * Example implementation:
 * ```kotlin
 * class MyViewModel : ViewModel {
 *     private val job = SupervisorJob()
 *     private val scope = CoroutineScope(job + Dispatchers.Main)
 *
 *     override fun onDispose() {
 *         job.cancel() // Cancel all coroutines
 *         // Clean up other resources...
 *     }
 * }
 * ```
 */
public interface ViewModel {
    /**
     * Called when the ViewModel is no longer needed and should clean up its resources.
     *
     * Implementations should:
     * - Cancel any ongoing coroutines
     * - Close any open resources (files, database connections, etc.)
     * - Unregister any listeners or callbacks
     * - Clear references to prevent memory leaks
     *
     * This method is typically called when:
     * - The associated screen/component is being destroyed
     * - The application is shutting down
     * - The ViewModel is being replaced with a new instance
     */
    public fun onDispose()
}
