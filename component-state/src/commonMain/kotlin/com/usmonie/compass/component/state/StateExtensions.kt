@file:OptIn(ExperimentalContracts::class)

package com.usmonie.compass.component.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.navEntryDecorator
import com.usmonie.compass.state.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Extension function for StateViewModel to easily observe state changes
 */
@Composable
public fun <S : State, A : Action, V : Event, F : Effect> StateViewModel<S, A, V, F>.observeState(): S {
    return this.state.collectAsState().value
}

/**
 * Extension function for FlowStateViewModel to easily observe state changes
 */
@Composable
public fun <S : State, A : Action, V : Event, F : Effect> FlowStateViewModel<S, A, V, F>.observeState(): S {
    return this.state.collectAsState().value
}

/**
 * Extension function for FlowStateViewModel to easily observe effects
 */
@Composable
public fun <S : State, A : Action, V : Event, F : Effect> FlowStateViewModel<S, A, V, F>.ObserveEffects(
    onEffect: suspend (F) -> Unit
) {
    LaunchedEffect(this) {
        this@ObserveEffects.effect.collect { effect ->
            onEffect(effect)
        }
    }
}

/**
 * Convenient composable for creating stateful UI with StateViewModel
 */
@Composable
public inline fun <S : State, A : Action, V : Event, F : Effect> SimpleStateContent(
    viewModel: StateViewModel<S, A, V, F>,
    noinline onEffect: suspend (F) -> Unit = {},
    crossinline content: @Composable (S, (A) -> Unit) -> Unit
) {
    val state = viewModel.observeState()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            onEffect(effect)
        }
    }

    content(state, viewModel::handleAction)
}

/**
 * Convenient composable for creating stateful UI with minimal boilerplate, using FlowStateViewModel
 */
@Composable
public inline fun <S : State, A : Action, V : Event, F : Effect> StateContent(
    viewModel: FlowStateViewModel<S, A, V, F>,
    noinline onEffect: suspend (F) -> Unit = {},
    crossinline content: @Composable (S, (A) -> Unit) -> Unit
) {
    val state = viewModel.observeState()
    viewModel.ObserveEffects(onEffect)
    content(state, viewModel::handleAction)
}

/**
 * Convenient composable for creating stateful UI with minimal boilerplate, using StateViewModel
 */
@Composable
public inline fun <S : State, A : Action, V : Event, F : Effect> StateContent(
    viewModel: StateViewModel<S, A, V, F>,
    noinline onEffect: suspend (F) -> Unit = {},
    crossinline content: @Composable (S, (A) -> Unit) -> Unit
) {
    val state = viewModel.observeState()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            onEffect(effect)
        }
    }
    content(state, viewModel::handleAction)
}

/**
 * Extension function to create a simple state with loading, success, error states
 */
@Composable
public fun <T> ContentState<T>.onSuccess(action: @Composable (T) -> Unit): ContentState<T> {
    if (this is ContentState.Success) {
        action(data)
    }
    return this
}

@Composable
public fun <T, E : ErrorState> ContentState<T>.onError(action: @Composable (E) -> Unit): ContentState<T> {
    contract { returns() implies (this@onError is ContentState.Error<*, *>) }
    if (this is ContentState.Error<*, *>) {
        @Suppress("UNCHECKED_CAST")
        action(error as E)
    }
    return this
}

@OptIn(ExperimentalContracts::class)
@Composable
public fun <T> ContentState<T>.onLoading(action: @Composable () -> Unit): ContentState<T> {
    contract { returns() implies (this@onLoading is ContentState.Loading) }
    if (this is ContentState.Loading) {
        action()
    }
    return this
}

/**
 * Composition local for accessing the current StateViewModel from navigation context
 */
public val LocalStateViewModel: androidx.compose.runtime.CompositionLocal<StateViewModel<*, *, *, *>?> = compositionLocalOf<StateViewModel<*, *, *, *>?> { null }

/**
 * Composition local for accessing the current FlowStateViewModel from navigation context
 */
public val LocalFlowStateViewModel: androidx.compose.runtime.CompositionLocal<FlowStateViewModel<*, *, *, *>?> = compositionLocalOf<FlowStateViewModel<*, *, *, *>?> { null }

// TODO: NavEntryDecorator integration will be implemented in a future version
// The current androidx.navigation3 implementation needs additional work for proper decorator support