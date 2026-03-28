package com.usmonie.compass.example

import com.usmonie.compass.screen.state.navigation.ScreenId
import kotlinx.serialization.Serializable

@Serializable
internal object LoginScreen : ScreenId("login")
@Serializable
internal data class ProfileScreen(val user: User) : ScreenId("profile")

@Serializable
internal object CounterScreen : ScreenId("counter")

