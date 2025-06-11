# Compass State Management

[![Maven Central](https://img.shields.io/maven-central/v/com.usmonie.compass/state.svg)](https://search.maven.org/search?q=g:com.usmonie.compass)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-orange.svg)](https://kotlinlang.org/docs/multiplatform.html)

A powerful, type-safe state management library for Kotlin Multiplatform projects, implementing the
MVI (Model-View-Intent) architecture pattern with seamless Jetpack Compose integration and built-in
navigation support.

## 🚀 Why Compass State?

- **🏗️ MVI Architecture**: Clean, predictable unidirectional data flow
- **🔒 Type Safety**: Compile-time safety for actions, states, events, and effects
- **🌍 Multiplatform**: Works across Android, iOS, Desktop, and Web
- **⚛️ Compose-First**: Built specifically for Jetpack Compose with automatic state observation
- **🧭 Navigation**: Integrated navigation system with state preservation
- **⚡ Performance**: Optimized for minimal recompositions and efficient memory usage
- **🧪 Testable**: Pure functions and immutable data make testing straightforward
- **📖 DSL Support**: Declarative APIs with minimal boilerplate

## 📦 Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    implementation("com.usmonie.compass:state:0.2.0")
    
    // Optional: For navigation integration
    implementation("com.usmonie.compass:core:0.2.0")
}
```

### Gradle (Groovy)

```groovy
dependencies {
    implementation 'com.usmonie.compass:state:0.2.0'
    
    // Optional: For navigation integration
    implementation 'com.usmonie.compass:core:0.2.0'
}
```

## 🏃‍♂️ Quick Start

### 1. Define Your MVI Components

```kotlin
// State - What your UI looks like
data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false
) : State

// Actions - What users can do
sealed class CounterAction : Action {
    object Increment : CounterAction()
    object Decrement : CounterAction()
    object Reset : CounterAction()
}

// Events - What happened internally
sealed class CounterEvent : Event {
    object Incremented : CounterEvent()
    object Decremented : CounterEvent()
    object Reset : CounterEvent()
}

// Effects - One-time side effects (optional)
sealed class CounterEffect : Effect {
    data class ShowToast(val message: String) : CounterEffect()
    object NavigateToSettings : CounterEffect()
}
```

### 2. Create a ViewModel

```kotlin
class CounterViewModel : StateViewModel<CounterState, CounterAction, CounterEvent, CounterEffect>(
    initialState = CounterState()
) {
    override suspend fun processAction(action: CounterAction): CounterEvent = when (action) {
        CounterAction.Increment -> CounterEvent.Incremented
        CounterAction.Decrement -> CounterEvent.Decremented
        CounterAction.Reset -> CounterEvent.Reset
    }

    override fun CounterState.reduce(event: CounterEvent): CounterState = when (event) {
        CounterEvent.Incremented -> copy(count = count + 1)
        CounterEvent.Decremented -> copy(count = count - 1)
        CounterEvent.Reset -> copy(count = 0)
    }

    override suspend fun handleEvent(event: CounterEvent): CounterEffect? = when (event) {
        CounterEvent.Incremented -> {
            if (state.value.count >= 10) CounterEffect.ShowToast("Maximum reached!") else null
        }
        else -> null
    }
}
```

### 3. Create Your Compose UI

```kotlin
@Composable
fun CounterScreen(viewModel: CounterViewModel = remember { CounterViewModel() }) {
    StateContent(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is CounterEffect.ShowToast -> {
                    // Show toast or snackbar
                    println("Toast: ${effect.message}")
                }
                CounterEffect.NavigateToSettings -> {
                    // Handle navigation
                }
            }
        }
    ) { state, onAction ->
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Count: ${state.count}",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row {
                Button(onClick = { onAction(CounterAction.Decrement) }) {
                    Text("-")
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Button(onClick = { onAction(CounterAction.Increment) }) {
                    Text("+")
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Button(onClick = { onAction(CounterAction.Reset) }) {
                    Text("Reset")
                }
            }
        }
    }
}
```

## 🎯 Key Concepts

### MVI Flow

```
User Action → Process Action → Event → Reduce State → New State
                                ↓
                            Handle Event → Effect (Optional)
```

1. **User triggers an Action** (button click, text input, etc.)
2. **Action is processed** into an Event (business logic, API calls)
3. **Event reduces State** (pure function, no side effects)
4. **New State updates UI** (automatic recomposition)
5. **Effects are handled** (navigation, toasts, etc.)

### State Management Patterns

#### Simple State Updates

```kotlin
// For straightforward state changes
class SimpleViewModel : StateViewModel<SimpleState, SimpleAction, SimpleEvent, Nothing>(
    initialState = SimpleState()
) {
    override suspend fun processAction(action: SimpleAction): SimpleEvent = when (action) {
        is SimpleAction.UpdateText -> SimpleEvent.TextUpdated(action.text)
    }

    override fun SimpleState.reduce(event: SimpleEvent): SimpleState = when (event) {
        is SimpleEvent.TextUpdated -> copy(text = event.text)
    }

    override suspend fun handleEvent(event: SimpleEvent): Nothing? = null
}
```

#### Complex Async Operations

```kotlin
// For operations that need multiple state updates
class DataViewModel : FlowStateViewModel<DataState, DataAction, DataEvent, DataEffect>(
    initialState = DataState()
) {
    override suspend fun processAction(action: DataAction): Flow<DataEvent> = flow {
        when (action) {
            DataAction.LoadData -> {
                emit(DataEvent.LoadingStarted)
                try {
                    val data = repository.loadData()
                    emit(DataEvent.DataLoaded(data))
                    emit(DataEvent.LoadingCompleted)
                } catch (e: Exception) {
                    emit(DataEvent.LoadingFailed(e.message ?: "Unknown error"))
                }
            }
        }
    }
    
    override fun DataState.reduce(event: DataEvent): DataState = when (event) {
        DataEvent.LoadingStarted -> copy(isLoading = true, error = null)
        is DataEvent.DataLoaded -> copy(data = event.data)
        DataEvent.LoadingCompleted -> copy(isLoading = false)
        is DataEvent.LoadingFailed -> copy(isLoading = false, error = event.error)
    }

    override suspend fun handleEvent(event: DataEvent): DataEffect? = when (event) {
        is DataEvent.LoadingFailed -> DataEffect.ShowError(event.error)
        else -> null
    }
}
```

## 🔧 Advanced Features

### Content State Management

Handle loading/success/error states elegantly:

```kotlin
data class UserListState(
    val users: ContentState<List<User>> = ContentState.Loading()
) : State

// In your UI
when (val usersState = state.users) {
    is ContentState.Loading -> CircularProgressIndicator()
    is ContentState.Success -> LazyColumn {
        items(usersState.data) { user ->
            UserItem(user = user)
        }
    }
    is ContentState.Error -> ErrorMessage(
        error = usersState.error.message,
        onRetry = { onAction(UserAction.LoadUsers) }
    )
}

// Or use extension functions
state.users
    .onLoading { CircularProgressIndicator() }
    .onSuccess { users -> UserList(users = users) }
    .onError { error -> ErrorMessage(error = error.message) }
```

### Screen DSL

Create screens with minimal boilerplate:

```kotlin
val counterScreen = flowStateScreen<CounterState, CounterAction, CounterEvent, CounterEffect>(
    id = "counter"
) {
    initialState(CounterState())
    
    processAction { action, state ->
        flowOf(when (action) {
            CounterAction.Increment -> CounterEvent.Incremented
            CounterAction.Decrement -> CounterEvent.Decremented
        })
    }
    
    reduce { event ->
        when (event) {
            CounterEvent.Incremented -> copy(count = count + 1)
            CounterEvent.Decremented -> copy(count = count - 1)
        }
    }
    
    handleEvent { event, state -> null }
    
    content { state, onAction ->
        CounterContent(state = state, onAction = onAction)
    }
    
    onEffect { effect ->
        when (effect) {
            is CounterEffect.ShowToast -> showToast(effect.message)
        }
    }
}
```

### Extension Functions for Less Boilerplate

```kotlin
// Create ViewModels with lambda functions
val viewModel = createStateViewModel(
    initialState = CounterState(),
    processAction = { action, state ->
        when (action) {
            CounterAction.Increment -> CounterEvent.Incremented
            CounterAction.Decrement -> CounterEvent.Decremented
        }
    },
    handleEvent = { event, state -> null },
    reduce = { event ->
        when (event) {
            CounterEvent.Incremented -> copy(count = count + 1)
            CounterEvent.Decremented -> copy(count = count - 1)
        }
    }
)
```

## 🧭 Navigation Integration

Compass State integrates seamlessly with navigation:

```kotlin
@Composable
fun CounterScreen(viewModel: CounterViewModel) {
    val navController = LocalNavController.current
    
    StateContent(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                CounterEffect.NavigateToSettings -> {
                    navController.navigate(ScreenId("settings"))
                }
                is CounterEffect.ShowToast -> {
                    // Show toast
                }
            }
        }
    ) { state, onAction ->
        // Your UI content
        CounterContent(
            state = state,
            onAction = onAction,
            onNavigateToSettings = { onAction(CounterAction.NavigateToSettings) }
        )
    }
}
```

## 🧪 Testing

Testing is straightforward with pure functions and immutable data:

```kotlin
@Test
fun `incrementing should increase count`() = runTest {
    val viewModel = CounterViewModel()
    
    viewModel.handleAction(CounterAction.Increment)
    
    assertEquals(1, viewModel.state.value.count)
}

@Test
fun `state reduction should be pure`() {
    val initialState = CounterState(count = 5)
    val event = CounterEvent.Incremented
    
    val newState = with(CounterViewModel()) {
        initialState.reduce(event)
    }
    
    assertEquals(6, newState.count)
    // Original state unchanged
    assertEquals(5, initialState.count)
}
```

## 📚 Documentation

- **[API Documentation](API.md)** - Complete API reference
- **[Examples & Migration Guide](EXAMPLES.md)** - Real-world examples and migration from other
  libraries
- **[Changelog](CHANGELOG.md)** - Version history and breaking changes

## 🤝 Contributing

We welcome contributions! Please see our contributing guidelines for details.

### Development Setup

```bash
git clone https://github.com/usmonie/compass.git
cd compass/state
./gradlew build
```

## 📄 License

```
Copyright 2024 Compass

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## 🔗 Related Projects

- **[Compass Navigation](../core/)** - Type-safe navigation for Kotlin Multiplatform
- **[Compass Core](../core/)** - Core utilities and navigation system

---

**Made with ❤️ by the Compass team**