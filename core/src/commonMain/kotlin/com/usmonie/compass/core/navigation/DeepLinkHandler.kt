package com.usmonie.compass.core.navigation

import com.usmonie.compass.core.GraphId

/**
 * Utility for handling deep links in the navigation system
 */
public class DeepLinkHandler(
    private val baseUrl: String = ""
) {
    // Registry of deep link patterns to screen or graph matches
    private val deepLinkRegistry = mutableMapOf<DeepLinkPattern, DeepLinkDestination>()

    /**
     * Register a screen with a deep link pattern
     */
    public fun registerScreen(
        pattern: String,
        screenId: ScreenId,
        paramExtractor: (DeepLinkMatch) -> TypedParams = { TypedParams.empty() }
    ) {
        val deepLinkPattern = DeepLinkPattern(pattern)
        deepLinkRegistry[deepLinkPattern] = DeepLinkDestination.Screen(
            screenId = screenId,
            paramExtractor = paramExtractor
        )
    }

    /**
     * Register a navigation graph with a deep link pattern
     */
    public fun registerGraph(
        pattern: String,
        graphId: GraphId,
        paramExtractor: (DeepLinkMatch) -> TypedParams = { TypedParams.empty() }
    ) {
        val deepLinkPattern = DeepLinkPattern(pattern)
        deepLinkRegistry[deepLinkPattern] = DeepLinkDestination.Graph(
            graphId = graphId,
            paramExtractor = paramExtractor
        )
    }

    /**
     * Parse a deep link and find matching destination
     *
     * @return A DeepLinkResult containing the matching destination and extracted params
     */
    public fun parseDeepLink(deepLink: String): DeepLinkResult? {
        val uri = normalizeUri(deepLink)

        // Try to match against registered patterns
        deepLinkRegistry.forEach { (pattern, destination) ->
            val match = pattern.matchUri(uri)
            if (match != null) {
                val params = when (destination) {
                    is DeepLinkDestination.Screen -> destination.paramExtractor(match)
                    is DeepLinkDestination.Graph -> destination.paramExtractor(match)
                }

                return when (destination) {
                    is DeepLinkDestination.Screen -> DeepLinkResult.Screen(
                        screenId = destination.screenId,
                        params = params
                    )

                    is DeepLinkDestination.Graph -> DeepLinkResult.Graph(
                        graphId = destination.graphId,
                        params = params
                    )
                }
            }
        }

        return null
    }

    /**
     * Normalize the URI by ensuring it has the base URL if needed
     */
    private fun normalizeUri(uri: String): String {
        if (baseUrl.isEmpty()) return uri
        return if (uri.startsWith(baseUrl)) uri else "$baseUrl$uri"
    }
}

/**
 * Pattern for matching deep links
 */
public class DeepLinkPattern(pattern: String) {
    // Parts of the pattern, with placeholders marked for parameter extraction
    private val patternParts = parsePattern(pattern)

    /**
     * Match URI against this pattern
     */
    public fun matchUri(uri: String): DeepLinkMatch? {
        val uriParts = uri.split("/").filter { it.isNotEmpty() }

        // Quick length check
        if (patternParts.size != uriParts.size) {
            return null
        }

        val parameters = mutableMapOf<String, String>()

        // Match each part of the pattern
        for (i in patternParts.indices) {
            val patternPart = patternParts[i]
            val uriPart = uriParts[i]

            when {
                patternPart.isParameter -> {
                    // Extract parameter value
                    parameters[patternPart.value] = uriPart
                }

                patternPart.value != uriPart -> {
                    // Static parts must match exactly
                    return null
                }
            }
        }

        return DeepLinkMatch(parameters)
    }

    /**
     * Parse pattern string into parts for matching
     */
    private fun parsePattern(pattern: String): List<PatternPart> {
        return pattern.split("/").filter { it.isNotEmpty() }.map { part ->
            if (part.startsWith("{") && part.endsWith("}")) {
                // Extract parameter name without braces
                PatternPart(part.substring(1, part.length - 1), true)
            } else {
                PatternPart(part, false)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeepLinkPattern) return false
        return patternParts == other.patternParts
    }

    override fun hashCode(): Int {
        return patternParts.hashCode()
    }

    /**
     * Part of a deep link pattern
     */
    private data class PatternPart(val value: String, val isParameter: Boolean)
}

/**
 * Match result from deep link pattern matching
 */
public class DeepLinkMatch(
    public val parameters: Map<String, String>
) {
    /**
     * Get a parameter value by name
     */
    public fun getParameter(name: String): String? {
        return parameters[name]
    }
}

/**
 * Destination for a deep link
 */
public sealed class DeepLinkDestination {
    /**
     * Screen destination for a deep link
     */
    public class Screen(
        public val screenId: ScreenId,
        public val paramExtractor: (DeepLinkMatch) -> TypedParams
    ) : DeepLinkDestination()

    /**
     * Graph destination for a deep link
     */
    public class Graph(
        public val graphId: GraphId,
        public val paramExtractor: (DeepLinkMatch) -> TypedParams
    ) : DeepLinkDestination()
}

/**
 * Result of parsing a deep link
 */
public sealed class DeepLinkResult {
    /**
     * Deep link points to a screen
     */
    public class Screen(
        public val screenId: ScreenId,
        public val params: TypedParams
    ) : DeepLinkResult()

    /**
     * Deep link points to a graph
     */
    public class Graph(
        public val graphId: GraphId,
        public val params: TypedParams
    ) : DeepLinkResult()
}

/**
 * Extension function to build deep link URI for a screen
 */
public fun DeepLinkHandler.buildScreenUri(
    screenId: ScreenId,
    params: Map<String, String> = emptyMap()
): String? {
    // This is a simplified implementation - in a real app, 
    // we would need to find the pattern for this screen and substitute parameters
    return null // Placeholder for real implementation
}

/**
 * Extension function to build deep link URI for a graph
 */
public fun DeepLinkHandler.buildGraphUri(
    graphId: GraphId,
    params: Map<String, String> = emptyMap()
): String? {
    // This is a simplified implementation - in a real app,
    // we would need to find the pattern for this graph and substitute parameters
    return null // Placeholder for real implementation
}