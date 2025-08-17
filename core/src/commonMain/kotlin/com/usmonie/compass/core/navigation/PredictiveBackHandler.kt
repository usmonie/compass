package com.usmonie.compass.core.navigation

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

/**
 * Platform-specific implementation of predictive back handler.
 * 
 * This composable handles back gestures and provides predictive back animations
 * where supported by the platform.
 * 
 * @param enabled Whether the back handler is enabled
 * @param onBack Callback for handling back gestures with predictive feedback
 * @param onBackPressed Callback for simple back button presses
 * @param content The content to display
 */
@Composable
public expect fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (Flow<NavigationGesture>) -> Boolean,
    onBackPressed: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
)
