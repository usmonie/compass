package com.usmonie.compass.core.navigation

import kotlinx.browser.window
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.w3c.dom.events.Event

/**
 * JS implementation of WebNavigationSupport that integrates with browser history
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
     * Bind to the browser's history
     */
    public actual fun bindToBrowserHistory() {
        if (isActive || deepLinkHandler == null) return

        // Set up popstate listener to handle browser back button
        window.addEventListener("popstate", { event ->
            handleUrlChange(window.location.pathname)
        })

        // Handle initial URL on page load
        handleUrlChange(window.location.pathname)

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
     * Update the browser URL without triggering navigation
     */
    public actual fun updateBrowserUrl(path: String) {
        if (!isActive) return

        // Don't update if we're already at this URL
        if (window.location.pathname == path) {
            return
        }

        // Use pushState to update URL without page reload
        window.history.pushState(null, "", path)
    }

    /**
     * Get current URL path from browser
     */
    public actual fun getCurrentPath(): String {
        return window.location.pathname
    }

    /**
     * Get a flow of browser back button events
     */
    public actual fun browserBackEvents(): Flow<Unit> = callbackFlow {
        val listener: (Event) -> Unit = {
            trySend(Unit)
        }

        window.addEventListener("popstate", listener)

        awaitClose {
            window.removeEventListener("popstate", listener)
        }
    }

    /**
     * Get a flow of URL change events
     */
    public actual fun urlChangeEvents(): Flow<String> = callbackFlow {
        val listener: (Event) -> Unit = {
            trySend(window.location.pathname)
        }

        window.addEventListener("popstate", listener)

        awaitClose {
            window.removeEventListener("popstate", listener)
        }
    }

    /**
     * Handle URL changes by parsing the path and navigating
     */
    private fun handleUrlChange(path: String) {
        val deepLinkHandler = this.deepLinkHandler ?: return

        // Try to parse the URL as a deep link
        val result = deepLinkHandler.parseDeepLink(path)

        when (result) {
            is DeepLinkResult.Screen -> {
                // Navigate to screen with parsed parameters
                // Navigation will be handled via composition local
                println("Navigate to screen: ${result.screenId}")
            }

            is DeepLinkResult.Graph -> {
                // Navigate to graph with parsed parameters  
                // Navigation will be handled via composition local
                println("Navigate to graph: ${result.graphId}")
            }

            null -> {
                // URL doesn't match any registered deep links
                console.warn("No matching destination found for path: $path")
            }
        }
    }
}

/**
 * Create a web navigation support instance for JS platform
 */
public actual fun createWebNavigationSupport(): WebNavigationSupport {
    return WebNavigationSupport()
}