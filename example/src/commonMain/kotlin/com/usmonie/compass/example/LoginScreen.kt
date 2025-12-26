package com.usmonie.compass.example

import User
import com.usmonie.compass.screen.state.navigation.ScreenId

internal object LoginScreen : ScreenId("login")
internal data class ProfileScreen(val user: User) : ScreenId("profile")