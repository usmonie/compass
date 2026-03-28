package com.usmonie.compass.example

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import com.usmonie.compass.component.state.stateComponent
import com.usmonie.compass.state.Action
import com.usmonie.compass.state.Effect
import com.usmonie.compass.state.Event
import com.usmonie.compass.state.State

internal data class UserCardState(val user: User) : State
internal object UserCardAction : Action
internal object UserCardEvent : Event
internal data class UserCardEffect(val userId: String) : Effect

internal val UserCardComponent = stateComponent<Unit, UserCardState, UserCardAction, UserCardEvent, UserCardEffect> {
    initialStateProvider { UserCardState(User("1", "Loading...")) }
    processAction { _, _, _, _ ->  }
    handleEvent { _, _ -> null }
    reduce { this }
    content { p, state, _ ->
        BasicText("Hello, ${state.user.name}!")
    }
}
