package com.usmonie.compass.core.navigation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * WASM-JS implementation of WebNavigationSupport
 * Limited functionality as some browser APIs might not be fully available in WASM
 */
public actual class WebNavigationSupport {
    private var deepLinkHandler: DeepLinkHandler? = null
    private var isActive = false

    /**
     * Initialize web navigation binding
     */
    public actual fun initialize(deepLinkHandler: DeepLinkHandler) {
        this.deepLinkHandler = deepLinkHandler
    }

    /**
     * Bind to the browser's history (limited in WASM)
     */
    public actual fun bindToBrowserHistory() {
        // Limited browser API access in WASM, so just mark as active
        isActive = true
    }

    /**
     * Unbind from the browser's history
     */
    public actual fun unbindFromBrowserHistory() {
        isActive = false
    }

    /**
     * Check if browser history binding is active
     */
    public actual fun isBindingActive(): Boolean {
        return isActive
    }

    /**
     * Update the browser URL without triggering navigation (no-op in WASM)
     */
    public actual fun updateBrowserUrl(path: String) {
        // Browser history API might not be fully available in WASM
        // This is a no-op for now
    }

    /**
     * Get current URL path from browser (returns empty string in WASM)
     */
    public actual fun getCurrentPath(): String {
        // Browser location API might not be fully available in WASM
        return ""
    }

    /**
     * Get a flow of browser back button events (empty flow in WASM)
     */
    public actual fun browserBackEvents(): Flow<Unit> {
        // Browser event handling might not be fully available in WASM
        return emptyFlow()
    }

    /**
     * Get a flow of URL change events (empty flow in WASM)
     */
    public actual fun urlChangeEvents(): Flow<String> {
        // Browser event handling might not be fully available in WASM
        return emptyFlow()
    }
}

/**
 * Create a web navigation support instance for WASM-JS platform
 */
public actual fun createWebNavigationSupport(): WebNavigationSupport {
    return WebNavigationSupport()
}