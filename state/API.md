# Compass State Management - API Reference

Complete API documentation for the Compass State Management library. This reference covers all
public APIs, interfaces, and extension functions.

## 📚 Table of Contents

1. [Core Interfaces](#core-interfaces)
2. [ViewModels](#viewmodels)
3. [UI Components](#ui-components)
4. [Content State Management](#content-state-management)
5. [Navigation Integration](#navigation-integration)
6. [Extension Functions](#extension-functions)
7. [DSL Builders](#dsl-builders)
8. [Testing Utilities](#testing-utilities)

---

## 🔧 Core Interfaces

### State

```kotlin
@Stable
interface State
```

Base interface for all application states in the MVI architecture.

**Guidelines:**

- Must be immutable data classes
- Should be marked with `@Stable` or `@Immutable` for Compose optimization
- Contains all data needed to render the UI

**Example:**

```kotlin
@Immutable
data class UserProfileState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false
) : State
```

### Action

```kotlin
interface Action
```

Base interface for user actions that represent user intents and interactions.

**Guidelines:**

- Should be sealed classes or data classes
- Named with action verbs (Load, Save, Update, Delete, etc.)
- Contain all parameters needed for processing

**Example:**

```kotlin
sealed class UserProfileAction : Action {
    object LoadProfile : UserProfileAction()
    object StartEditing : UserProfileAction()
    object CancelEditing : UserProfileAction()
    data class UpdateName(val name: String) : UserProfileAction()
    data class UpdateEmail(val email: String) : UserProfileAction()
    object SaveProfile : UserProfileAction()
    object DeleteProfile : UserProfileAction()
}
```

### Event

```kotlin
interface Event
```

Base interface for internal events that represent what happened as a result of actions.

**Guidelines:**

- Should be sealed classes or data classes
- Named with past tense verbs (Loaded, Updated, Failed, etc.)
- Contain data needed for state reduction

**Example:**

```kotlin
sealed class UserProfileEvent : Event {
    object LoadingStarted : UserProfileEvent()
    data class ProfileLoaded(val user: User) : UserProfileEvent()
    data class LoadingFailed(val error: Throwable) : UserProfileEvent()
    object EditingStarted : UserProfileEvent()
    object EditingCancelled : UserProfileEvent()
    data class NameUpdated(val name: String) : UserProfileEvent()
    data class EmailUpdated(val email: String) : UserProfileEvent()
    object ProfileSaved : UserProfileEvent()
    data class SaveFailed(val error: Throwable) : UserProfileEvent()
}
```

### Effect

```kotlin
interface Effect
```

Base interface for one-time side effects that don't directly modify state.

**Guidelines:**

- Should be sealed classes or data classes
- Represent actions that happen outside the state (navigation, toasts, etc.)
- Should not modify application state directly

**Example:**

```kotlin
sealed class UserProfileEffect : Effect {
    object NavigateBack : UserProfileEffect()
    data class ShowToast(val message: String) : UserProfileEffect()
    data class ShowConfirmDialog(val message: String) : UserProfileEffect()
    object OpenImagePicker : UserProfileEffect()
    data class ShareProfile(val user: User) : UserProfileEffect()
}
```

### ErrorState

```kotlin
@Stable
abstract class ErrorState(error: Throwable) : State {
    val message: String = error.message ?: "Unknown error"
    val throwable: Throwable = error
}
```

Base class for representing error conditions in a consistent way.

**Example:**

```kotlin
class NetworkErrorState(error: Throwable) : ErrorState(error)
class ValidationErrorState(error: Throwable) : ErrorState(error)
class AuthenticationErrorState(error: Throwable) : ErrorState(error)

// Usage in state
data class LoginState(
    val loginError: ErrorState? = null,
    val isLoading: Boolean = false
) : State
```

---

## 🧠 ViewModels

### StateViewModel

```kotlin
abstract class StateViewModel<S : State, in A : Action, V : Event, out F : Effect>(
    initialState: S,
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : ViewModel
```

Core ViewModel implementing MVI pattern for single event per action.

**Properties:**

```kotlin
val state: StateFlow<S>  // Current state observable
val effect: Flow<F>      // Side effects stream
```

**Abstract Methods:**

```kotlin
// Convert action to event (business logic)
abstract suspend fun processAction(action: A): V

// Reduce state based on event (pure function)
abstract fun S.reduce(event: V): S

// Handle event and produce optional effect
abstract suspend fun handleEvent(event: V): F?
```

**Public Methods:**

```kotlin
// Main entry point for user actions
fun handleAction(action: A)

// Lifecycle management
override fun onDispose()
```

**Complete Example:**

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
            if (state.value.count >= 10) CounterEffect.ShowMaxReached else null
        }
        else -> null
    }
}
```

### FlowStateViewModel

```kotlin
abstract class FlowStateViewModel<S : State, A_IN : Action, V_EVENT : Event, F_EFFECT : Effect>(
    initialState: S
) : ViewModel
```

Advanced ViewModel supporting multiple events per action via Flow.

**Properties:**

```kotlin
val state: StateFlow<S>      // Current state observable
val effect: Flow<F_EFFECT>   // Side effects stream
```

**Abstract Methods:**

```kotlin
// Process action into flow of events (allows multiple state updates)
abstract suspend fun processAction(action: A_IN): Flow<V_EVENT>

// Reduce state based on event
abstract fun S.reduce(event: V_EVENT): S

// Handle event and produce optional effect
abstract suspend fun handleEvent(event: V_EVENT): F_EFFECT?
```

**Example:**

```kotlin
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

---

## 🎨 UI Components

### StateContent

```kotlin
@Composable
inline fun <S : State, A : Action, V : Event, F : Effect> StateContent(
    viewModel: StateViewModel<S, A, V, F>,
    noinline onEffect: suspend (F) -> Unit = {},
    crossinline content: @Composable (S, (A) -> Unit) -> Unit
)
```

Main composable for creating UI with automatic state management.

**Parameters:**

- `viewModel` - The ViewModel managing state
- `onEffect` - Handler for side effects
- `content` - UI content lambda receiving state and action handler

**Example:**

```kotlin
@Composable
fun CounterScreen(viewModel: CounterViewModel) {
    StateContent(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is CounterEffect.ShowToast -> {
                    // Show toast or snackbar
                }
                CounterEffect.NavigateToSettings -> {
                    // Handle navigation
                }
            }
        }
    ) { state, onAction ->
        Column {
            Text("Count: ${state.count}")
            Button(onClick = { onAction(CounterAction.Increment) }) {
                Text("Increment")
            }
        }
    }
}
```

### StatefulComponent

```kotlin
@Composable
inline fun <S : State, A : Action, V : Event, F : Effect, VM : StateViewModel<S, A, V, F>> 
StatefulComponent(
    viewModel: VM,
    crossinline onEffect: suspend (F) -> Unit = {},
    crossinline content: @Composable (S, (A) -> Unit) -> Unit
)
```

Alternative composable function for creating UI with automatic state management.

**Example:**

```kotlin
@Composable
fun CounterWidget(viewModel: CounterViewModel) {
    StatefulComponent(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is CounterEffect.ShowMaxReached -> {
                    // Handle max reached effect
                }
            }
        }
    ) { state, onAction ->
        Row {
            Button(onClick = { onAction(CounterAction.Decrement) }) {
                Text("-")
            }
            Text("${state.count}", modifier = Modifier.padding(16.dp))
            Button(onClick = { onAction(CounterAction.Increment) }) {
                Text("+")
            }
        }
    }
}
```

---

## 📦 Content State Management

### ContentState<T>

```kotlin
sealed class ContentState<T>(val item: T?) {
    data class Success<T>(val data: T) : ContentState<T>(data)
    data class Error<T, E : ErrorState>(val error: E) : ContentState<T>(null)
    class Loading<T> : ContentState<T>(null)
}
```

Type-safe representation of loading/success/error states.

**Usage Patterns:**

```kotlin
// In your state
data class ProductListState(
    val products: ContentState<List<Product>> = ContentState.Loading(),
    val categories: ContentState<List<Category>> = ContentState.Loading()
) : State

// In your UI - Pattern matching
when (val productsState = state.products) {
    is ContentState.Loading -> CircularProgressIndicator()
    is ContentState.Success -> ProductGrid(products = productsState.data)
    is ContentState.Error -> ErrorMessage(error = productsState.error.message)
}

// In your UI - Extension functions
state.products
    .onLoading { CircularProgressIndicator() }
    .onSuccess { products -> ProductGrid(products = products) }
    .onError { error -> ErrorMessage(error = error.message) }
```

### ContentState Extensions

```kotlin
// Transform data within ContentState
inline fun <T> ContentState<T>.updateData(onSuccess: (T) -> T): ContentState<T>

// Chain operations
fun <T> ContentState<T>.onSuccess(action: (T) -> Unit): ContentState<T>
fun <T, E : ErrorState> ContentState<T>.onError(action: (E) -> Unit): ContentState<T>
fun <T> ContentState<T>.onLoading(action: () -> Unit): ContentState<T>

// Transform ContentState type
inline fun <T, R> ContentState<T>.map(transform: (T) -> R): ContentState<R>
inline fun <T, R> ContentState<T>.flatMap(transform: (T) -> ContentState<R>): ContentState<R>
```

**Examples:**

```kotlin
// Update data
val updatedProducts = state.products.updateData { products ->
    products.filter { it.isAvailable }
}

// Chain operations (non-Compose)
state.products
    .onLoading { showProgressBar() }
    .onSuccess { products -> hideProgressBar() }
    .onError { error -> showErrorDialog(error.message) }

// Transform type
val productNames: ContentState<List<String>> = state.products.map { products ->
    products.map { it.name }
}

// Chain ContentState operations
val discountedProducts = state.products.flatMap { products ->
    ContentState.Success(products.filter { it.discount > 0 })
}
```

---

## 🧭 Navigation Integration

### LocalNavController

```kotlin
@Composable
val LocalNavController.current: NavController
```

Composition local for accessing the current navigation controller.

**Example:**

```kotlin
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val navController = LocalNavController.current
    
    StateContent(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                ProfileEffect.NavigateToSettings -> {
                    navController.navigate(ScreenId("settings"))
                }
                ProfileEffect.NavigateBack -> {
                    navController.popBackStack()
                }
            }
        }
    ) { state, onAction ->
        ProfileContent(state = state, onAction = onAction)
    }
}
```

---

## 🔧 Extension Functions

### ViewModel Creation

```kotlin
// Create StateViewModel with lambdas
inline fun <S : State, A : Action, V : Event, F : Effect> createStateViewModel(
    initialState: S,
    crossinline processAction: suspend CoroutineScope.(A, S) -> V,
    crossinline handleEvent: (V, S) -> F?,
    crossinline reduce: S.(V) -> S,
    noinline init: suspend StateViewModel<S, A, V, F>.() -> Unit = {}
): StateViewModel<S, A, V, F>

// Create FlowStateViewModel with lambdas
inline fun <S : State, A : Action, V : Event, F : Effect> createFlowViewModel(
    initialState: S,
    crossinline processAction: suspend CoroutineScope.(A, S) -> Flow<V>,
    crossinline handleEvent: (V, S) -> F?,
    crossinline reduce: S.(V) -> S,
    crossinline init: suspend FlowStateViewModel<S, A, V, F>.() -> Unit = {}
): FlowStateViewModel<S, A, V, F>
```

**Examples:**

```kotlin
// Simple ViewModel with lambdas
val counterViewModel = createStateViewModel(
    initialState = CounterState(),
    processAction = { action, state ->
        when (action) {
            CounterAction.Increment -> CounterEvent.Incremented
            CounterAction.Decrement -> CounterEvent.Decremented
            CounterAction.Reset -> CounterEvent.Reset
        }
    },
    handleEvent = { event, state ->
        when (event) {
            CounterEvent.Incremented -> {
                if (state.count >= 10) CounterEffect.ShowMaxReached else null
            }
            else -> null
        }
    },
    reduce = { event ->
        when (event) {
            CounterEvent.Incremented -> copy(count = count + 1)
            CounterEvent.Decremented -> copy(count = count - 1)
            CounterEvent.Reset -> copy(count = 0)
        }
    }
)
```

### State Observation

```kotlin
// Observe state in Compose
@Composable
fun <S : State, A : Action, V : Event, F : Effect> StateViewModel<S, A, V, F>.observeState(): S

@Composable
fun <S : State, A : Action, V : Event, F : Effect> FlowStateViewModel<S, A, V, F>.observeState(): S

// Observe effects in Compose
@Composable
fun <S : State, A : Action, V : Event, F : Effect> FlowStateViewModel<S, A, V, F>.ObserveEffects(
    onEffect: suspend (F) -> Unit
)
```

---

## 🏗️ DSL Builders

### State Screen DSL

```kotlin
inline fun <S : State, A : Action, V : Event, F : Effect> stateScreen(
    id: String,
    storeInBackStack: Boolean = true,
    builder: StateScreenBuilder<S, A, V, F>.() -> Unit
): StateScreenDestination<S, A, V, F>

inline fun <S : State, A : Action, V : Event, F : Effect> flowStateScreen(
    id: String,
    storeInBackStack: Boolean = true,
    builder: FlowStateScreenBuilder<S, A, V, F>.() -> Unit
): FlowStateScreenDestination<S, A, V, F>
```

**Example:**

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

---

## 🧪 Testing Utilities

### ViewModel Testing

```kotlin
// Test state changes
@Test
fun `action should update state correctly`() = runTest {
    val viewModel = CounterViewModel()
    
    viewModel.handleAction(CounterAction.Increment)
    
    assertEquals(1, viewModel.state.value.count)
}

// Test effects
@Test
fun `action should emit correct effect`() = runTest {
    val viewModel = CounterViewModel()
    val effects = mutableListOf<CounterEffect>()
    
    val job = launch {
        viewModel.effect.collect { effects.add(it) }
    }
    
    viewModel.handleAction(CounterAction.Increment)
    
    assertTrue(effects.isEmpty()) // No effect for normal increment
    job.cancel()
}
```

### State Reduction Testing

```kotlin
@Test
fun `state reduction should be pure`() {
    val initialState = CounterState(count = 5)
    val event = CounterEvent.Incremented
    
    val newState = with(CounterViewModel()) {
        initialState.reduce(event)
    }
    
    assertEquals(6, newState.count)
    // Verify original state is unchanged
    assertEquals(5, initialState.count)
}
```

---

## 📋 Best Practices

### State Design

- Keep states immutable and small
- Use `@Immutable` or `@Stable` annotations for Compose optimization
- Derive computed properties from basic state properties
- Avoid nested mutable collections

### Action Design

- Make actions granular and focused
- Include all necessary data in action parameters
- Use descriptive names that represent user intent
- Avoid actions that combine multiple concerns

### Event Design

- Name events with past tense verbs
- Include all data needed for state reduction
- Keep events focused on single state changes
- Use sealed classes for type safety

### Effect Design

- Use effects for one-time actions only
- Don't use effects to modify application state
- Make effects serializable when possible
- Keep effect handling logic in UI layer

### Testing

- Test state reduction functions as pure functions
- Test action processing logic separately
- Use TestCoroutineScheduler for time-based testing
- Mock external dependencies in ViewModels

This comprehensive API reference covers all the essential components of the Compass State Management
library. For complete examples and migration guides, see the [Examples](EXAMPLES.md) documentation.