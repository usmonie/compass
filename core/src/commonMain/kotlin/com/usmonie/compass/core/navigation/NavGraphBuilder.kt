package com.usmonie.compass.core.navigation

import androidx.collection.MutableScatterMap
import androidx.collection.ScatterMap
import androidx.collection.mutableScatterMapOf
import com.usmonie.compass.core.Extra
import com.usmonie.compass.core.GraphId

/**
 * Builder class for constructing navigation graphs in a DSL-style syntax.
 */
public class NavGraphBuilder {
    /**
     * Map of graph factories
     */
    internal val graphs: MutableScatterMap<GraphId, DestinationGraphFactory> = mutableScatterMapOf()

    /**
     * Map of pre-built graphs
     */
    internal val prebuiltGraphs: MutableScatterMap<GraphId, DestinationGraph> = mutableScatterMapOf()

    /**
     * Register a graph factory with the builder
     */
    public fun register(factory: DestinationGraphFactory) {
        graphs[factory.id] = factory
    }

    /**
     * Register a pre-built graph with the builder
     */
    public fun register(graph: DestinationGraph) {
        prebuiltGraphs[graph.id] = graph
    }

    /**
     * Create and register a graph using the provided DSL
     */
    public fun graph(
        id: GraphId,
        builder: GraphBuilder.() -> Unit
    ) {
        val graphBuilder = GraphBuilder(id)
        graphBuilder.apply(builder)
        register(graphBuilder.build())
    }
}

/**
 * Builder class for constructing a single navigation graph
 */
public class GraphBuilder(private val id: GraphId) {
    private val screenFactories: MutableScatterMap<ScreenId, DestinationFactory> = mutableScatterMapOf()
    private var rootScreenFactory: DestinationFactory? = null

    /**
     * Define a screen within this graph
     */
    public fun screen(
        id: ScreenId,
        factory: (Boolean, ScatterMap<String, String>?, Extra?) -> ScreenDestination
    ) {
        val screenFactory = SimpleDestinationFactory(id, factory)
        screenFactories[id] = screenFactory
    }

    /**
     * Define a screen and mark it as the root screen of the graph
     */
    public fun rootScreen(
        id: ScreenId,
        factory: (Boolean, ScatterMap<String, String>?, Extra?) -> ScreenDestination
    ) {
        val screenFactory = SimpleDestinationFactory(id, factory)
        screenFactories[id] = screenFactory
        rootScreenFactory = screenFactory
    }

    /**
     * Build the graph factory from the current configuration
     */
    internal fun build(): DestinationGraphFactory {
        requireNotNull(rootScreenFactory) { "Root screen must be defined for graph $id" }
        return DestinationGraphFactory(
            id = id,
            screenFactories = screenFactories,
            rootScreenFactoryId = rootScreenFactory!!.id
        )
    }
}

/**
 * Type alias for screen parameters to improve readability
 */
public typealias NavParams = ScatterMap<String, String>

/**
 * Type alias for screen extras to improve readability
 */
public typealias NavExtra = Extra