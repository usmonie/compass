package com.usmonie.compass.component.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * A managed StateComponent that provides commonly used state operations
 */
public abstract class ManagedStateComponent<
        S : com.usmonie.compass.state.State,
        A : com.usmonie.compass.state.Action,
        V : com.usmonie.compass.state.Event,
        F : com.usmonie.compass.state.Effect,
        VM : com.usmonie.compass.state.StateViewModel<S, A, V, F>
        >(viewModel: VM) :
    StateComponent<S, A, V, F, VM>(viewModel) {

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsState()

        LaunchedEffect(viewModel) {
            viewModel.effect.collect { effect ->
                onEffect(effect)
            }
        }

        Content(
            state = state.value,
            onAction = viewModel::handleAction
        )
    }

    /**
     * Handle side effects from the ViewModel
     */
    protected open suspend fun onEffect(effect: F) {}

    /**
     * Render the component's content with state and action handler
     */
    @Composable
    protected abstract fun Content(
        state: S,
        onAction: (A) -> Unit
    )
}

/**
 * Composable function to create a stateful component with automatic state management
 */
@Composable
public inline fun <
        S : com.usmonie.compass.state.State,
        A : com.usmonie.compass.state.Action,
        V : com.usmonie.compass.state.Event,
        F : com.usmonie.compass.state.Effect,
        VM : com.usmonie.compass.state.StateViewModel<S, A, V, F>
        > StatefulComponent(
    viewModel: VM,
    crossinline onEffect: suspend (F) -> Unit = {},
    crossinline content: @Composable (S, (A) -> Unit) -> Unit
) {
    val component = remember(viewModel) {
        object : ManagedStateComponent<S, A, V, F, VM>(viewModel) {
            override suspend fun onEffect(effect: F) {
                onEffect(effect)
            }

            @Composable
            override fun Content(state: S, onAction: (A) -> Unit) {
                content(state, onAction)
            }
        }
    }

    component.Content()
}
