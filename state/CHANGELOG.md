# Changelog

All notable changes to the Compass State Management library will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Full integration with Compass Navigation System
- `StateScreen` class for screens with built-in MVI state management
- Navigation effects handling in `StateViewModel` and `FlowStateViewModel`
- Type-safe navigation parameters with `TypedParams`
- Navigation result passing with `NavigationResult` interface
- Deep link support with `DeepLinkHandler`
- Automatic state preservation across navigation
- Multi-graph navigation support
- Gesture-based navigation support
- Web navigation integration capabilities
- Navigation DSL for declarative screen creation
- `LocalNavController` composition local for accessing navigation
- Comprehensive navigation extensions and utilities

### Enhanced

- `StateViewModel` now supports navigation effects
- `FlowStateViewModel` enhanced with navigation integration
- `StatefulComponent` works seamlessly with navigation
- Content state management integrated with navigation flows
- Error handling extended to navigation scenarios

### Breaking Changes

- Navigation effects now require integration with `NavController`
- Screen lifecycle methods now include navigation-aware callbacks
- Some existing APIs updated to support navigation parameters

## [0.2.0] - 2024-01-XX

### Added

- Initial release of Compass State Management library
- Core MVI architecture implementation
- `StateViewModel` for single event per action
- `FlowStateViewModel` for multiple events per action
- `State`, `Action`, `Event`, and `Effect` base interfaces
- `ContentState` for loading/success/error state management
- `StatefulComponent` for automatic state observation
- `StateComponent` base class for reusable components
- Extension functions for easier ViewModel creation
- Comprehensive DSL support
- Type-safe state management
- Jetpack Compose integration
- Kotlin Multiplatform support
- Coroutines-first approach
- Built-in error handling
- State preservation capabilities
- Testing utilities and examples

### Features

- **MVI Architecture**: Complete Model-View-Intent implementation
- **Type Safety**: Strongly typed actions, states, events, and effects
- **Reactive**: Built on Kotlin Coroutines and Flow
- **Compose Integration**: Seamless integration with Jetpack Compose
- **Multiplatform**: Works across all Kotlin targets
- **Testing**: Easy to test with pure functions and data classes
- **Performance**: Optimized for minimal recompositions
- **Error Handling**: Comprehensive error management
- **Documentation**: Extensive documentation and examples

## Integration Features

### Navigation System Integration

#### Core Navigation Components

- **NavController**: Central navigation controller with state management integration
- **NavigationHost**: Main composable for hosting the entire navigation system
- **ScreenDestination**: Base class for screens with automatic state preservation
- **StateScreen**: Specialized screen with built-in MVI state management
- **NavigationEngine**: Internal engine powering all navigation operations

#### Type-Safe Navigation

- **TypedParams**: Type-safe parameter passing between screens
- **NavigationResult**: Type-safe result passing between screens
- **ScreenId** and **GraphId**: Strong typing for screen and graph identification
- Parameter validation and automatic type conversion
- Compile-time safety for navigation operations

#### Advanced Navigation Features

- **Multi-Graph Support**: Navigate between different navigation graphs
- **Deep Link Integration**: Built-in deep link parsing and handling
- **State Preservation**: Automatic state saving and restoration
- **Gesture Navigation**: Swipe-to-go-back gesture support
- **Web Integration**: Browser history integration for web platforms
- **Result Handling**: Navigate for results with type-safe callbacks

#### Navigation DSL

- Declarative navigation graph creation
- Simplified screen registration
- Factory pattern for screen creation
- Easy integration with state management

### State Management Enhancements

#### Navigation-Aware ViewModels

```kotlin
// Effects can now trigger navigation
sealed class UserEffect : Effect {
    data class NavigateToProfile(val userId: String) : UserEffect()
    object NavigateBack : UserEffect()
}

class UserViewModel : StateViewModel<UserState, UserAction, UserEvent, UserEffect>(...) {
    override suspend fun handleEvent(event: UserEvent): UserEffect? = when (event) {
        is UserEvent.ProfileRequested -> UserEffect.NavigateToProfile(event.userId)
        else -> null
    }
}
```

#### Integrated Screen Components

```kotlin
class UserScreen : StateScreen<UserState, UserAction, UserEvent, UserEffect, UserViewModel>(
    id = ScreenId("user"),
    viewModel = UserViewModel()
) {
    @Composable
    override fun Content() {
        val navController = LocalNavController.current
        
        StatefulComponent(
            viewModel = viewModel,
            onEffect = { effect ->
                when (effect) {
                    is UserEffect.NavigateToProfile -> {
                        navController.navigate(
                            screenId = ScreenId("profile"),
                            params = buildParams {
                                putString("userId", effect.userId)
                            }
                        )
                    }
                    UserEffect.NavigateBack -> navController.popBackStack()
                }
            }
        ) { state, onAction ->
            UserUI(state = state, onAction = onAction)
        }
    }
}
```

### Comprehensive Example

```kotlin
@Composable
fun MyApp() {
    val navController = navController(initialGraphId = "main") {
        graph("main") {
            rootScreen("home") { _, _, _ ->
                HomeScreen()
            }
            screen("user_detail") { _, params, _ ->
                val userId = TypedParams.fromStringMap(params).get<String>("userId") ?: ""
                UserDetailScreen(userId)
            }
        }
        
        graph("auth") {
            rootScreen("login") { _, _, _ ->
                LoginScreen()
            }
        }
    }
    
    NavigationHost(
        navController = navController,
        gestureEnabled = true
    )
}
```

## Architecture Benefits

### Unified State and Navigation Management

- Single source of truth for both state and navigation
- Consistent patterns across the entire application
- Reduced boilerplate code
- Improved testability

### Type Safety

- Compile-time validation of navigation operations
- Strong typing for parameters and results
- Prevention of runtime navigation errors
- Better IDE support and refactoring

### Performance Optimizations

- Automatic state preservation and restoration
- Efficient memory management
- Minimal recompositions
- Lazy loading of screens and resources

### Developer Experience

- Declarative APIs
- Comprehensive documentation
- Rich debugging information
- Extensive examples and migration guides

## Migration Path

### From Version 0.1.x

1. Add navigation dependency: `implementation("com.usmonie.compass:core:$compass_version")`
2. Update effects to handle navigation: Add navigation effects to your Effect sealed classes
3. Integrate with NavController: Use `LocalNavController.current` in your screens
4. Update screen creation: Consider using `StateScreen` for automatic integration
5. Add navigation handling: Handle navigation effects in your `onEffect` callbacks

### From Other Libraries

- See the comprehensive migration guide in EXAMPLES.md
- Gradual migration is supported
- Existing ViewModels can be easily adapted
- Navigation can be integrated incrementally

## Breaking Changes

### Navigation Integration

- Effects now commonly include navigation operations
- Screen lifecycle methods include navigation callbacks
- Some APIs updated to support navigation parameters
- `StateScreen` constructor requires `ScreenId`

### Recommended Upgrades

- Use `StateScreen` instead of plain `ScreenDestination` for better integration
- Leverage `TypedParams` instead of string-based parameters
- Implement navigation effects for better user experience
- Use `NavigationResult` for type-safe result passing

## Future Roadmap

### Planned Features

- Enhanced animation support
- Better testing utilities
- Performance profiling tools
- Advanced state synchronization
- Improved web platform support
- AI-powered state management suggestions

### Community Contributions

- Bug reports and feature requests welcome
- Contribution guidelines available
- Community plugins supported
- Documentation improvements encouraged

---

For detailed usage examples and migration guides, see:

- [README.md](README.md) - Getting started guide
- [API.md](API.md) - Complete API documentation
- [EXAMPLES.md](EXAMPLES.md) - Real-world examples and migration guides