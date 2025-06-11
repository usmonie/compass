package com.usmonie.compass.state

/**
 * Base interface for any element that manages state using the MVI pattern
 */
public interface StateElement<
        S : State,
        A : Action,
        V : Event,
        F : Effect,
        VM : StateViewModel<S, A, V, F>
        > {
    /**
     * The ViewModel that manages the state of this element
     */
    public val viewModel: VM

    /**
     * Called when the element is cleared/disposed
     */
    public fun onCleared() {
        viewModel.onDispose()
    }
}