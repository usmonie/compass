package com.usmonie.compass.screen.state.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.usmonie.compass.state.ViewModel
import kotlinx.serialization.Serializable

/**
 * Base class representing a destination screen in the navigation system.
 * Provides clearer lifecycle management and simplified transitions.
 */
@Immutable
@Serializable
public abstract class ScreenDestination<T : ScreenId>(
    public val key: T,
    public val storeInBackStack: Boolean = true
) : ViewModel {
    /**
     * The content to be displayed
     */
    @Composable
    public abstract fun Content()

    override fun onDispose() {}
}
