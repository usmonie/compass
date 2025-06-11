package com.usmonie.compass.core

import com.usmonie.compass.core.navigation.ScreenId

/**
 * Represents a shared element for transitions between screens.
 */
public data class SharedElement(
    val key: String,
    val screenFromId: ScreenId,
    val screenToId: ScreenId
)
