package com.usmonie.compass.screen.state.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent

public fun <T : Any> predictivePopSlideTransitionSpec():
        AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform =
    {
        val slideOut = slideOutOfContainer(
            when (it) {
                NavigationEvent.EDGE_RIGHT -> AnimatedContentTransitionScope.SlideDirection.Start
                NavigationEvent.EDGE_LEFT -> AnimatedContentTransitionScope.SlideDirection.End
                else -> AnimatedContentTransitionScope.SlideDirection.End
            },
            tween(easing = FastOutSlowInEasing)
        )
        ContentTransform(
            fadeIn(spring(stiffness = Spring.StiffnessVeryLow)) + scaleIn(initialScale = 0.9f),
            scaleOut(
                targetScale = 0.8f,
                animationSpec = spring(stiffness = 3000f)
            ) + slideOut,
        )
    }
