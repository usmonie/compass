package com.usmonie.compass.example

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Badge
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
// ...
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
// ...
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.usmonie.compass.component.state.onLoading
import com.usmonie.compass.component.state.onSuccess
import com.usmonie.compass.screen.state.CompassSaveableState
import com.usmonie.compass.screen.state.SimpleAction
import com.usmonie.compass.screen.state.SimpleEffect
import com.usmonie.compass.screen.state.SimpleEvent
import com.usmonie.compass.screen.state.StateScreenDestination
import com.usmonie.compass.screen.state.navigation.ScreenId
import com.usmonie.compass.screen.state.simpleEntry
import com.usmonie.compass.screen.state.simpleStateScreen
import com.usmonie.compass.screen.state.stateEntry
import com.usmonie.compass.state.ContentState

@Composable
internal fun ExampleApp() {
    val backStack = remember { mutableStateListOf<ScreenId>(CounterScreen) }

    val entryProvider: (ScreenId) -> NavEntry<ScreenId> = entryProvider {
        stateEntry<CounterScreen, CounterState, CounterAction, CounterEvent, CounterEffect> {
            buildCounterScreen {
                backStack.add(LoginScreen)
            }
        }

        simpleEntry<ProfileScreen, ProfileScreenState> {
            buildProfileScreen(it.user)
        }

        stateEntry<LoginScreen, LoginState, LoginAction, LoginEvent, LoginEffect>(
            screenDestination = {
                buildLoginScreen(
                    onBack = { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) },
                    onLoginSuccess = { user ->
                        backStack.add(ProfileScreen(user))
                    }
                )
            }
        )
    }

    MaterialTheme {
        CompassSaveableState(backStack.toList()) {
            BackHandler(enabled = backStack.size > 1) {
                backStack.removeAt(backStack.size - 1)
            }
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
                entryProvider = entryProvider
            )
        }
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
                    Scaffold { paddingValues ->
                        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
                            Text("Welcome, ${user.name}!", style = MaterialTheme.typography.headlineMedium)
                            UserCardComponent.Component(Unit)
                        }
                    }
                }
        }
    }
}
