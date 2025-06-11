package com.usmonie.compass.core.navigation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

/**
 * Processor for back navigation gestures that handles the flow of gesture events
 */
internal class BackProcessor(
    scope: CoroutineScope,
    val isPredictive: Boolean,
    onBack: suspend (progress: Flow<NavigationGesture>) -> Boolean,
) {
    /**
     * Channel for gesture events
     */
    private val channel = Channel<NavigationGesture>(
        capacity = BUFFERED,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    /**
     * Job handling the gesture flow
     */
    private val job = scope.launch {
        var completed = false
        onBack(
            channel
                .consumeAsFlow()
                .onCompletion { completed = true }
        )
        check(completed) {
            "You must collect the gesture flow"
        }
    }

    /**
     * Send a gesture event to the channel
     */
    fun send(gesture: NavigationGesture) = channel.trySend(gesture)

    /**
     * Close the channel (idempotent)
     */
    fun close() = channel.close()

    /**
     * Cancel the processor
     */
    fun cancel() {
        channel.cancel(CancellationException("Back navigation cancelled"))
        job.cancel()
    }
}