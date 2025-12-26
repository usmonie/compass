package com.usmonie.compass.screen.state.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent

public fun <T : Any> predictivePopSlideTransitionSpec():
        AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform =
    {
        ContentTransform(
            fadeIn(
                spring(
                    dampingRatio = 1.0f, // reflects material3 motionScheme.defaultEffectsSpec()
                    stiffness = 1600.0f, // reflects material3 motionScheme.defaultEffectsSpec()
                )
            ) + scaleIn(initialScale = 0.95f),
            scaleOut(targetScale = 0.8f) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End),
        )
    }
