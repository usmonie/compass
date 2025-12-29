package com.usmonie.compass.state

import kotlinx.coroutines.CoroutineScope

/**
 * Interface for processing user actions into events in the MVI architecture.
 *
 * This component is responsible for converting user intents (Actions) into internal events
 * that represent what should happen in response to those actions. It acts as the business
 * logic layer that processes user interactions.
 *
 * @param Action The type of user actions that this processor can handle
 * @param State The type of the current application state
 * @param Event The type of events that this processor produces
 *
 * Example usage:
 * ```kotlin
 * val processor = ActionProcessor<CounterAction, CounterState, CounterEvent> { scope, action, state ->
 *     when (action) {
 *         CounterAction.Increment -> CounterEvent.Incremented
 *         CounterAction.Decrement -> CounterEvent.Decremented
 *         CounterAction.Reset -> CounterEvent.Reset
 *     }
 * }
 * ```
 */
public fun interface ActionProcessor<in Action : com.usmonie.compass.state.Action, State : com.usmonie.compass.state.State, out Event : com.usmonie.compass.state.Event> {
    /**
     * Processes a user action and produces an event based on the current state.
     *
     * This method is called whenever a user action needs to be processed. It receives
     * the current state as context and should return an event that represents what
     * should happen as a result of the action.
     *
     * @param coroutineScope The coroutine scope in which this processing occurs
     * @param action The user action to be processed
     * @param state The current application state at the time of processing
     * @return An event representing the result of processing the action
     */
    public suspend fun process(
        coroutineScope: CoroutineScope,
        action: Action,
        state: State,
        emit: suspend (Event) -> Unit,
        launchFlow: suspend (key: SubscriptionKey, block: suspend CoroutineScope.() -> Unit) -> Unit,
    )
}
