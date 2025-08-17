# Compass Navigation Migration to androidx.navigation3

This guide outlines the migration from custom navigation implementation to androidx.navigation3.

## Overview

The Compass Navigation library has been migrated to use androidx.navigation3 as its core navigation engine while preserving the state management capabilities and DSL syntax.

## Key Changes

### 1. Core Navigation Types

- `ScreenId` and `GraphId` now extend `androidx.navigation3.runtime.NavKey`
- Both are `@Serializable` for proper state persistence
- Added `toString()` overrides for better debugging

```kotlin
@Serializable
@JvmInline
public value class ScreenId(public val id: String) : NavKey {
    override fun toString(): String = id
}
```

### 2. Navigation Controller

- Replaced custom `NavController` with `Navigation3Controller`
- Uses `androidx.navigation3.runtime.NavBackStack` for state management
- Provides imperative navigation operations:
  - `navigate(screenId)`
  - `pop()`
  - `popTo(screenId, inclusive)`
  - `replace(screenId)`

### 3. Entry Provider System

- Created `StateNavEntryProvider` for integrating state management
- Supports both `StateViewModel` and `FlowStateViewModel`
- Screen registration with metadata support:

```kotlin
entryProvider.registerScreen(
    screenId = ScreenId("home"),
    viewModelFactory = { homeViewModel },
    content = { viewModel -> HomeContent(viewModel) },
    metadata = emptyMap()
)
```

### 4. DSL Updates

The DSL has been updated to work with androidx.navigation3:

```kotlin
SimpleCompassNavHost(
    startDestination = ScreenId("home"),
    onEffect = { effect -> /* handle effects */ }
) {
    screen(
        screenId = ScreenId("home"),
        initialState = HomeState(),
        processAction = { action, state -> /* process action */ },
        handleEvent = { event, state -> /* handle event */ },
        reduce = { event -> /* reduce state */ }
    ) { state, sendAction ->
        HomeScreen(state, sendAction)
    }
}
```

### 5. NavEntryDecorators

Created decorators for state management integration:

- `createStateViewModelDecorator()` - Provides ViewModel to composition
- `createEffectDecorator()` - Handles effects from ViewModels
- `createResultHandlerDecorator()` - Handles navigation results

### 6. Main Navigation Components

- `CompassNavHost` - Main navigation host using NavDisplay
- `SimpleCompassNavHost` - Simplified host for basic use cases
- `Navigation3GraphBuilder` - DSL builder for navigation graphs

## Migration Steps

### For Screen Definitions

**Before:**
```kotlin
val homeScreen = screen("home") { store, params, extra ->
    HomeScreen()
}
```

**After:**
```kotlin
screen(
    screenId = ScreenId("home"),
    initialState = HomeState(),
    processAction = { action, state -> HomeEvent.ActionProcessed },
    handleEvent = { event, state -> null },
    reduce = { event -> this }
) { state, sendAction ->
    HomeScreen(state, sendAction)
}
```

### For Navigation Operations

**Before:**
```kotlin
navController.navigate("details".toScreenId())
navController.popBackStack()
```

**After:**
```kotlin
val backStack = rememberNavBackStack()
backStack.navigate(ScreenId("details"))
backStack.removeAt(backStack.lastIndex)
```

### For Main App

**Before:**
```kotlin
NavigationHost(navController = navController)
```

**After:**
```kotlin
SimpleCompassNavHost(
    startDestination = ScreenId("home")
) {
    // screen definitions
}
```

## Benefits

1. **Improved Performance** - Uses optimized androidx.navigation3 engine
2. **Better State Management** - Seamless integration with Compose state
3. **Enhanced Animation Support** - Leverages Navigation 3's animation system
4. **Predictive Back Support** - Built-in predictive back gesture handling
5. **Multiplatform Ready** - Works across all Compose Multiplatform targets

## Breaking Changes

1. Screen IDs must be explicitly converted to `ScreenId` type
2. Navigation operations now work directly with `NavBackStack`
3. Graph-based navigation simplified to screen-based with metadata
4. Result handling moved to decorators and composition locals
5. Removed custom NavController in favor of Navigation3Controller

## Legacy Support

Old navigation files have been marked as deprecated:
- `NavBackstack.kt` - Use `androidx.navigation3.runtime.NavBackStack`
- Custom scene strategies - Use `androidx.navigation3.ui.SceneStrategy`

## Example App

See `samples/navigation-desktop/src/commonMain/kotlin/App.kt` for a complete example of the migrated navigation system.
