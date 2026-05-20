# compass:state

Core MVI types and ViewModel infrastructure for Compass.

For the full library documentation see the [root README](../README.md).

```kotlin
implementation("com.usmonie.compass:state:0.5.0")
```

## What's in this module

- `State`, `Action`, `Event`, `Effect` — marker interfaces
- `ErrorState`, `ThrowableErrorState` — typed error wrappers
- `ContentState<T>` — `Loading` / `Success` / `Error` sealed class with chainable extensions
- `ViewModel` — lifecycle interface (`onDispose()`)
- `StateViewModel<S, A, V, F>` — abstract MVI base class
- `ViewModelStore` — global singleton that owns `ViewModel` instances
- `SubscriptionKey` — key for named cancellable coroutine flows inside a ViewModel
- `createStateViewModel(...)` — lambda-based factory (no subclass required)
- `ActionProcessor`, `EventHandler`, `StateManager` — functional interfaces for dependency injection
