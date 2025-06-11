package com.usmonie.compass.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

/**
 * Base class for Composable components that manage state using the MVI pattern
 */
public abstract class StateComponent<
        S : State,
        A : Action,
        V : Event,
        F : Effect,
        VM : StateViewModel<S, A, V, F>
        >(override val viewModel: VM) : StateElement<S, A, V, F, VM> {

    /**
     * Composable function that renders the component with automatic lifecycle management
     */
    @Composable
    public fun Content() {
        DisposableEffect(viewModel) {
            onDispose {
                onCleared()
            }
        }

        Render()
    }

    /**
     * Abstract method to render the component's UI
     */
    @Composable
    protected abstract fun Render()
}

/**
 * Extension function to create a StateComponent with automatic lifecycle management
 */
@Composable
public inline fun <
        S : State,
        A : Action,
        V : Event,
        F : Effect,
        VM : StateViewModel<S, A, V, F>
        > StateComponent(
    viewModel: VM,
    crossinline content: @Composable (VM) -> Unit
) {
    val component = remember(viewModel) {
        object : StateComponent<S, A, V, F, VM>(viewModel) {
            @Composable
            override fun Render() {
                content(viewModel)
            }
        }
    }

    component.Content()
}