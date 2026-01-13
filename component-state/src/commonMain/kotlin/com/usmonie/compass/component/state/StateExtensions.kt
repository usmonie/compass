@file:OptIn(ExperimentalContracts::class)

package com.usmonie.compass.component.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.usmonie.compass.state.Action
import com.usmonie.compass.state.ContentState
import com.usmonie.compass.state.Effect
import com.usmonie.compass.state.ErrorState
import com.usmonie.compass.state.Event
import com.usmonie.compass.state.State
import com.usmonie.compass.state.StateViewModel
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Extension function for StateViewModel to easily observe state changes
 */
@Composable
public fun <S : State, A : Action, V : Event, F : Effect> StateViewModel<S, A, V, F>.observeState(): androidx.compose.runtime.State<S> {
    return this.state.collectAsState()
}

@Composable
public fun <S : State, A : Action, V : Event, F : Effect> StateViewModel<S, A, V, F>.observeEffect(): androidx.compose.runtime.State<F?> {
    return this.effect.collectAsState(null)
}

/**
 * Convenient composable for creating stateful UI with StateViewModel
 */
@Composable
public inline fun <S : State, A : Action, V : Event, F : Effect> SimpleStateContent(
    viewModel: StateViewModel<S, A, V, F>,
    noinline onEffect: suspend (F) -> Unit = {},
    crossinline content: @Composable (S, (A) -> Unit) -> Unit,
) {
    val state by viewModel.observeState()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            onEffect(effect)
        }
    }

    content(state, viewModel::handleAction)
}

/**
 * Convenient composable for creating stateful UI with minimal boilerplate, using StateViewModel
 */
@Composable
public inline fun <S : State, A : Action, V : Event, F : Effect> StateContent(
    viewModel: StateViewModel<S, A, V, F>,
    crossinline onEffect: @Composable (S, F?) -> Unit = { _, _ -> },
    crossinline content: @Composable (S, (A) -> Unit) -> Unit,
) {
    val state by viewModel.observeState()
    val effect by viewModel.observeEffect()
    onEffect(state, effect)
    content(state, remember(viewModel) { { viewModel.handleAction(it) } })
}

/**
 * Extension function to create a simple state with loading, success, error states
 */
@Composable
public inline fun <reified T> ContentState<T>.onSuccess(content: @Composable (T) -> Unit): ContentState<T> {
    if (this is ContentState.Success) {
        content(data)
    }
    return this
}

@Composable
public inline fun <T, reified E : ErrorState> ContentState<T>.onError(content: @Composable (E) -> Unit): ContentState<T> {
    contract { returns() implies (this@onError is ContentState.Error<*, *>) }
    if (this is ContentState.Error<*, *>) {
        @Suppress("UNCHECKED_CAST")
        content(error as E)
    }
    return this
}

@OptIn(ExperimentalContracts::class)
@Composable
public inline fun <reified T> ContentState<T>.onLoading(content: @Composable () -> Unit): ContentState<T> {
    contract { returns() implies (this@onLoading is ContentState.Loading) }
    if (this is ContentState.Loading) {
        content()
    }
    return this
}