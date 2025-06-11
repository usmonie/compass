# Compass Core - Type-Safe Navigation for Kotlin Multiplatform

[![Maven Central](https://img.shields.io/maven-central/v/com.usmonie.compass/core.svg)](https://search.maven.org/search?q=g:com.usmonie.compass)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-orange.svg)](https://kotlinlang.org/docs/multiplatform.html)

Modern, type-safe navigation library for Kotlin Multiplatform projects with seamless Jetpack Compose
integration.

## 🌟 Key Features

- **🔒 Type Safety** - Compile-time safety with `ScreenId`, `GraphId`, and `TypedParams`
- **🔗 Deep Linking** - Pattern matching with `DeepLinkHandler` and `DeepLinkPattern`
- **📊 Multi-Graph Navigation** - Organize complex flows with nested navigation graphs
- **👆 Gesture Support** - Swipe-to-go-back with `NavigationGesture` and `GestureHandler`
- **💾 State Preservation** - Automatic state saving/restoration via `StateRegistry`
- **↩️ Result Passing** - Type-safe communication with `NavigationResult`
- **🎬 Animation Support** - Custom transitions per screen with `ScreenDestination`
- **🌐 Web Integration** - Browser history support with `WebNavigationSupport`

## 🚀 Quick Start

### Installation

```kotlin
dependencies {
    implementation("com.usmonie.compass:core:0.2.0")
}
```

### Basic Setup

```kotlin
@Composable
fun MyApp() {
    val navController = navController(initialGraphId = GraphId("main")) {
        graph("main") {
            rootScreen("home") { _, _, _ ->
                HomeScreen()
            }
            screen("profile") { _, params, _ ->
                val typedParams = TypedParams.fromStringMap(params)
                val userId = typedParams.get<String>("userId") ?: ""
                ProfileScreen(userId)
            }
            screen("settings") { _, _, _ ->
                SettingsScreen()
            }
        }
    }
    
    NavigationHost(
        navController = navController,
        gestureEnabled = true
    )
}
```

### Navigation Operations

```kotlin
// Basic navigation
navController.navigate(ScreenId("profile"))

// Navigation with parameters
navController.navigate(
    ScreenId("profile"),
    NavOptions(
        params = buildParams {
            putString("userId", "123")
            putInt("tab", 2)
        }.toStringMap()
    )
)

// Navigation with replace
navController.navigate(
    ScreenId("login"),
    NavOptions(replace = true)
)

// Navigate to different graph
navController.navigateToGraph(GraphId("auth"))

// Navigate for result
navController.navigateForResult<ProfileResult>(
    ScreenId("editProfile")
) { result ->
    // Handle result
    when (result) {
        is ProfileResult.Updated -> updateUI(result.profile)
        is ProfileResult.Cancelled -> showMessage("Cancelled")
    }
}

// Navigate back
navController.popBackStack()

// Pop until specific screen
navController.popUntil(ScreenId("home"))

// Pop with result
navController.popWithResult(ProfileResult.Updated(newProfile))
```

## 🏗️ Core Components

### ScreenDestination

Base class for all navigation destinations:

```kotlin
class ProfileScreen(
    private val userId: String
) : ScreenDestination(
    id = ScreenId("profile"),
    storeInBackStack = true
) {
    @Composable
    override fun Content() {
        ProfileContent(userId = userId)
    }
    
    override fun onEnter() {
        // Called when screen enters
    }
    
    override fun onExit() {
        // Called when screen exits
    }
    
    override suspend fun onResult(result: NavigationResult) {
        // Handle navigation results
    }
}
```

### TypedParams

Type-safe parameter passing:

```kotlin
// Building parameters
val params = buildParams {
    putString("userId", "123")
    putInt("tab", 2)
    putBoolean("isEdit", true)
    putFloat("scale", 1.5f)
}

// Reading parameters
val userId = params.get<String>("userId") // "123"
val tab = params.get<Int>("tab") // 2
val isEdit = params.get<Boolean>("isEdit") // true

// Converting to/from string maps
val stringMap = params.toStringMap()
val fromStringMap = TypedParams.fromStringMap(stringMap)
```

### Deep Linking

Pattern-based deep link handling:

```kotlin
val deepLinkHandler = DeepLinkHandler(baseUrl = "https://myapp.com")

// Register patterns
deepLinkHandler.registerScreen(
    pattern = "user/{userId}/profile",
    screenId = ScreenId("profile")
) { match ->
    buildParams {
        putString("userId", match.getParameter("userId") ?: "")
    }
}

// Parse deep links
val result = deepLinkHandler.parseDeepLink("https://myapp.com/user/123/profile")
when (result) {
    is DeepLinkResult.Screen -> {
        navController.navigate(result.screenId, NavOptions(params = result.params.toStringMap()))
    }
    is DeepLinkResult.Graph -> {
        navController.navigateToGraph(result.graphId, NavOptions(params = result.params.toStringMap()))
    }
    null -> { /* Handle invalid link */ }
}
```

### NavigationResult

Type-safe result passing:

```kotlin
// Define results
data class ProfileResult(
    override val id: ScreenId,
    val profile: Profile? = null,
    val action: Action
) : NavigationResult {
    enum class Action { UPDATED, DELETED, CANCELLED }
}

// Return result
navController.popWithResult(
    ProfileResult(
        id = ScreenId("profile"),
        profile = updatedProfile,
        action = ProfileResult.Action.UPDATED
    )
)
```

### Web Integration

```kotlin
@Composable
fun WebApp() {
    val navController = navController(initialGraphId = GraphId("main")) { /* ... */ }
    val webSupport = remember { createWebNavigationSupport() }
    val webIntegration = remember { 
        NavigationWebIntegration(navController, deepLinkHandler, webSupport) 
    }
    
    LaunchedEffect(Unit) {
        webIntegration.initialize()
        webIntegration.bindToBrowserHistory()
    }
    
    NavigationHost(navController = navController)
}
```

## 🎬 Animation Support

Custom screen transitions:

```kotlin
class AnimatedScreen : ScreenDestination(id = ScreenId("animated")) {
    override val enterTransition: AnimatedContentTransitionScope<ScreenDestination>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300)
        )
    }
    
    override val exitTransition: AnimatedContentTransitionScope<ScreenDestination>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(300)
        )
    }
    
    override val popEnterTransition: AnimatedContentTransitionScope<ScreenDestination>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(300)
        )
    }
    
    override val popExitTransition: AnimatedContentTransitionScope<ScreenDestination>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(300)
        )
    }
}
```

## 👆 Gesture Navigation

Handle back gestures:

```kotlin
class GestureScreen : ScreenDestination(id = ScreenId("gesture")) {
    override val gestureHandler: GestureHandler = GestureHandler.Handling(edgePadding = 20f)
    
    @Composable
    override fun Content() {
        // Your content here
    }
}

// In NavigationHost
NavigationHost(
    navController = navController,
    gestureEnabled = true // Enable gesture navigation
)
```

## 🌐 Platform Support

| Feature | Android | iOS | Desktop | Web |
|---------|---------|-----|---------|-----|
| Basic Navigation | ✅ | ✅ | ✅ | ✅ |
| Gesture Navigation | ✅ | ✅ | ❌ | ❌ |
| Deep Links | ✅ | ✅ | ✅ | ✅ |
| State Preservation | ✅ | ✅ | ✅ | ✅ |
| Browser History | N/A | N/A | N/A | ✅ |
| System Back Button | ✅ | ❌ | ❌ | ❌ |

## 🧪 Testing

```kotlin
class NavigationTest {
    @Test
    fun `should navigate to profile screen`() = runTest {
        val navController = NavController.create(GraphId("test")) {
            graph("test") {
                rootScreen("home") { _, _, _ -> HomeScreen() }
                screen("profile") { _, params, _ -> 
                    val userId = TypedParams.fromStringMap(params).get<String>("userId") ?: ""
                    ProfileScreen(userId) 
                }
            }
        }
        
        // Test navigation
        val success = navController.navigate(
            ScreenId("profile"),
            NavOptions(params = buildParams { putString("userId", "123") }.toStringMap())
        )
        
        assertTrue(success)
        assertEquals(ScreenId("profile"), navController.currentDestination.value?.id)
    }
}
```

## 📖 API Reference

### Core Classes

- **`NavController`** - Main navigation controller
- **`ScreenDestination`** - Base class for screens
- **`ScreenId`** - Type-safe screen identifier
- **`GraphId`** - Type-safe graph identifier
- **`TypedParams`** - Type-safe parameter container
- **`NavigationResult`** - Result communication interface
- **`DeepLinkHandler`** - Deep link processing
- **`NavigationHost`** - Main UI component

### Builder Functions

- **`navController()`** - Create navigation controller
- **`buildParams()`** - Build typed parameters
- **`createWebNavigationSupport()`** - Web platform support

### Options & Configuration

- **`NavOptions`** - Navigation configuration options
- **`GestureHandler`** - Gesture handling configuration
- **`NavigationAnimationState`** - Animation state enum

## 🔗 Integration

- **[Compass State](../state/README.md)** - Integrate with MVI state management
- **[Sample App](../sample/README.md)** - Complete example application

---

**Part of the [Compass](../README.md) Kotlin Multiplatform library suite**