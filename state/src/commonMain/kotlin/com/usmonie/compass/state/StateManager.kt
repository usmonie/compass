package com.usmonie.compass.state

/**
 * Interface for managing state transitions in MVI architecture.
 *
 * StateManager is responsible for producing new states based on current states and events.
 * It implements the core state reduction logic that determines how the application state
 * should change in response to various events.
 *
 * This interface encapsulates the pure state transition logic, ensuring that state changes
 * are predictable, testable, and side-effect free.
 *
 * @param State The type of state this manager handles
 * @param Event The type of events that can trigger state changes
 *
 * Example usage:
 * ```kotlin
 * val stateManager = StateManager<CounterState, CounterEvent> { state, event ->
 *     when (event) {
 *         CounterEvent.Incremented -> state.copy(count = state.count + 1)
 *         CounterEvent.Decremented -> state.copy(count = state.count - 1)
 *         CounterEvent.Reset -> state.copy(count = 0)
 *     }
 * }
 * ```
 */
public fun interface StateManager<State : com.usmonie.compass.state.State, in Event : com.usmonie.compass.state.Event> {
    /**
     * Reduces the current state to a new state based on the given event.
     *
     * This method implements the state reduction logic. It should be a pure function
     * that produces a new state based on the current state and the event that occurred.
     * It should not have side effects or modify the input state directly.
     *
     * @param state The current state
     * @param event The event that should trigger a state change
     * @return The new state after applying the event
     */
    public fun reduce(state: State, event: Event): State
}
