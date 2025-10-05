package com.usmonie.compass.example// state/ProfileState.kt

import User
import com.usmonie.compass.state.ContentState
import com.usmonie.compass.state.State

internal data class ProfileScreenState(
    val user: ContentState<User> = ContentState.Loading(),
) : State