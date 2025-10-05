package com.usmonie.compass.example// navigation/AppScreens.kt
import User
import com.usmonie.compass.core.navigation.ScreenId

internal object LoginScreen : ScreenId("login")
internal data class ProfileScreen(val user: User) : ScreenId("profile")