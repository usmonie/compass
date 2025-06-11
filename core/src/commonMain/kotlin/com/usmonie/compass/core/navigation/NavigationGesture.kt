package com.usmonie.compass.core.navigation

/**
 * Represents a navigation gesture event for handling back navigation.
 */
public sealed class NavigationGesture {
    /**
     * Gesture starting event with initial touch position
     */
    public data class Start(
        val positionX: Float,
        val positionY: Float,
        val screenWidth: Float,
        val edge: GestureEdge,
    ) : NavigationGesture()

    /**
     * Gesture sliding event with updated position
     */
    public data class Sliding(
        val positionX: Float,
        val positionY: Float,
        val screenWidth: Float,
        val edge: GestureEdge,
    ) : NavigationGesture()

    /**
     * Gesture end event
     */
    public data class End(val screenWidth: Float) : NavigationGesture()
}

/**
 * Represents the edge from which the gesture originated
 */
public enum class GestureEdge {
    /**
     * Gesture from left edge to right (common on Android)
     */
    LEFT_TO_RIGHT,

    /**
     * Gesture from right edge to left (common on iOS)
     */
    RIGHT_TO_LEFT,
}

/**
 * Platform-specific property to determine if gesture navigation is supported
 */
public expect val isGestureNavigationSupported: Boolean