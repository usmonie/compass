package com.usmonie.compass.core.navigation

import androidx.collection.ScatterMap
import com.usmonie.compass.core.Extra
import com.usmonie.compass.core.navigation.ScreenId

/**
 * Factory interface for creating screen destinations
 */
public interface DestinationFactory {
    /**
     * The ID of the screen this factory creates
     */
    public val id: ScreenId

    /**
     * Creates a new screen destination
     */
    public operator fun invoke(
        storeInBackStack: Boolean,
        params: ScatterMap<String, String>?,
        extra: Extra?
    ): ScreenDestination
}

/**
 * Simple implementation of DestinationFactory
 */
public class SimpleDestinationFactory(
    override val id: ScreenId,
    private val creator: (Boolean, ScatterMap<String, String>?, Extra?) -> ScreenDestination
) : DestinationFactory {
    override fun invoke(
        storeInBackStack: Boolean, 
        params: ScatterMap<String, String>?, 
        extra: Extra?
    ): ScreenDestination {
        return creator(storeInBackStack, params, extra)
    }
}