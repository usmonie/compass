package com.usmonie.compass.example// ui/components/UserCardComponent.kt

import User
import androidx.compose.foundation.text.BasicText
import com.usmonie.compass.component.state.stateComponent
import com.usmonie.compass.state.*

internal data class UserCardState(val user: User) : State
internal object UserCardAction : Action
internal object UserCardEvent : Event
internal data class UserCardEffect(val userId: String) : Effect

internal val UserCardComponent = stateComponent<UserCardState, UserCardAction, UserCardEvent, UserCardEffect> {
    initialStateProvider { UserCardState(User("1", "Loading...")) }
    processAction { _, _, _, _ ->  }
    handleEvent { _, _ -> null }
    reduce { this }
    content { state, _ ->
        BasicText("Hello, ${state.user.name}!")
    }
}