package com.usmonie.compass.core.navigation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * iOS implementation of WebNavigationSupport.
 *
 * On iOS, web navigation features are not applicable, so this is a minimal implementation
 * that allows the code to compile but doesn't provide actual web functionality.
 */
public actual class WebNavigationSupport {
    private var navController: NavController? = null
    private var deepLinkHandler: DeepLinkHandler? = null

    /**
     * Initialize web navigation binding - minimal implementation for iOS
     */
    public actual fun initialize(navController: NavController, deepLinkHandler: DeepLinkHandler) {
        this.navController = navController
        this.deepLinkHandler = deepLinkHandler
    }

    /**
     * Bind to the browser's history - no-op on iOS
     */
    public actual fun bindToBrowserHistory() {
        // No browser history on iOS
    }

    /**
     * Unbind from the browser's history - no-op on iOS
     */
    public actual fun unbindFromBrowserHistory() {
        // No browser history on iOS
    }

    /**
     * Check if browser history binding is active - always false on iOS
     */
    public actual fun isBindingActive(): Boolean {
        return false
    }

    /**
     * Update the browser URL - no-op on iOS
     */
    public actual fun updateBrowserUrl(path: String) {
        // No browser URL on iOS
    }

    /**
     * Get current URL path - empty string on iOS
     */
    public actual fun getCurrentPath(): String {
        return ""
    }

    /**
     * Get a flow of browser back button events - empty flow on iOS
     */
    public actual fun browserBackEvents(): Flow<Unit> {
        return emptyFlow()
    }

    /**
     * Get a flow of URL change events - empty flow on iOS
     */
    public actual fun urlChangeEvents(): Flow<String> {
        return emptyFlow()
    }
}

/**
 * Create a web navigation support instance for iOS platform
 */
public actual fun createWebNavigationSupport(): WebNavigationSupport {
    return WebNavigationSupport()
}