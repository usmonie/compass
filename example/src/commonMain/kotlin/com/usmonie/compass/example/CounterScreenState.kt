package com.usmonie.compass.example

import com.usmonie.compass.state.Action
import com.usmonie.compass.state.Effect
import com.usmonie.compass.state.Event
import com.usmonie.compass.state.State

internal data class CounterState(
    val count: Int = 0,
    val text: String = ""
) : State

internal sealed class CounterAction : Action {
    object Increment : CounterAction()
    data class UpdateText(val text: String) : CounterAction()
    object GoToProfile : CounterAction()
}

internal sealed class CounterEvent : Event {
    object Incremented : CounterEvent()
    data class TextUpdated(val text: String) : CounterEvent()
}

internal sealed class CounterEffect : Effect {
    object NavigateToProfile : CounterEffect()
}
