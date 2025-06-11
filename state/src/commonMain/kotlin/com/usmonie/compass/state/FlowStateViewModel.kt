package com.usmonie.compass.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Abstract ViewModel that processes actions into a Flow of events,
 * allowing for multiple, sequential state updates from a single action.
 */
public abstract class FlowStateViewModel<S : State, A_IN : Action, V_EVENT : Event, F_EFFECT : Effect>(
    initialState: S
) : ViewModel {
    private val viewModelJob = SupervisorJob()
    protected val viewModelScope: CoroutineScope =
        CoroutineScope(viewModelJob + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(initialState)
    public val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = Channel<F_EFFECT>()
    public val effect: Flow<F_EFFECT> = _effect.receiveAsFlow()

    /**
     * Handles an incoming action, processes it to a Flow of events, and updates state for each event.
     */
    @Suppress("TooGenericExceptionCaught")
    public fun handleAction(action: A_IN) {
        viewModelScope.launch {
            try {
                // Process the action, which returns a Flow of events
                processAction(action).collect { event ->
                    // Handle each event individually to update state and potentially emit effects
                    handleSingleEvent(event)
                }
            } catch (e: Exception) {
                onReduceError(e)
            }
        }
    }

    /**
     * Processes a single event to update the state and emit an effect.
     */
    protected suspend fun handleSingleEvent(event: V_EVENT) {
        val newState = state.value.reduce(event)
        _state.emit(newState)
        handleEvent(event)?.let { effect -> _effect.send(effect) }
    }

    /**
     * Called when an error occurs during state reduction or action processing.
     */
    protected open fun onReduceError(exception: Exception) {
        // Default implementation prints the stack trace.
        // Override in subclasses to handle errors more specifically (e.g., emitting an error state or effect).
        exception.printStackTrace()
    }

    /**
     * Defines how the current state is reduced to a new state based on an event.
     */
    protected abstract fun S.reduce(event: V_EVENT): S

    /**
     * Processes an action and returns a Flow of events.
     * This allows for actions to result in multiple, sequential state changes.
     */
    protected abstract suspend fun processAction(action: A_IN): Flow<V_EVENT>

    /**
     * Handles an event and potentially produces a side effect.
     */
    protected abstract suspend fun handleEvent(event: V_EVENT): F_EFFECT?

    /**
     * Clears ViewModel resources, cancelling any ongoing coroutines.
     */
    override fun onDispose() {
        viewModelJob.cancel()
    }

    /**
     * Launches a coroutine in the ViewModel's scope with a default exception handler.
     */
    protected fun CoroutineScope.launchSafe(
        block: suspend CoroutineScope.() -> Unit
    ): kotlinx.coroutines.Job = launch(
        kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
            // Default error handling: print stack trace.
            // Consider customizing this for specific ViewModel needs.
            throwable.printStackTrace()
        }
    ) {
        block()
    }
}