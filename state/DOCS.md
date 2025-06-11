# 📚 Compass State Management - Documentation

Welcome to the comprehensive documentation for Compass State Management! This guide provides
everything you need to know about implementing clean, type-safe state management in your Kotlin
Multiplatform applications.

## 📖 Documentation Overview

### 🚀 Getting Started

- **[README](README.md)** - Quick overview and installation
- **[Getting Started Guide](GETTING_STARTED.md)** - Step-by-step tutorial building a complete Todo
  app
- **[API Reference](API.md)** - Complete API documentation
- **[Examples & Migration](EXAMPLES.md)** - Real-world examples and migration guides
- **[Changelog](CHANGELOG.md)** - Version history and breaking changes

## 🎯 What is Compass State?

Compass State is a powerful, type-safe state management library that implements the MVI (
Model-View-Intent) architecture pattern. It's designed specifically for Kotlin Multiplatform
projects with seamless Jetpack Compose integration.

### Core Benefits

- **🏗️ Clean Architecture**: Unidirectional data flow with clear separation of concerns
- **🔒 Type Safety**: Compile-time safety prevents runtime errors
- **🌍 Multiplatform**: Works across Android, iOS, Desktop, and Web
- **⚛️ Compose-First**: Built specifically for Jetpack Compose with automatic state observation
- **🧪 Testable**: Pure functions and immutable data make testing straightforward
- **⚡ Performance**: Optimized for minimal recompositions

## 🏛️ Architecture Overview

### MVI Pattern

```
┌─────────────┐    ┌──────────────┐    ┌─────────────┐    ┌─────────────┐
│    User     │───▶│    Action    │───▶│   Process   │───▶│    Event    │
│ Interaction │    │              │    │   Action    │    │             │
└─────────────┘    └──────────────┘    └─────────────┘    └─────────────┘
                                                                   │
┌─────────────┐    ┌──────────────┐    ┌─────────────┐            │
│     UI      │◀───│     State    │◀───│   Reduce    │◀───────────┘
│ Recompose   │    │              │    │    State    │
└─────────────┘    └──────────────┘    └─────────────┘
                           │
                           ▼
                   ┌─────────────┐
                   │   Effect    │
                   │ (Optional)  │
                   └─────────────┘
```

### Key Components

1. **State** - Immutable data representing your UI
2. **Action** - User intents and interactions
3. **Event** - Internal events resulting from action processing
4. **Effect** - One-time side effects (navigation, toasts, etc.)

## 📋 Quick Reference

### Basic Setup

```kotlin
// Add dependency
implementation("com.usmonie.compass:state:0.2.0")

// Define your MVI components
data class MyState(val data: String = "") : State
sealed class MyAction : Action { object Load : MyAction() }
sealed class MyEvent : Event { object Loaded : MyEvent() }
sealed class MyEffect : Effect { object Navigate : MyEffect() }

// Create ViewModel
class MyViewModel : StateViewModel<MyState, MyAction, MyEvent, MyEffect>(
    initialState = MyState()
) {
    override suspend fun processAction(action: MyAction): MyEvent = MyEvent.Loaded
    override fun MyState.reduce(event: MyEvent): MyState = copy(data = "loaded")
    override suspend fun handleEvent(event: MyEvent): MyEffect? = null
}

// Use in Compose
@Composable
fun MyScreen() {
    val viewModel = remember { MyViewModel() }
    StateContent(viewModel) { state, onAction ->
        Text(state.data)
        Button(onClick = { onAction(MyAction.Load) }) {
            Text("Load")
        }
    }
}
```

## 🎓 Learning Path

### 1. Beginner - Getting Started

Start here if you're new to MVI or Compass State:

1. **[README](README.md)** - Overview and installation
2. **[Getting Started Guide](GETTING_STARTED.md)** - Complete tutorial
3. **[Simple Examples](EXAMPLES.md#quick-start-examples)** - Basic patterns

### 2. Intermediate - Core Concepts

Once you understand the basics:

1. **[API Reference - Core Interfaces](API.md#core-interfaces)** - Deep dive into State, Action,
   Event, Effect
2. **[API Reference - ViewModels](API.md#viewmodels)** - StateViewModel vs FlowStateViewModel
3. **[Content State Management](API.md#content-state-management)** - Loading/Success/Error patterns
4. **[Testing Guide](API.md#testing-utilities)** - How to test your MVI components

### 3. Advanced - Real-World Usage

For complex applications:

1. **[Migration Examples](EXAMPLES.md#migration-from-other-patterns)** - Migrate from MVVM/Redux
2. **[Navigation Integration](API.md#navigation-integration)** - Screen-to-screen communication
3. **[DSL Builders](API.md#dsl-builders)** - Declarative screen creation
4. **[Performance Optimization](EXAMPLES.md#performance-optimization-tips)** - Best practices

## 🛠️ Common Use Cases

### Simple State Management

Perfect for basic screens with minimal state:

```kotlin
val counterViewModel = createStateViewModel(
    initialState = CounterState(),
    processAction = { action, _ -> /* convert action to event */ },
    reduce = { event -> /* update state */ },
    handleEvent = { _, _ -> null }
)
```

### Complex Async Operations

Use FlowStateViewModel for operations requiring multiple state updates:

```kotlin
class DataViewModel : FlowStateViewModel<DataState, DataAction, DataEvent, DataEffect>(
    initialState = DataState()
) {
    override suspend fun processAction(action: DataAction): Flow<DataEvent> = flow {
        emit(DataEvent.LoadingStarted)
        try {
            val data = loadData()
            emit(DataEvent.DataLoaded(data))
        } catch (e: Exception) {
            emit(DataEvent.LoadingFailed(e))
        }
    }
}
```

### Screen with Navigation

Handle navigation effects cleanly:

```kotlin
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val navController = LocalNavController.current
    
    StateContent(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                ProfileEffect.NavigateBack -> navController.popBackStack()
                is ProfileEffect.NavigateToUser -> navController.navigate(/* ... */)
            }
        }
    ) { state, onAction ->
        ProfileContent(state, onAction)
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

// In UI
state.users
    .onLoading { CircularProgressIndicator() }
    .onSuccess { users -> UserList(users) }
    .onError { error -> ErrorMessage(error.message) }
```

### DSL for Screens

Create screens with minimal boilerplate:

```kotlin
val userScreen = flowStateScreen<UserState, UserAction, UserEvent, UserEffect>("user") {
    initialState(UserState())
    processAction { action, state -> /* ... */ }
    reduce { event -> /* ... */ }
    content { state, onAction -> UserUI(state, onAction) }
    onEffect { effect -> /* handle effects */ }
}
```

### Testing Support

Test your MVI components easily:

```kotlin
@Test
fun `incrementing should increase count`() = runTest {
    val viewModel = CounterViewModel()
    viewModel.handleAction(CounterAction.Increment)
    assertEquals(1, viewModel.state.value.count)
}
```

## 🎨 Best Practices

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

## 🚨 Common Pitfalls

### ❌ Don't Do This

```kotlin
// Mutable state
data class BadState(val items: MutableList<Item>) : State

// Complex actions
sealed class BadAction : Action {
    data class LoadUserAndPostsAndComments(val userId: String) : BadAction()
}

// Side effects in state reduction
override fun MyState.reduce(event: MyEvent): MyState {
    analyticsService.track("event_occurred") // ❌ Side effect!
    return copy(/* ... */)
}
```

### ✅ Do This Instead

```kotlin
// Immutable state
data class GoodState(val items: List<Item>) : State

// Focused actions
sealed class GoodAction : Action {
    data class LoadUser(val userId: String) : GoodAction()
    data class LoadPosts(val userId: String) : GoodAction()
    data class LoadComments(val postId: String) : GoodAction()
}

// Pure state reduction
override fun MyState.reduce(event: MyEvent): MyState = copy(/* ... */)

// Side effects in event handling
override suspend fun handleEvent(event: MyEvent): MyEffect? = when (event) {
    is MyEvent.UserLoaded -> {
        analyticsService.track("user_loaded") // ✅ Side effect in proper place
        null
    }
}
```

## 🔗 Related Resources

### Official Documentation

- **[Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)** - KMP documentation
- **[Jetpack Compose](https://developer.android.com/jetpack/compose)** - Compose documentation
- **[Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)** - Coroutines guide

### Community Resources

- **[MVI Pattern Guide](https://hannesdorfmann.com/android/model-view-intent/)** - Understanding MVI
- *
  *[State Management Patterns](https://developer.android.com/topic/architecture/ui-layer/stateholders)
  ** - Android Architecture Guide

### Sample Projects

- **Todo App** - Complete example in [Getting Started Guide](GETTING_STARTED.md)
- **Shopping Cart** - Advanced example in [Examples](EXAMPLES.md)
- **Form Validation** - Complex state management in [Examples](EXAMPLES.md)

## 🤝 Contributing

We welcome contributions! Here's how you can help:

1. **Report Issues** - Found a bug? Report it on GitHub
2. **Suggest Features** - Have an idea? Open a feature request
3. **Improve Documentation** - Help make these docs better
4. **Submit Code** - Fix bugs or implement features

### Development Setup

```bash
git clone https://github.com/usmonie/compass.git
cd compass/state
./gradlew build
```

## 📞 Support

Need help? Here are your options:

- **[GitHub Issues](https://github.com/usmonie/compass/issues)** - Bug reports and feature requests
- **[Discussions](https://github.com/usmonie/compass/discussions)** - Questions and community help
- **[API Reference](API.md)** - Complete API documentation
- **[Examples](EXAMPLES.md)** - Real-world usage patterns

## 📈 Roadmap

### Current Version (0.2.0)

- ✅ Core MVI implementation
- ✅ Jetpack Compose integration
- ✅ Navigation support
- ✅ Content state management
- ✅ DSL builders
- ✅ Testing utilities

### Future Versions

- 🔄 Enhanced animation support
- 🔄 Performance profiling tools
- 🔄 Advanced state synchronization
- 🔄 Improved web platform support
- 🔄 AI-powered state management suggestions

---

**Ready to get started?** Begin with the **[Getting Started Guide](GETTING_STARTED.md)** or explore
the **[API Reference](API.md)** for detailed documentation.

**Made with ❤️ by the Compass team**