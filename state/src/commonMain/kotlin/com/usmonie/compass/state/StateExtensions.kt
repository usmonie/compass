@file:OptIn(ExperimentalContracts::class)

package com.usmonie.compass.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Convenient function to create simple StateViewModel with lambdas
 */
public inline fun <S : State, A : Action, V : Event, F : Effect> createStateViewModel(
    initialState: S,
    crossinline processAction: suspend CoroutineScope.(A, S) -> V,
    crossinline handleEvent: (V, S) -> F?,
    crossinline reduce: S.(V) -> S,
    noinline init: suspend StateViewModel<S, A, V, F>.() -> Unit = {},
): StateViewModel<S, A, V, F> = object : StateViewModel<S, A, V, F>(initialState) {

    init {
        viewModelScope.launch {
            init()
        }
    }

    override suspend fun processAction(action: A): V =
        processAction(this.viewModelScope, action, this.state.value)

    override suspend fun handleEvent(event: V): F? =
        handleEvent(event, this.state.value)

    override fun S.reduce(event: V): S =
        reduce(event)
}

/**
 * Extension function to create a FlowStateViewModel with less boilerplate.
 */
public inline fun <S_STATE : State, A_ACTION : Action, V_EVENT : Event, F_EFFECT : Effect> createFlowViewModel(
    initialState: S_STATE,
    crossinline processActionFlow: suspend CoroutineScope.(A_ACTION, S_STATE) -> Flow<V_EVENT>,
    crossinline handleEffect: (V_EVENT, S_STATE) -> F_EFFECT?,
    crossinline reduceState: S_STATE.(V_EVENT) -> S_STATE,
    crossinline init: suspend FlowStateViewModel<S_STATE, A_ACTION, V_EVENT, F_EFFECT>.() -> Unit = {},
): FlowStateViewModel<S_STATE, A_ACTION, V_EVENT, F_EFFECT> =
    object : FlowStateViewModel<S_STATE, A_ACTION, V_EVENT, F_EFFECT>(initialState) {
        init {
            viewModelScope.launch {
                init()
            }
        }

        override suspend fun processAction(action: A_ACTION): Flow<V_EVENT> =
            this.viewModelScope.processActionFlow(action, this.state.value)

        override suspend fun handleEvent(event: V_EVENT): F_EFFECT? =
            handleEffect(event, this.state.value)

        override fun S_STATE.reduce(event: V_EVENT): S_STATE =
            this.reduceState(event)
    }

/**
 * ActionProcessor that can emit multiple events.
 */
public fun interface FlowActionProcessor<ACTION_TYPE : Action, STATE_TYPE : State, out EVENT_TYPE : Event> {
    public suspend fun process(
        coroutineScope: CoroutineScope,
        action: ACTION_TYPE,
        state: STATE_TYPE
    ): Flow<EVENT_TYPE>
}

/**
 * Extension function to create a FlowActionProcessor
 */
public inline fun <A_ACTION : Action, S_STATE : State, V_EVENT : Event> flowActionProcessor(
    crossinline process: suspend CoroutineScope.(A_ACTION, S_STATE) -> Flow<V_EVENT>
): FlowActionProcessor<A_ACTION, S_STATE, V_EVENT> = FlowActionProcessor { scope, action, state ->
    process(scope, action, state)
}

/**
 * Extension function to create a simple ActionProcessor (Single Event)
 */
public inline fun <A_ACTION : Action, S_STATE : State, V_EVENT : Event> actionProcessor(
    crossinline process: suspend CoroutineScope.(A_ACTION, S_STATE) -> V_EVENT
): ActionProcessor<A_ACTION, S_STATE, V_EVENT> = ActionProcessor { scope, action, state ->
    scope.process(action, state)
}

/**
 * Extension function to create a simple StateManager
 */
public inline fun <S : State, V : Event> stateManager(
    crossinline reduce: S.(V) -> S
): StateManager<S, V> = StateManager { state, event ->
    state.reduce(event)
}

/**
 * Extension function to create a simple EventHandler
 */
public inline fun <V : Event, S : State, F : Effect> eventHandler(
    crossinline handle: (V, S) -> F?
): EventHandler<V, S, F> = EventHandler { event, state ->
    handle(event, state)
}

/**
 * Convenient function to create StateViewModel with lambdas
 */
public inline fun <S : State, A : Action, V : Event, F : Effect> stateViewModel(
    initialState: S,
    crossinline processAction: suspend CoroutineScope.(A, S) -> V,
    crossinline handleEvent: (V, S) -> F?,
    crossinline reduce: S.(V) -> S,
    noinline init: suspend StateViewModel<S, A, V, F>.() -> Unit = {},
): StateViewModel<S, A, V, F> = object : StateViewModel<S, A, V, F>(initialState) {

    init {
        viewModelScope.launch {
            init()
        }
    }

    override suspend fun processAction(action: A): V =
        processAction(this.viewModelScope, action, this.state.value)

    override suspend fun handleEvent(event: V): F? {
        return handleEvent(event, this.state.value)
    }

    override fun S.reduce(event: V): S =
        reduce(event)
}

/**
 * Convenient function to create FlowStateViewModel with lambdas
 */
public inline fun <S : State, A : Action, V : Event, F : Effect> flowStateViewModel(
    initialState: S,
    crossinline processAction: suspend CoroutineScope.(A, S) -> Flow<V>,
    crossinline handleEvent: (V, S) -> F?,
    crossinline reduce: S.(V) -> S,
    noinline init: suspend FlowStateViewModel<S, A, V, F>.() -> Unit = {},
): FlowStateViewModel<S, A, V, F> = object : FlowStateViewModel<S, A, V, F>(initialState) {

    init {
        viewModelScope.launch {
            init()
        }
    }

    override suspend fun processAction(action: A): Flow<V> =
        processAction(this.viewModelScope, action, this.state.value)

    override suspend fun handleEvent(event: V): F? {
        return handleEvent(event, this.state.value)
    }

    override fun S.reduce(event: V): S = reduce(event)
}

/**
 * Extension function to create a simple state with loading, success, error states
 */
public fun <T> ContentState<T>.onSuccess(action: (T) -> Unit): ContentState<T> {
    if (this is ContentState.Success) {
        action(data)
    }
    return this
}

public fun <T, E : ErrorState> ContentState<T>.onError(action: (E) -> Unit): ContentState<T> {
    contract { returns() implies (this@onError is ContentState.Error<*, *>) }
    if (this is ContentState.Error<*, *>) {
        @Suppress("UNCHECKED_CAST")
        action(error as E)
    }
    return this
}

@OptIn(ExperimentalContracts::class)
public fun <T> ContentState<T>.onLoading(action: () -> Unit): ContentState<T> {
    contract { returns() implies (this@onLoading is ContentState.Loading) }
    if (this is ContentState.Loading) {
        action()
    }
    return this
}

/**
 * Extension function to map ContentState data
 */
public inline fun <T, R> ContentState<T>.map(transform: (T) -> R): ContentState<R> {
    return when (this) {
        is ContentState.Success -> ContentState.Success(transform(data))
        is ContentState.Error<*, *> -> ContentState.Error(error)
        is ContentState.Loading -> ContentState.Loading()
    }
}

/**
 * Extension function to flatMap ContentState
 */
public inline fun <T, R> ContentState<T>.flatMap(transform: (T) -> ContentState<R>): ContentState<R> {
    return when (this) {
        is ContentState.Success -> transform(data)
        is ContentState.Error<*, *> -> ContentState.Error(error)
        is ContentState.Loading -> ContentState.Loading()
    }
}
