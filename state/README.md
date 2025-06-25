# Compass State - MVI Architecture for Kotlin Multiplatform

[![Maven Central](https://img.shields.io/maven-central/v/com.usmonie.compass/state.svg)](https://search.maven.org/search?q=g:com.usmonie.compass)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-orange.svg)](https://kotlinlang.org/docs/multiplatform.html)

A powerful, type-safe state management library implementing the MVI (Model-View-Intent) architecture
pattern with seamless Jetpack Compose integration.

## 🌟 Key Features

- **🏗️ MVI Architecture** - Clean, predictable unidirectional data flow
- **🔒 Type Safety** - Strongly typed `State`, `Action`, `Event`, and `Effect`
- **⚛️ Reactive** - Built on Kotlin Coroutines and Flow
- **🎯 Multiple ViewModels** - `StateViewModel` and `FlowStateViewModel`
- **📦 Content State** - `ContentState<T>` for loading/success/error patterns
- **🧩 Compose Integration** - `StateContent` and `StatefulComponent`
- **🧪 Testing Support** - Pure functions for easy testing
- **🎨 DSL Support** - Declarative APIs with minimal boilerplate

## 🚀 Quick Start

### Installation

```kotlin
dependencies {
    implementation("com.usmonie.compass:state:0.2.1")
    
    // Optional: For navigation integration
    implementation("com.usmonie.compass:core:0.2.1")
}
```

### Basic Usage

```kotlin
// 1. Define your MVI components
data class CounterState(val count: Int = 0) : State

sealed class CounterAction : Action {
    object Increment : CounterAction()
    object Decrement : CounterAction()
    object Reset : CounterAction()
}

sealed class CounterEvent : Event {
    object Incremented : CounterEvent()
    object Decremented : CounterEvent()
    object Reset : CounterEvent()
}

sealed class CounterEffect : Effect {
    data class ShowToast(val message: String) : CounterEffect()
}

// 2. Create ViewModel
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

// 3. Use in Compose
@Composable
fun CounterScreen() {
    val viewModel = remember { CounterViewModel() }
    
    StateContent(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is CounterEffect.ShowToast -> {
                    // Show toast or snackbar
                }
            }
        }
    ) { state, onAction ->
        Column {
            Text("Count: ${state.count}")
            Row {
                Button(onClick = { onAction(CounterAction.Decrement) }) { Text("-") }
                Button(onClick = { onAction(CounterAction.Increment) }) { Text("+") }
                Button(onClick = { onAction(CounterAction.Reset) }) { Text("Reset") }
            }
        }
    }
}
```

## 🏗️ Architecture Overview

### MVI Flow
```
User Action → ActionProcessor → Event → StateManager → New State
                                 ↓
                           EventHandler → Effect (Optional)
```

### Core Components

1. **State** - Immutable data representing your UI
2. **Action** - User intents and interactions
3. **Event** - Internal events resulting from action processing
4. **Effect** - One-time side effects (navigation, toasts, etc.)

## 📚 Core APIs

### StateViewModel

For single event per action:

```kotlin
class SimpleViewModel : StateViewModel<MyState, MyAction, MyEvent, MyEffect>(
    initialState = MyState()
) {
    override suspend fun processAction(action: MyAction): MyEvent {
        // Convert action to event (business logic)
    }

    override fun MyState.reduce(event: MyEvent): MyState {
        // Pure state reduction
    }

    override suspend fun handleEvent(event: MyEvent): MyEffect? {
        // Optional side effects
    }
}
```

### FlowStateViewModel

For multiple events per action:

```kotlin
class FlowViewModel : FlowStateViewModel<MyState, MyAction, MyEvent, MyEffect>(
    initialState = MyState()
) {
    override suspend fun processAction(action: MyAction): Flow<MyEvent> = flow {
        emit(MyEvent.LoadingStarted)
        try {
            val data = loadData()
            emit(MyEvent.DataLoaded(data))
            emit(MyEvent.LoadingCompleted)
        } catch (e: Exception) {
            emit(MyEvent.LoadingFailed(e.message ?: "Unknown error"))
        }
    }

    override fun MyState.reduce(event: MyEvent): MyState = when (event) {
        MyEvent.LoadingStarted -> copy(isLoading = true)
        is MyEvent.DataLoaded -> copy(data = event.data, isLoading = false)
        is MyEvent.LoadingFailed -> copy(error = event.error, isLoading = false)
        else -> this
    }

    override suspend fun handleEvent(event: MyEvent): MyEffect? = when (event) {
        is MyEvent.LoadingFailed -> MyEffect.ShowError(event.error)
        else -> null
    }
}
```

### ContentState

Elegant loading/success/error state management:

```kotlin
data class UserListState(
    val users: ContentState<List<User>> = ContentState.Loading()
) : State

// In UI
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

## 🎨 UI Integration

### StateContent

Main composable for UI with automatic state management:

```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    StateContent(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is MyEffect.NavigateToProfile -> {
                    // Handle navigation
                }
                is MyEffect.ShowToast -> {
                    // Show toast
                }
            }
        }
    ) { state, onAction ->
        // Your UI content
        MyContent(state = state, onAction = onAction)
    }
}
```

### StatefulComponent

For reusable components:

```kotlin
@Composable
fun CounterWidget(viewModel: CounterViewModel) {
    StatefulComponent(
        viewModel = viewModel,
        onEffect = { effect ->
            // Handle effects
        }
    ) { state, onAction ->
        Row {
            Button(onClick = { onAction(CounterAction.Decrement) }) { Text("-") }
            Text("${state.count}", modifier = Modifier.padding(16.dp))
            Button(onClick = { onAction(CounterAction.Increment) }) { Text("+") }
        }
    }
}
```

## 🛠️ Advanced Features

### Extension Functions

Create ViewModels with minimal boilerplate:

```kotlin
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

### DSL Support

Create reusable components:

```kotlin
val counterComponent = stateComponent<CounterState, CounterAction, CounterEvent, CounterEffect> {
    initialStateProvider { CounterState() }
    
    processAction { action, state ->
        when (action) {
            CounterAction.Increment -> CounterEvent.Incremented
            CounterAction.Decrement -> CounterEvent.Decremented
        }
    }
    
    reduce { event ->
        when (event) {
            CounterEvent.Incremented -> copy(count = count + 1)
            CounterEvent.Decremented -> copy(count = count - 1)
        }
    }
    
    content { state, onAction ->
        CounterUI(state = state, onAction = onAction)
    }
}

// Use the component
@Composable
fun MyScreen() {
    counterComponent.Component()
}
```

## 🧭 Navigation Integration

Integrate with Compass Core navigation:

```kotlin
sealed class UserEffect : Effect {
    data class NavigateToProfile(val userId: String) : UserEffect()
    object NavigateBack : UserEffect()
}

@Composable
fun UserScreen(viewModel: UserViewModel) {
    val navController = LocalNavController.current
    
    StateContent(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is UserEffect.NavigateToProfile -> {
                    navController.navigate(
                        ScreenId("profile"),
                        NavOptions(
                            params = buildParams {
                                putString("userId", effect.userId)
                            }.toStringMap()
                        )
                    )
                }
                UserEffect.NavigateBack -> navController.popBackStack()
            }
        }
    ) { state, onAction ->
        UserContent(state = state, onAction = onAction)
    }
}
```

## 🧪 Testing

Testing is straightforward with pure functions:

```kotlin
@Test
fun `should increment count when increment action is processed`() = runTest {
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
    assertEquals(5, initialState.count) // Original unchanged
}

@Test
fun `should emit effect when count reaches maximum`() = runTest {
    val viewModel = CounterViewModel()
    val effects = mutableListOf<CounterEffect>()
    
    val job = launch {
        viewModel.effect.collect { effects.add(it) }
    }
    
    // Increment to maximum
    repeat(10) {
        viewModel.handleAction(CounterAction.Increment)
    }
    
    assertTrue(effects.any { it is CounterEffect.ShowToast })
    job.cancel()
}
```

## 🎯 Best Practices

### State Design

- Keep states **immutable** and **small**
- Use `@Immutable` or `@Stable` for Compose optimization
- Derive computed properties from basic state
- Avoid nested mutable collections

### Action Design

- Make actions **granular** and **focused**
- Include all necessary data in parameters
- Use **descriptive names** representing user intent
- Avoid actions combining multiple concerns

### Event Design

- Name events with **past tense verbs**
- Include all data needed for state reduction
- Keep events focused on **single state changes**
- Use sealed classes for type safety

### Effect Design

- Use effects for **one-time actions** only
- Don't modify application state with effects
- Keep effect handling in the **UI layer**
- Make effects serializable when possible

## 🌐 Platform Support

| Platform | StateViewModel | FlowStateViewModel | ContentState | DSL Support |
|----------|----------------|--------------------|--------------|-------------|
| Android  | ✅              | ✅                  | ✅            | ✅           |
| iOS      | ✅              | ✅                  | ✅            | ✅           |
| Desktop  | ✅              | ✅                  | ✅            | ✅           |
| Web      | ✅              | ✅                  | ✅            | ✅           |

## 📖 Documentation

- **[API Reference](API.md)** - Complete API documentation
- **[Examples](EXAMPLES.md)** - Real-world examples and migration guides
- **[Getting Started](GETTING_STARTED.md)** - Step-by-step tutorial
- **[Changelog](CHANGELOG.md)** - Version history and breaking changes

## 🔗 Integration

- **[Compass Core](../core/README.md)** - Navigation system integration
- **[Main Documentation](../README.md)** - Complete project overview

---

**Part of the [Compass](../README.md) Kotlin Multiplatform library suite**
