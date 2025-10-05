package com.usmonie.compass.screen.state

import com.usmonie.compass.state.Action
import com.usmonie.compass.state.Effect
import com.usmonie.compass.state.Event
import com.usmonie.compass.state.State

public sealed class SimpleAction<S : State> :
    Action {
    public data class UpdateState<S : State>(val newState: S) :
        SimpleAction<S>()
}

public sealed class SimpleEvent<S : State> :
    Event {
    public data class StateUpdated<S : State>(
        val newState: S,
    ) : SimpleEvent<S>()
}

public object SimpleEffect : Effect

