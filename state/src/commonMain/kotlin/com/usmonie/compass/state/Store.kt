package com.usmonie.compass.state

/**
 * A central store for ViewModels that persists instances across composition changes.
 * This is crucial for maintaining state during navigation.
 */
public object ViewModelStore {
    private val vms = mutableMapOf<Any, ViewModel>()

    /**
     * Returns an existing ViewModel of type [T] or creates a new one using [factory].
     */
    public fun <T : ViewModel> getOrPut(key: Any, factory: () -> T): T {
        @Suppress("UNCHECKED_CAST")
        return vms.getOrPut(key) { factory() } as T
    }

    /**
     * Removes and disposes the ViewModel associated with the given [key].
     */
    public fun remove(key: Any) {
        vms.remove(key)?.onDispose()
    }

    /**
     * Disposes all ViewModels and clears the store.
     */
    public fun clear() {
        vms.values.forEach { it.onDispose() }
        vms.clear()
    }
    
    /**
     * Clears ViewModels that are no longer present in the [activeKeys] list.
     * Use this when the backstack changes to prevent memory leaks.
     */
    public fun sync(activeKeys: List<Any>, onRemoved: (Any) -> Unit = {}) {
        val keysToRemove = vms.keys.filter { it !in activeKeys }
        keysToRemove.forEach { 
            remove(it)
            onRemoved(it)
        }
    }
}
