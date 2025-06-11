package com.usmonie.compass.mvi

import com.usmonie.compass.core.navigation.ScreenDestination
import com.usmonie.compass.core.navigation.ScreenId
import com.usmonie.compass.state.Action
import com.usmonie.compass.state.Effect
import com.usmonie.compass.state.Event
import com.usmonie.compass.state.State
import com.usmonie.compass.state.StateElement
import com.usmonie.compass.state.StateViewModel

public abstract class StateScreen<
        S : State,
        A : Action,
        V : Event,
        F : Effect,
        VM : StateViewModel<S, A, V, F>
        >(
    id: ScreenId,
    override val viewModel: VM,
    storeInBackStack: Boolean = true
) : ScreenDestination(id, storeInBackStack), StateElement<S, A, V, F, VM> {

    override fun onCleared() {
        viewModel.onDispose()
    }
}