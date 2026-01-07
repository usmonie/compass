package com.usmonie.compass.state

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * An abstract base ViewModel class implementing the MVI (Model-View-Intent) architecture pattern.
 *
 * StateViewModel provides the core infrastructure for managing application state in a reactive,
 * unidirectional way. It handles the complete MVI flow: Actions → Events → State Changes → Effects.
 *
 * Key features:
 * - Type-safe state management with StateFlow
 * - Reactive action processing with coroutines
 * - Automatic state updates based on events
 * - Side effect handling with Flow
 * - Built-in error handling and lifecycle management
 *
 * The MVI flow in this ViewModel:
 * 1. UI sends Actions via handleAction()
 * 2. Actions are processed into Events via processAction()
 * 3. Events are reduced to new States via reduce()
 * 4. Events can optionally produce Effects via handleEvent()
 * 5. UI observes State changes and Effects
 *
 * @param S The type of the component's state - represents the current UI state
 * @param A The type of the user action - represents user intents and interactions
 * @param V The type of the event - represents internal events that trigger state changes
 * @param F The type of the effect - represents one-time side effects (navigation, toasts, etc.)
 *
 * Example implementation:
 * ```kotlin
 * class CounterViewModel : StateViewModel<CounterState, CounterAction, CounterEvent, CounterEffect>(
 *     initialState = CounterState(count = 0)
 * ) {
 *     override suspend fun processAction(action: CounterAction): CounterEvent = when (action) {
 *         CounterAction.Increment -> CounterEvent.Incremented
 *         CounterAction.Decrement -> CounterEvent.Decremented
 *         CounterAction.Reset -> CounterEvent.Reset
 *     }
 *
 *     override fun CounterState.reduce(event: CounterEvent): CounterState = when (event) {
 *         CounterEvent.Incremented -> copy(count = count + 1)
 *         CounterEvent.Decremented -> copy(count = count - 1)
 *         CounterEvent.Reset -> copy(count = 0)
 *     }
 *
 *     override suspend fun handleEvent(event: CounterEvent): CounterEffect? = when (event) {
 *         CounterEvent.Incremented -> if (state.value.count >= 10) CounterEffect.ShowMaxReached else null
 *         else -> null
 *     }
 * }
 * ```
 *
 * @param initialState The initial state of the component
 * @param defaultDispatcher The default coroutine dispatcher that the ViewModel will use
 */

@Immutable
public abstract class StateViewModel<S : State, in A : Action, V : Event, out F : Effect>(
    initialState: S,
    protected val defaultDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : ViewModel {
    private val viewModelJob = SupervisorJob()
    protected val viewModelScope: CoroutineScope = CoroutineScope(viewModelJob + defaultDispatcher)

    private val flowController = FlowController()

    /**
     * The current state as a StateFlow. UI components should observe this to react to state changes.
     *
     * This StateFlow emits new states whenever events are processed and reduce() is called.
     * It guarantees that observers always receive the most current state.
     */
    public val state: StateFlow<S>
        field = MutableStateFlow(initialState)

    private val _effect = Channel<F>()

    /**
     * A Flow of side effects that should be handled by the UI.
     *
     * Effects are one-time events like navigation, showing toasts, or triggering animations.
     * The UI should collect from this Flow and handle each effect appropriately.
     *
     * Note: Only one subscriber per ViewModel is supported for effects.
     */
    public val effect: Flow<F> = _effect.receiveAsFlow()

    /**
     * Handles user actions by converting them into events, then into state changes and possible side effects.
     *
     * This is the main entry point for user interactions. When the UI wants to perform an action,
     * it calls this method. The method processes the action asynchronously and updates the state
     * accordingly.
     *
     * The processing flow:
     * 1. Call processAction() to convert Action to Event
     * 2. Call reduce() to create new State from Event
     * 3. Emit new State to observers
     * 4. Call handleEvent() to potentially create Effect
     * 5. Send Effect to effect Flow if produced
     *
     * @param action The user action to be handled
     */
    public fun handleAction(action: A) {
        viewModelScope.launch {
            processAction(
                action = action,
                state = state.value,
                emit = { event ->
                    handleEventInternal(event)
                },
                launchFlow = { key, block ->
                    launchFlow(key, block)
                }
            )
        }
    }

    private suspend fun handleEventInternal(event: V) {
        val newState = state.value.reduce(event)
        state.emit(newState)

        handleEvent(event)?.let {
            _effect.send(it)
        }
    }

    /**
     * Internal method that handles state updates and effect emission for a given event.
     *
     * @param event The event to process
     */
    protected suspend fun handleState(event: V) {
        val newState = state.value.reduce(event)
        state.emit(newState)
        handleEvent(event)?.let { _effect.send(it) }
    }

    /**
     * Called when an error occurs during action processing or state reduction.
     *
     * By default, this method prints the stack trace. Override this method in subclasses
     * to provide custom error handling, such as emitting error states or effects.
     *
     * @param exception The exception that occurred during processing
     */
    protected open fun onReduceError(exception: Exception) {
        exception.printStackTrace()
    }

    protected open fun mapErrorToEvent(throwable: Throwable): V? = null

    /**
     * Reduces the current state to a new state based on the given event.
     *
     * This method implements the core state transition logic. It should be a pure function
     * that produces a new state based on the current state and the event. It should not
     * have side effects or modify external state.
     *
     * @receiver The current state
     * @param event The event to reduce the state upon
     * @return The new state after applying the event
     */
    protected abstract fun S.reduce(event: V): S

    /**
     * Processes a user action and converts it into an event.
     *
     * This method contains the business logic for handling user actions. It can perform
     * asynchronous operations like API calls, database queries, or other side effects.
     * The result should be an event that represents what happened as a result of the action.
     *
     * @param action The user action to process
     * @return The resulting event that will be used to update the state
     */
    protected abstract suspend fun processAction(
        action: A,
        state: S,
        emit: suspend (V) -> Unit,
        launchFlow: suspend (key: SubscriptionKey, block: suspend CoroutineScope.() -> Unit) -> Unit,
    )

    /**
     * Handles an event and potentially produces a side effect.
     *
     * This method is called after the state has been updated with an event. It can examine
     * the event and current state to determine if any side effects should be triggered.
     * Side effects are things like navigation, showing toasts, or other one-time actions.
     *
     * @param event The event to handle
     * @return The side effect to emit, or null if no side effect is needed
     */
    protected abstract fun handleEvent(event: V): F?

    /**
     * Clears the resources of the ViewModel, particularly cancelling any ongoing coroutine work.
     *
     * This method should be called when the ViewModel is no longer needed to prevent
     * memory leaks and cancel any ongoing operations.
     */
    override fun onDispose() {
        stopAllFlows()
        viewModelJob.cancel()
    }

    /**
     * Launches a coroutine in the ViewModel's scope with an exception handler.
     *
     * This utility method provides a safe way to launch coroutines within the ViewModel,
     * ensuring that uncaught exceptions don't crash the application.
     *
     * @param block The suspending block of code to execute
     * @return The Job representing the launched coroutine
     */
    protected fun CoroutineScope.launchSafe(
        block: suspend CoroutineScope.() -> Unit,
    ): Job = launch(
        CoroutineExceptionHandler { _, throwable ->
            // Default error handling - can be customized by overriding onReduceError
            throwable.printStackTrace()
        }
    ) {
        block()
    }

    protected fun launchFlow(
        key: SubscriptionKey,
        block: suspend CoroutineScope.() -> Unit,
    ) {
        flowController.launch(key, viewModelScope, block)
    }

    protected fun stopFlow(key: SubscriptionKey) {
        flowController.stop(key)
    }

    protected fun stopAllFlows() {
        flowController.stopAll()
    }
}
