package com.usmonie.compass.core.navigation

import androidx.collection.MutableScatterMap
import androidx.collection.ObjectList
import androidx.collection.ScatterMap
import androidx.collection.emptyScatterMap
import androidx.collection.mutableScatterMapOf
import com.usmonie.compass.core.Extra
import com.usmonie.compass.core.GraphId
import com.usmonie.compass.core.SharedElement
import com.usmonie.compass.core.navigation.ScreenId

/**
 * Factory for creating destination graphs
 */
public class DestinationGraphFactory(
    public val id: GraphId,
    public val screenFactories: ScatterMap<ScreenId, DestinationFactory>,
    public val rootScreenFactoryId: ScreenId,
    private val builder: DestinationGraph.() -> Unit = {}
) {
    public operator fun invoke(
        params: ScatterMap<String, String> = emptyScatterMap(),
        extra: Extra? = null,
        storeInBackstack: Boolean
    ): DestinationGraph {
        return DestinationGraph(
            id,
            rootScreenFactoryId,
            screenFactories,
            extra,
            params,
        ).apply(builder)
    }
}

/**
 * Graph containing screen destinations
 */
public class DestinationGraph(
    public val id: GraphId,
    public val rootScreenFactory: DestinationFactory,
    public val extra: Extra? = null,
    public val params: ScatterMap<String, String> = emptyScatterMap(),
) {
    private val screenFactories: MutableScatterMap<ScreenId, DestinationFactory> =
        mutableScatterMapOf(rootScreenFactory.id to rootScreenFactory)

    public constructor(
        id: GraphId,
        rootScreenFactoryId: ScreenId,
        screenFactories: ScatterMap<ScreenId, DestinationFactory>,
        extra: Extra? = null,
        params: ScatterMap<String, String> = emptyScatterMap(),
    ) : this(
        id,
        screenFactories.getOrElse(
            rootScreenFactoryId
        ) { throw IllegalArgumentException("Not found factory for root screen with id: $rootScreenFactoryId") },
        extra,
        params,
    ) {
        this.screenFactories.remove(rootScreenFactoryId)
        this.screenFactories.putAll(screenFactories)
    }

    /**
     * Register a screen factory with this graph
     */
    public fun register(factory: DestinationFactory) {
        screenFactories[factory.id] = factory
    }

    /**
     * Find a screen by ID
     */
    public fun findScreen(
        screenId: ScreenId,
        storeInBackstack: Boolean,
        params: ScatterMap<String, String>?,
        extras: Extra?,
        sharedElements: ObjectList<SharedElement>
    ): ScreenDestination? {
        val factory = screenFactories[screenId] ?: return null
        return factory(storeInBackstack, params, extras)
    }
}
