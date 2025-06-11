# Compass - Kotlin Multiplatform Navigation & State Management

[![Maven Central](https://img.shields.io/maven-central/v/com.usmonie.compass/core.svg)](https://search.maven.org/search?q=g:com.usmonie.compass)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-orange.svg)](https://kotlinlang.org/docs/multiplatform.html)

> **This file is deprecated. Please see the main [README.md](../README.md) for current
documentation.**

A modern, type-safe navigation and state management solution for Kotlin Multiplatform projects.

## 📁 Project Structure

```
compass/
├── core/                   # Navigation library
│   ├── src/
│   │   ├── commonMain/    # Common navigation code
│   │   ├── androidMain/   # Android-specific implementation
│   │   ├── iosMain/       # iOS-specific implementation
│   │   └── commonTest/    # Navigation tests
│   └── README.md          # Core documentation
├── state/                  # State management (MVI) library
│   ├── src/
│   │   ├── commonMain/    # Common MVI code
│   │   ├── androidMain/   # Android-specific state management
│   │   ├── iosMain/       # iOS-specific state management
│   │   └── commonTest/    # State management tests
│   ├── README.md          # State documentation
│   ├── API.md             # Complete API reference
│   ├── EXAMPLES.md        # Real-world examples
│   └── GETTING_STARTED.md # Step-by-step tutorial
└── sample/                 # Example applications
    └── src/
        └── commonMain/
```

## 🚀 Quick Navigation

- **[📘 Main Documentation](../README.md)** - Complete project overview
- **[🧭 Navigation Guide](core/README.md)** - Compass Core documentation
- **[🏗️ State Management Guide](state/README.md)** - Compass State documentation
- **[🔧 API Reference](state/API.md)** - Detailed API documentation
- **[📖 Examples](state/EXAMPLES.md)** - Real-world usage examples
- **[🎯 Getting Started](state/GETTING_STARTED.md)** - Step-by-step tutorial

## 🏛️ Architecture Overview

### 🧭 Navigation (Compass Core)
```
NavController → NavigationEngine → ScreenDestination
     ↑                                      ↓
TypedParams ←── DeepLinkHandler ←── NavigationResult
```

**Key Components:**

- `NavController` - Main navigation controller
- `ScreenDestination` - Base class for screens
- `TypedParams` - Type-safe parameters
- `DeepLinkHandler` - Deep link processing
- `NavigationResult` - Result communication

### 🏗️ State Management (Compass State)
```
User Action → ActionProcessor → Event → StateManager → New State
                                 ↓
                           EventHandler → Effect (Optional)
```

**Key Components:**

- `State` - Immutable application state
- `Action` - User interactions
- `Event` - Internal state change events
- `Effect` - One-time side effects
- `StateViewModel` / `FlowStateViewModel` - State management

## 💻 Code Examples

### Navigation
```kotlin
// Navigate with type-safe parameters
navController.navigate(
    ScreenId("profile"),
    NavOptions(
        params = buildParams {
            putString("userId", "123")
            putInt("tab", 2)
        }.toStringMap()
    )
)
```

### State Management
```kotlin
class CounterViewModel : StateViewModel<CounterState, CounterAction, CounterEvent, CounterEffect>(
    initialState = CounterState()
) {
    override suspend fun processAction(action: CounterAction): CounterEvent = when (action) {
        CounterAction.Increment -> CounterEvent.Incremented
        CounterAction.Decrement -> CounterEvent.Decremented
    }

    override fun CounterState.reduce(event: CounterEvent): CounterState = when (event) {
        CounterEvent.Incremented -> copy(count = count + 1)
        CounterEvent.Decremented -> copy(count = count - 1)
    }

    override suspend fun handleEvent(event: CounterEvent): CounterEffect? = when (event) {
        CounterEvent.Incremented -> {
            if (state.value.count >= 10) CounterEffect.ShowToast("Max reached!") else null
        }
        else -> null
    }
}
```

## 🎯 Platform Support

| Platform | Navigation | State Management | Web Support |
|----------|------------|------------------|-------------|
| Android  | ✅          | ✅                | N/A         |
| iOS      | ✅          | ✅                | N/A         |
| Desktop  | ✅          | ✅                | N/A         |
| Web      | ✅          | ✅                | ✅           |

## 📦 Installation

```kotlin
dependencies {
    // Navigation
    implementation("com.usmonie.compass:core:0.2.0")
    
    // State Management (MVI)
    implementation("com.usmonie.compass:state:0.2.0")
}
```

---

**For complete and up-to-date documentation, please visit the [main README](../README.md).**
