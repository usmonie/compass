package com.usmonie.compass.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.usmonie.compass.screen.state.StateScreenDestination
import com.usmonie.compass.screen.state.stateScreen

internal fun buildCounterScreen(onNavigate: () -> Unit): StateScreenDestination<CounterScreen, CounterState, CounterAction, CounterEvent, CounterEffect> {
    return stateScreen(
        CounterScreen,
        storeInBackStack = true
    ) {
        initialState(CounterState())

        processAction { action, _, emit, _ ->
            when (action) {
                CounterAction.Increment -> emit(CounterEvent.Incremented)
                is CounterAction.UpdateText -> emit(CounterEvent.TextUpdated(action.text))
                CounterAction.GoToProfile -> Unit // Handled in onEffect
            }
        }

        handleEvent { event, _ ->
            when (event) {
                is CounterEvent.Incremented -> null
                is CounterEvent.TextUpdated -> null
            }
        }

        reduce { event ->
            when (event) {
                CounterEvent.Incremented -> copy(count = count + 1)
                is CounterEvent.TextUpdated -> copy(text = event.text)
            }
        }

        onEffect { _, effect ->
            when (effect) {
                CounterEffect.NavigateToProfile -> onNavigate()
                null -> Unit
            }
        }

        content { state, sendAction ->
            Scaffold { padding ->
                Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
                    Text("This screen tests state preservation.", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Counter (ViewModel State): ${state.count}")
                    Button(onClick = { sendAction(CounterAction.Increment) }) {
                        Text("Increment")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = state.text,
                        onValueChange = { sendAction(CounterAction.UpdateText(it)) },
                        label = { Text("Typed Text (UI State)") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Scroll position below also persists:")
                    LazyColumn(modifier = Modifier.height(100.dp)) {
                        items((1..50).toList()) {
                            Text("Item #$it", modifier = Modifier.padding(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(onClick = { onNavigate() }) {
                        Text("Go to Next Screen")
                    }
                }
            }
        }
    }
}
