package com.usmonie.compass.core.navigation

import com.usmonie.compass.core.GraphId
import com.usmonie.compass.core.navigation.ScreenId
import kotlinx.coroutines.flow.Flow

/**
 * Expect/actual interface for platform-specific web navigation integration
 */
public expect class WebNavigationSupport {
    /**
     * Initialize web navigation binding
     */
    public fun initialize(deepLinkHandler: DeepLinkHandler)

    /**
     * Bind to the browser's history
     */
    public fun bindToBrowserHistory()

    /**
     * Unbind from the browser's history
     */
    public fun unbindFromBrowserHistory()

    /**
     * Check if browser history binding is active
     */
    public fun isBindingActive(): Boolean

    /**
     * Update the browser URL without triggering navigation
     */
    public fun updateBrowserUrl(path: String)

    /**
     * Get current URL path from browser
     */
    public fun getCurrentPath(): String

    /**
     * Get a flow of browser back button events
     */
    public fun browserBackEvents(): Flow<Unit>

    /**
     * Get a flow of URL change events
     */
    public fun urlChangeEvents(): Flow<String>
}

/**
 * Utility for connecting navigation system with web browser history
 */
public class NavigationWebIntegration(
    private val deepLinkHandler: DeepLinkHandler,
    private val webSupport: WebNavigationSupport
) {
    /**
     * Initialize integration with web navigation
     */
    public fun initialize() {
        webSupport.initialize(deepLinkHandler)
    }

    /**
     * Bind navigation controller to browser history
     */
    public fun bindToBrowserHistory() {
        webSupport.bindToBrowserHistory()
    }

    /**
     * Unbind navigation controller from browser history
     */
    public fun unbindFromBrowserHistory() {
        webSupport.unbindFromBrowserHistory()
    }

    /**
     * Helper method to update URL when navigating to a screen
     */
    public fun onNavigateToScreen(
        screenId: ScreenId,
        params: TypedParams = TypedParams.empty()
    ) {
        if (!webSupport.isBindingActive()) return

        // Convert params to string map for URL generation
        val stringParams = params.keys().associateWith { key ->
            params.get<String>(key) ?: ""
        }

        // Try to generate a URL for this screen
        val url = deepLinkHandler.buildScreenUri(screenId, stringParams)
        if (url != null) {
            webSupport.updateBrowserUrl(url)
        }
    }

    /**
     * Helper method to update URL when navigating to a graph
     */
    public fun onNavigateToGraph(
        graphId: GraphId,
        params: TypedParams = TypedParams.empty()
    ) {
        if (!webSupport.isBindingActive()) return

        // Convert params to string map for URL generation
        val stringParams = params.keys().associateWith { key ->
            params.get<String>(key) ?: ""
        }

        // Try to generate a URL for this graph
        val url = deepLinkHandler.buildGraphUri(graphId, stringParams)
        if (url != null) {
            webSupport.updateBrowserUrl(url)
        }
    }

    /**
     * Check if web integration is currently active
     */
    public fun isActive(): Boolean {
        return webSupport.isBindingActive()
    }
}

/**
 * JS implementation for web platform
 */
public expect fun createWebNavigationSupport(): WebNavigationSupport