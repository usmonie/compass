package com.usmonie.compass.core.navigation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Android implementation of WebNavigationSupport.
 *
 * On Android, web navigation features are not applicable, so this is a minimal implementation
 * that allows the code to compile but doesn't provide actual web functionality.
 */
public actual class WebNavigationSupport {
    private var deepLinkHandler: DeepLinkHandler? = null

    /**
     * Initialize web navigation binding - minimal implementation for Android
     */
    public actual fun initialize(deepLinkHandler: DeepLinkHandler) {
        this.deepLinkHandler = deepLinkHandler
    }

    /**
     * Bind to the browser's history - no-op on Android
     */
    public actual fun bindToBrowserHistory() {
        // No browser history on Android
    }

    /**
     * Unbind from the browser's history - no-op on Android
     */
    public actual fun unbindFromBrowserHistory() {
        // No browser history on Android
    }

    /**
     * Check if browser history binding is active - always false on Android
     */
    public actual fun isBindingActive(): Boolean {
        return false
    }

    /**
     * Update the browser URL - no-op on Android
     */
    public actual fun updateBrowserUrl(path: String) {
        // No browser URL on Android
    }

    /**
     * Get current URL path - empty string on Android
     */
    public actual fun getCurrentPath(): String {
        return ""
    }

    /**
     * Get a flow of browser back button events - empty flow on Android
     */
    public actual fun browserBackEvents(): Flow<Unit> {
        return emptyFlow()
    }

    /**
     * Get a flow of URL change events - empty flow on Android
     */
    public actual fun urlChangeEvents(): Flow<String> {
        return emptyFlow()
    }
}

/**
 * Create a web navigation support instance for Android platform
 */
public actual fun createWebNavigationSupport(): WebNavigationSupport {
    return WebNavigationSupport()
}