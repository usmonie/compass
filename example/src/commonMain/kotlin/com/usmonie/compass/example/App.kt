package com.usmonie.compass.example

import User
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.TransformOrigin
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.scene.rememberSceneSetupNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.usmonie.compass.component.state.onLoading
import com.usmonie.compass.component.state.onSuccess
import com.usmonie.compass.core.navigation.ScreenId
import com.usmonie.compass.screen.state.SimpleAction
import com.usmonie.compass.screen.state.SimpleEffect
import com.usmonie.compass.screen.state.SimpleEvent
import com.usmonie.compass.screen.state.StateScreenDestination
import com.usmonie.compass.screen.state.simpleEntry
import com.usmonie.compass.screen.state.simpleStateScreen
import com.usmonie.compass.screen.state.stateEntry
import com.usmonie.compass.state.ContentState

@Composable
internal fun App() {
    var backStack: List<ScreenId> by remember { mutableStateOf(listOf(LoginScreen)) }

    val entryProvider = entryProvider {
        stateEntry(
            LoginScreen,
            {
                buildLoginScreen {
                    backStack = backStack.toMutableList().apply { add(ProfileScreen(it)) }
                }
            }
        )

        simpleEntry<ProfileScreen, ProfileScreenState>(
            screenDestination = {
                buildProfileScreen(it.user)
            },
        )
    }

    MaterialTheme {
        NavDisplay(
            backStack = backStack,
            transitionSpec = { // Define custom transitions for screen changes
                fadeIn(tween(300)) togetherWith fadeOut(tween(300))
            },
            popTransitionSpec = {
                fadeIn(tween(300)) togetherWith scaleOut(
                    targetScale = 0.9f,
                    transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.5f)
                )
            },
            entryDecorators = listOf(
                // Add the default decorators for managing scenes and saving state
                rememberSceneSetupNavEntryDecorator(),
                rememberSavedStateNavEntryDecorator(),
            ),
            onBack = { count ->
                repeat(count) {
                    if (backStack.size > 1) backStack = backStack.dropLast(1)
                }
            },
            entryProvider = entryProvider
        )
    }
}

private fun buildProfileScreen(user: User): StateScreenDestination<ProfileScreen, ProfileScreenState, SimpleAction<ProfileScreenState>, SimpleEvent<ProfileScreenState>, SimpleEffect> {
    return simpleStateScreen(
        ProfileScreen(user),
        initialState = ProfileScreenState(ContentState.Success(user)),
        storeInBackStack = true,
    ) {

        content { state, _ ->
            state.user
                .onLoading { CircularProgressIndicator() }
                .onSuccess { user ->
                    Scaffold {
                        Column {
                            Text("Welcome, ${user.name}!")
                            UserCardComponent.Component()
                        }
                    }
                }
        }
    }
}
