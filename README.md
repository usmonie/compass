# Compass

[![Maven Central](https://img.shields.io/maven-central/v/com.usmonie.compass/state.svg)](https://search.maven.org/search?q=g:com.usmonie.compass)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-orange.svg)](https://kotlinlang.org/docs/multiplatform.html)

A Kotlin Multiplatform library for MVI state management and Jetpack Navigation 3 screen integration.

## Modules

| Artifact | Description |
|---|---|
| `com.usmonie.compass:state` | Core MVI types, `StateViewModel`, `ViewModelStore`, `ContentState` |
| `com.usmonie.compass:component-state` | Composable `StateContent`, reusable component DSL |
| `com.usmonie.compass:screen-state` | Nav3 integration: `ScreenId`, `Navigator`, `CompassSaveableState`, screen DSL |

```kotlin
// build.gradle.kts
implementation("com.usmonie.compass:state:0.5.0")
implementation("com.usmonie.compass:component-state:0.5.0")
implementation("com.usmonie.compass:screen-state:0.5.0")
```

---

## MVI core (`state`)

### Marker interfaces

```kotlin
interface State       // immutable data class
interface Action      // user intent
interface Event       // internal state-change trigger
interface Effect      // one-time side effect
```

### StateViewModel

Implement three abstract members:

```kotlin
class CounterViewModel : StateViewModel<CounterState, CounterAction, CounterEvent, CounterEffect>(
    initialState = CounterState()
) {
    // Pure state transition
    override fun CounterState.reduce(event: CounterEvent): CounterState = when (event) {
        CounterEvent.Incremented -> copy(count = count + 1)
        CounterEvent.Decremented -> copy(count = count - 1)
    }

    // Business logic — suspend, can call APIs, emit multiple events
    override suspend fun processAction(
        action: CounterAction,
        state: CounterState,
        emit: suspend (CounterEvent) -> Unit,
        launchFlow: suspend (SubscriptionKey, suspend CoroutineScope.() -> Unit) -> Unit,
    ) {
        when (action) {
            CounterAction.Increment -> emit(CounterEvent.Incremented)
            CounterAction.Decrement -> emit(CounterEvent.Decremented)
            CounterAction.LoadRemote -> launchFlow(SubscriptionKey("load")) {
                val data = api.fetch()
                emit(CounterEvent.Loaded(data))
            }
        }
    }

    // Optional one-time effects
    override fun handleEvent(event: CounterEvent): CounterEffect? =
        if (event is CounterEvent.Incremented && state.value.count >= 10)
            CounterEffect.ShowMaxReached
        else null
}
```

Key members:

| Member | Type | Purpose |
|---|---|---|
| `state` | `StateFlow<S>` | Current state — observe in UI |
| `effect` | `Flow<F>` | One-time side effects |
| `handleAction(action)` | `Unit` | Entry point for UI events |
| `viewModelScope` | `CoroutineScope` | Coroutine scope, cancelled in `onDispose()` |
| `launchFlow(key, block)` | — | Named cancellable sub-job (replaces previous run on same key) |
| `onDispose()` | `Unit` | Called by `ViewModelStore`; cancels all coroutines |

### Lambdas-based factory

```kotlin
val vm = createStateViewModel(
    initialState = CounterState(),
    processAction = { action, state, emit, launchFlow ->
        when (action) {
            CounterAction.Increment -> emit(CounterEvent.Incremented)
        }
    },
    handleEvent = { event, state -> null },
    reduce = { event ->
        when (event) {
            CounterEvent.Incremented -> copy(count = count + 1)
        }
    },
)
```

### ViewModelStore

Global singleton that keeps one ViewModel instance per navigation key. You rarely touch this directly — `CompassSaveableState` manages it automatically.

```kotlin
// Get or create
val vm = ViewModelStore.getOrPut(screenId) { MyViewModel() }

// Remove and dispose one entry
ViewModelStore.remove(screenId)

// Dispose entries not in the active backstack
ViewModelStore.sync(backStack.toList())

// Dispose everything (useful in tests)
ViewModelStore.clear()
```

### ContentState

Typed loading/success/error wrapper:

```kotlin
data class UserState(
    val user: ContentState<User> = ContentState.Loading()
) : State
```

```kotlin
// In UI
when (val s = state.user) {
    is ContentState.Loading -> CircularProgressIndicator()
    is ContentState.Success -> UserCard(s.data)
    is ContentState.Error  -> ErrorMessage(s.error.message)
}

// Chained extension functions
state.user
    .onLoading { CircularProgressIndicator() }
    .onSuccess { user -> UserCard(user) }
    .onError   { err -> ErrorMessage(err.message) }
```

Available extensions: `onLoading`, `onSuccess`, `onError`, `map`, `flatMap`, `updateData`.

---

## Compose integration (`component-state`)

### StateContent

```kotlin
@Composable
fun CounterScreen(viewModel: CounterViewModel) {
    StateContent(
        viewModel = viewModel,
        onEffect = { state, effect ->
            when (effect) {
                CounterEffect.ShowMaxReached -> showToast("Max reached!")
                null -> Unit
            }
        },
    ) { state, onAction ->
        Column {
            Text("Count: ${state.count}")
            Button(onClick = { onAction(CounterAction.Increment) }) { Text("+") }
        }
    }
}
```

### Reusable component DSL

For components that are defined once and instantiated from multiple places:

```kotlin
val counterComponent = stateComponent<Unit, CounterState, CounterAction, CounterEvent, CounterEffect> {
    initialStateProvider { CounterState() }
    processAction { action, state, emit, _ ->
        when (action) {
            CounterAction.Increment -> emit(CounterEvent.Incremented)
        }
    }
    handleEvent { _, _ -> null }
    reduce { event ->
        when (event) {
            CounterEvent.Incremented -> copy(count = count + 1)
        }
    }
    content { _, state, onAction ->
        CounterContent(state, onAction)
    }
}

// Usage
counterComponent.Component(params = Unit)
```

---

## Navigation integration (`screen-state`)

Compass wraps [Jetpack Navigation 3](https://developer.android.com/guide/navigation/navigation-3) and adds ViewModel lifecycle management, typed navigation keys, and predictive-back animations.

### ScreenId

Every screen needs a unique key. Extend `ScreenId`:

```kotlin
// Singleton screen (no parameters)
@Serializable
object HomeScreenId : ScreenId("home")

// Parameterised screen
@Serializable
data class ProfileScreenId(val userId: String) : ScreenId("profile_$userId")
```

Two instances of `ProfileScreenId("alice")` are equal and share the same ViewModel. Two instances with different `userId` have independent ViewModels.

Navigation modes (set via `ScreenId.mode` or `navigateTo`):

| Mode | Behaviour |
|---|---|
| `STANDARD` | Always push a new entry (default) |
| `SINGLE_TOP` | Reuse if already at the top of the stack |
| `SINGLE_CONTENT_TOP` | Reuse if already present anywhere in the stack |
| `SINGLE_INSTANCE` | One instance total; bring to top if present |

### Navigator

Access via `LocalNavigator.current`:

```kotlin
val navigator = LocalNavigator.current

navigator.navigateTo(ProfileScreenId("alice"))
navigator.navigateTo(AuthScreenId, clearBackStack = true)  // replace entire stack
navigator.navigateTo(DetailScreenId, replace = true)       // replace top entry
navigator.pop()
```

### CompassSaveableState

Wrap your `NavDisplay` with `CompassSaveableState` — it keeps `ViewModelStore` in sync with the backstack. When a screen is popped, its ViewModel is disposed; when re-pushed, it gets a fresh instance.

```kotlin
CompassSaveableState(backStack.toList()) {
    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider { /* ... */ },
        onBack = { navigator.pop() },
    )
}
```

**Tab navigation:** pass the union of all tabs' backstacks so off-screen tabs keep their ViewModels:

```kotlin
val allActive = remember(tab1.toList(), tab2.toList()) { tab1 + tab2 }

CompassSaveableState(allActive) {
    NavDisplay(backStack = activeTabBackStack, ...)
}
```

### Registering screens (`entryProvider`)

**Type-matched registration** — one `stateEntry` covers every `ProfileScreenId` instance, regardless of the `userId` parameter:

```kotlin
entryProvider {
    // Full MVI screen
    stateEntry<ProfileScreenId, ProfileState, ProfileAction, ProfileEvent, ProfileEffect> { id ->
        buildProfileScreen(id)
    }

    // Simple form screen (no custom Action/Event types needed)
    simpleEntry<EditProfileScreenId, EditProfileState> { id ->
        simpleStateScreen(id, EditProfileState()) {
            content { state, sendAction ->
                EditProfileContent(state) { newState ->
                    sendAction(SimpleAction.UpdateState(newState))
                }
            }
        }
    }

    // Composable-only screen (no ViewModel)
    screen<SettingsScreenId> { buildSettingsScreen() }
}
```

**Single-key registration** — when the key is known at build time:

```kotlin
stateEntry(key = HomeScreenId) { buildHomeScreen() }
```

### stateScreen DSL

Build a screen destination with a lambda builder:

```kotlin
fun buildProfileScreen(id: ProfileScreenId) = stateScreen<ProfileScreenId, ProfileState, ProfileAction, ProfileEvent, ProfileEffect>(id) {
    initialState(ProfileState())
    processAction { action, state, emit, _ ->
        when (action) {
            ProfileAction.Load -> emit(ProfileEvent.Loaded(repo.get(id.userId)))
        }
    }
    handleEvent { event, _ ->
        if (event is ProfileEvent.SaveFailed) ProfileEffect.ShowError else null
    }
    reduce { event ->
        when (event) {
            is ProfileEvent.Loaded -> copy(user = event.user)
            ProfileEvent.SaveFailed -> this
        }
    }
    content { state, onAction ->
        ProfileContent(state, onAction)
    }
}
```

For simple form-like screens use `simpleStateScreen` — it wires up `SimpleAction.UpdateState` automatically so you only define `content`:

```kotlin
fun buildEditScreen(id: EditProfileScreenId) = simpleStateScreen(id, EditProfileState()) {
    content { state, sendAction ->
        EditProfileContent(state) { sendAction(SimpleAction.UpdateState(it)) }
    }
}
```

### Passing results between screens

```kotlin
// Provide in your nav host
CompositionLocalProvider(LocalResultStore provides rememberResultStore()) {
    NavDisplay(...)
}

// Set result from destination screen
val store = LocalResultStore.current
store.setResult<SelectedPhoto>(result = photo)
navigator.pop()

// Consume in calling screen
val photo = LocalResultStore.current.getResultState<SelectedPhoto>()
```

### Predictive-back animation

```kotlin
NavDisplay(
    predictivePopTransitionSpec = predictivePopSlideTransitionSpec(),
    ...
)
```

---

## Testing

`ViewModelStore` is global state — clear it before and after each test:

```kotlin
@Before fun setUp()    { ViewModelStore.clear() }
@After  fun tearDown() { ViewModelStore.clear() }
```

Unit-test pure state transitions without Compose:

```kotlin
@Test
fun increment_updatesCount() = runTest {
    val vm = CounterViewModel()
    vm.handleAction(CounterAction.Increment)
    assertEquals(1, vm.state.value.count)
}

@Test
fun reduce_isPure() {
    val state = CounterState(count = 5)
    val next = with(CounterViewModel()) { state.reduce(CounterEvent.Incremented) }
    assertEquals(6, next.count)
    assertEquals(5, state.count) // original unchanged
}
```

Instrumented navigation tests use `CompassSaveableState` directly:

```kotlin
@Test
fun popAndReNavigate_showsFreshState() = runAndroidComposeUiTest<ComponentActivity> {
    val backStack = mutableStateListOf<ScreenId>(LoginScreenId)
    setContent {
        MaterialTheme {
            CompassSaveableState(backStack.toList()) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = entryProvider { /* ... */ },
                )
            }
        }
    }

    onNodeWithTag("email_field").performTextReplacement("test@example.com")
    runOnUiThread { backStack.removeLastOrNull() }
    waitForIdle()
    runOnUiThread { backStack.add(LoginScreenId) }
    waitForIdle()

    // Fresh ViewModel — field is empty
    onNodeWithTag("email_field").assert(
        SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
    )
}
```

---

## Platform support

| Platform | Supported |
|---|---|
| Android | ✅ |
| iOS | ✅ |
| Desktop (JVM) | ✅ |
| Web (Wasm/JS) | ✅ |
