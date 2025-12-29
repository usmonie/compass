@file:OptIn(ExperimentalContracts::class)

package com.usmonie.compass.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

public typealias ProcessAction<A, S, V> = suspend CoroutineScope.(A, S, suspend (V) -> Unit) -> Unit

/**
 * Convenient function to create simple StateViewModel with lambdas
 */
public inline fun <S : State, A : Action, V : Event, F : Effect> createStateViewModel(
    initialState: S,
    crossinline processAction: suspend CoroutineScope.(
        action: A,
        state: S,
        emit: suspend (V) -> Unit,
        launchFlow: suspend (key: SubscriptionKey, block: suspend CoroutineScope.() -> Unit) -> Unit,
    ) -> Unit,
    crossinline handleEvent: (V, S) -> F?,
    crossinline reduce: S.(V) -> S,
    noinline init: suspend StateViewModel<S, A, V, F>.() -> Unit = {},
): StateViewModel<S, A, V, F> = object : StateViewModel<S, A, V, F>(initialState) {

    init {
        viewModelScope.launch {
            init()
        }
    }

    override suspend fun processAction(
        action: A,
        state: S,
        emit: suspend (V) -> Unit,
        launchFlow: suspend (key: SubscriptionKey, block: suspend CoroutineScope.() -> Unit) -> Unit,
    ): Unit = processAction(
        this.viewModelScope,
        action,
        this.state.value,
        emit,
        launchFlow
    )

    override fun handleEvent(event: V): F? =
        handleEvent(event, this.state.value)

    override fun S.reduce(event: V): S =
        reduce(event)
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
