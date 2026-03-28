package com.usmonie.compass.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.usmonie.compass.screen.state.StateScreenDestination
import com.usmonie.compass.screen.state.stateScreen
import com.usmonie.compass.state.SubscriptionKey

internal fun buildLoginScreen(
    onBack: () -> Unit,
    onLoginSuccess: (User) -> Unit
): StateScreenDestination<LoginScreen, LoginState, LoginAction, LoginEvent, LoginEffect> {
    return stateScreen(
        LoginScreen,
        storeInBackStack = true
    ) {
        initialState(LoginState())
        processAction { action, state, emit, launchFlow ->
            when (action) {
                is LoginAction.EnterEmail -> emit(LoginEvent.EmailChanged(action.email))
                is LoginAction.EnterPassword -> emit(LoginEvent.PasswordChanged(action.password))
                LoginAction.Submit -> {
                    if (state.email.isEmpty() || state.password.isEmpty()) {
                        emit(LoginEvent.LoginFailed(IllegalArgumentException("Fill all fields")))
                    } else {
                        emit(LoginEvent.LoadingStarted)
                        launchFlow(SubscriptionKey("login")) {
                            try {
                                val user = ApiClient.fetchUser(state.email)
                                emit(LoginEvent.LoginSuccess(user))
                            } catch (e: Exception) {
                                emit(LoginEvent.LoginFailed(e))
                            }
                        }
                    }
                }
            }
        }
        handleEvent { event, _ ->
            when (event) {
                is LoginEvent.LoginSuccess -> LoginEffect.NavigateToProfile(event.user)
                is LoginEvent.LoginFailed -> LoginEffect.ShowToast(
                    event.error.message ?: "Login failed"
                )

                else -> null
            }
        }
        reduce { event ->
            when (event) {
                is LoginEvent.EmailChanged -> copy(email = event.email)
                is LoginEvent.PasswordChanged -> copy(password = event.password)
                is LoginEvent.LoadingStarted -> copy(isLoading = true, error = null)
                is LoginEvent.LoginSuccess -> copy(isLoading = false, error = null)
                is LoginEvent.LoginFailed -> copy(isLoading = false, error = event.error.message)
            }
        }
        content { state, sendAction ->
            Scaffold { paddingValues ->
                Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { sendAction(LoginAction.EnterEmail(it)) },
                        label = { Text("Email") }
                    )
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { sendAction(LoginAction.EnterPassword(it)) },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    if (state.error != null) {
                        Text(text = state.error, color = colorScheme.error)
                    }
                    Button(
                        onClick = { sendAction(LoginAction.Submit) },
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) CircularProgressIndicator(Modifier.size(16.dp))
                        else Text("Login")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { onBack() },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Back (Manual)")
                    }
                }
            }
        }
        onEffect { state, effect ->
            when (effect) {
                is LoginEffect.NavigateToProfile -> onLoginSuccess(effect.user)
                is LoginEffect.ShowToast -> println("Toast: ${effect.message}") // Replace with real toast
                null -> Unit
            }
        }
    }
}
