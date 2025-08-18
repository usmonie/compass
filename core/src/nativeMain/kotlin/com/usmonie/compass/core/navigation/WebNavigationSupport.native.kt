package com.usmonie.compass.core.navigation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Native implementation of WebNavigationSupport.
 *
 * On native platforms, web navigation features are not applicable, so this is a minimal implementation
 * that allows the code to compile but doesn't provide actual web functionality.
 */
public actual class WebNavigationSupport {
    private var navController: NavController? = null
    private var deepLinkHandler: DeepLinkHandler? = null

    /**
     * Initialize web navigation binding - minimal implementation for native
     */
    public actual fun initialize(navController: NavController, deepLinkHandler: DeepLinkHandler) {
        this.navController = navController
        this.deepLinkHandler = deepLinkHandler
    }

    /**
     * Bind to the browser's history - no-op on native
     */
    public actual fun bindToBrowserHistory() {
        // No browser history on native
    }

    /**
     * Unbind from the browser's history - no-op on native
     */
    public actual fun unbindFromBrowserHistory() {
        // No browser history on native
    }

    /**
     * Check if browser history binding is active - always false on native
     */
    public actual fun isBindingActive(): Boolean {
        return false
    }

    /**
     * Update the browser URL - no-op on native
     */
    public actual fun updateBrowserUrl(path: String) {
        // No browser URL on native
    }

    /**
     * Get current URL path - empty string on native
     */
    public actual fun getCurrentPath(): String {
        return ""
    }

    /**
     * Get a flow of browser back button events - empty flow on native
     */
    public actual fun browserBackEvents(): Flow<Unit> {
        return emptyFlow()
    }

    /**
     * Get a flow of URL change events - empty flow on native
     */
    public actual fun urlChangeEvents(): Flow<String> {
        return emptyFlow()
    }
}

/**
 * Create a web navigation support instance for native platform
 */
public actual fun createWebNavigationSupport(): WebNavigationSupport {
    return WebNavigationSupport()
}