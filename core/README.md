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
    implementation("com.usmonie.compass:core:0.2.1")
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

### Extras - Additional Navigation Data

Extras provide a way to pass complex objects and additional context between screens that don't fit
into simple string-based parameters. Unlike TypedParams, extras can contain any type of data and are
not serialized.

#### Creating Custom Extras

```kotlin
// Define custom extra types
data class UserContext(
    val currentUser: User,
    val permissions: Set<Permission>,
    val sessionId: String
) : Extra

data class NavigationMetadata(
    val sourceScreen: String,
    val timestamp: Long,
    val analyticsData: Map<String, Any>
) : Extra

// Enum extras
enum class ScreenMode : Extra {
    VIEW_ONLY, EDIT_MODE, ADMIN_MODE
}

// Simple data extras
data class ThemeExtra(val isDarkMode: Boolean) : Extra
```

#### Passing Extras During Navigation

```kotlin
// Single extra
val userContext = UserContext(
    currentUser = getCurrentUser(),
    permissions = getUserPermissions(),
    sessionId = getSessionId()
)

navController.navigate(
    screenId = ScreenId("profile"),
    options = NavOptions(
        extras = userContext,
        params = buildParams {
            putString("userId", "123")
        }.toStringMap()
    )
)

// Multiple extras using a wrapper
data class MultipleExtras(
    val userContext: UserContext,
    val mode: ScreenMode,
    val metadata: NavigationMetadata
) : Extra

val combinedExtras = MultipleExtras(
    userContext = userContext,
    mode = ScreenMode.EDIT_MODE,
    metadata = NavigationMetadata(
        sourceScreen = "dashboard",
        timestamp = System.currentTimeMillis(),
        analyticsData = mapOf("feature" to "profile_edit")
    )
)

navController.navigate(
    screenId = ScreenId("profile"),
    options = NavOptions(extras = combinedExtras)
)
```

#### Accessing Extras in Screens

```kotlin
// Method 1: In screen factory function
val profileScreen = { storeInBackStack: Boolean, params: ScatterMap<String, String>?, extras: Extra? ->
    val userContext = extras as? UserContext
    val typedParams = TypedParams.fromStringMap(params)
    
    if (userContext != null) {
        ProfileScreen(
            userId = typedParams.get<String>("userId") ?: "",
            userContext = userContext
        )
    } else {
        ErrorScreen(message = "Missing user context")
    }
}

// Method 2: In ScreenDestination implementation
class ProfileScreen(
    private val userId: String,
    private val extras: Extra? = null
) : ScreenDestination(ScreenId("profile")) {
    
    private val userContext: UserContext? = extras as? UserContext
    private val multipleExtras: MultipleExtras? = extras as? MultipleExtras
    
    @Composable
    override fun Content() {
        when {
            userContext != null -> {
                ProfileContent(
                    userId = userId,
                    currentUser = userContext.currentUser,
                    permissions = userContext.permissions
                )
            }
            multipleExtras != null -> {
                ProfileContent(
                    userId = userId,
                    currentUser = multipleExtras.userContext.currentUser,
                    mode = multipleExtras.mode,
                    permissions = multipleExtras.userContext.permissions
                )
            }
            else -> {
                ErrorContent(message = "Missing required context")
            }
        }
    }
}
```

#### Accessing Extras in ViewModels

```kotlin
// Method 1: Pass extras to ViewModel constructor
class ProfileViewModel(
    private val userId: String,
    private val userContext: UserContext?,
    private val repository: UserRepository
) : StateViewModel<ProfileState, ProfileAction, ProfileEvent, ProfileEffect>(
    initialState = ProfileState()
) {
    
    init {
        if (userContext != null) {
            // Use context for initialization
            handleAction(ProfileAction.LoadUserWithContext(userContext))
        } else {
            handleAction(ProfileAction.LoadUser(userId))
        }
    }
    
    override suspend fun processAction(action: ProfileAction): ProfileEvent = when (action) {
        is ProfileAction.LoadUserWithContext -> {
            // Use the provided context
            if (action.context.permissions.contains(Permission.VIEW_PROFILE)) {
                val user = repository.getUser(userId, action.context.sessionId)
                ProfileEvent.UserLoaded(user)
            } else {
                ProfileEvent.AccessDenied
            }
        }
        // ... other actions
    }
}

// Method 2: Extract extras in screen and pass to ViewModel
class ProfileScreen(private val extras: Extra?) : ScreenDestination(ScreenId("profile")) {
    
    @Composable
    override fun Content() {
        val userContext = extras as? UserContext
        val viewModel = remember {
            ProfileViewModel(
                userContext = userContext,
                repository = UserRepository()
            )
        }
        
        StateContent(
            viewModel = viewModel,
            onEffect = { effect ->
                when (effect) {
                    ProfileEffect.ShowError -> {
                        // Handle error
                    }
                }
            }
        ) { state, onAction ->
            ProfileUI(state = state, onAction = onAction)
        }
    }
}
```

#### Using Extras with Navigation DSL

```kotlin
val navController = navController(initialGraphId = "main") {
    graph("main") {
        screen("profile") { storeInBackStack, params, extras ->
            val userContext = extras as? UserContext
            val userId = TypedParams.fromStringMap(params).get<String>("userId") ?: ""
            
            ProfileScreen(
                userId = userId,
                userContext = userContext
            )
        }
        
        screen("settings") { _, _, extras ->
            val mode = extras as? ScreenMode ?: ScreenMode.VIEW_ONLY
            SettingsScreen(mode = mode)
        }
    }
}
```

#### Advanced Extras Patterns

##### Extras with State Management Integration

```kotlin
// Define extras that work with state management
data class StateExtras(
    val initialState: ProfileState,
    val configuration: ProfileConfiguration
) : Extra

sealed class ProfileAction : Action {
    data class InitializeWithExtras(val extras: StateExtras) : ProfileAction()
    // ... other actions
}

class ProfileViewModel : StateViewModel<ProfileState, ProfileAction, ProfileEvent, ProfileEffect>(
    initialState = ProfileState()
) {
    override suspend fun processAction(action: ProfileAction): ProfileEvent = when (action) {
        is ProfileAction.InitializeWithExtras -> {
            ProfileEvent.StateInitialized(
                state = action.extras.initialState,
                config = action.extras.configuration
            )
        }
        // ... other actions
    }
}
```

##### Extras for Navigation Results

```kotlin
// Define result extras
data class ProfileResult(
    override val id: ScreenId,
    val updatedUser: User?,
    val action: ProfileAction
) : NavigationResult

// Pass result expectation as extra
data class ResultExpectation(
    val expectedResult: KClass<out NavigationResult>,
    val onResult: suspend (NavigationResult) -> Unit
) : Extra

// Navigate with result expectation
navController.navigateForResult<ProfileResult>(
    screenId = ScreenId("editProfile"),
    options = NavOptions(
        extras = ResultExpectation(
            expectedResult = ProfileResult::class,
            onResult = { result ->
                when (result) {
                    is ProfileResult -> handleProfileUpdate(result.updatedUser)
                }
            }
        )
    )
) { result ->
    // Handle result
}
```

#### Best Practices for Extras

##### 1. Type Safety

```kotlin
// Create type-safe extra accessors
inline fun <reified T : Extra> Extra?.asType(): T? = this as? T

fun <T : Extra> Extra?.requireType(type: KClass<T>): T {
    return type.cast(this) ?: throw IllegalArgumentException("Expected ${type.simpleName}, got ${this?.javaClass?.simpleName}")
}

// Usage
val userContext = extras.asType<UserContext>()
val requiredContext = extras.requireType(UserContext::class)
```

##### 2. Validation and Error Handling

```kotlin
// Validate extras in screen factory
fun createProfileScreen(storeInBackStack: Boolean, params: ScatterMap<String, String>?, extras: Extra?) = when {
    extras == null -> ErrorScreen("Missing required context")
    extras !is UserContext -> ErrorScreen("Invalid context type")
    !extras.permissions.contains(Permission.VIEW_PROFILE) -> AccessDeniedScreen()
    else -> ProfileScreen(
        userId = TypedParams.fromStringMap(params).get<String>("userId") ?: "",
        userContext = extras
    )
}
```

##### 3. Extras Documentation

```kotlin
/**
 * Extras for user profile screens
 * 
 * @property currentUser The current logged-in user
 * @property permissions Set of permissions for the user
 * @property sessionId Current session identifier
 * 
 * Required for: ProfileScreen, EditProfileScreen, UserSettingsScreen
 * Optional for: PublicProfileScreen
 */
data class UserContext(
    val currentUser: User,
    val permissions: Set<Permission>,
    val sessionId: String
) : Extra

/**
 * Navigation metadata for analytics and debugging
 * 
 * @property sourceScreen The screen that initiated navigation
 * @property timestamp When navigation was triggered
 * @property analyticsData Additional data for analytics
 * 
 * Used by: All screens for analytics tracking
 */
data class NavigationMetadata(
    val sourceScreen: String,
    val timestamp: Long = System.currentTimeMillis(),
    val analyticsData: Map<String, Any> = emptyMap()
) : Extra
```

##### 4. Testing with Extras

```kotlin
@Test
fun `should handle navigation with extras correctly`() {
    val userContext = UserContext(
        currentUser = testUser,
        permissions = setOf(Permission.VIEW_PROFILE),
        sessionId = "test-session"
    )
    
    val success = navController.navigate(
        screenId = ScreenId("profile"),
        options = NavOptions(
            extras = userContext,
            params = buildParams {
                putString("userId", "123")
            }.toStringMap()
        )
    )
    
    assertTrue(success)
    val currentScreen = navController.currentDestination.value
    assertNotNull(currentScreen)
    // Verify extras were passed correctly
}
```

## KSP Plugin for Compile-Time Extras Validation

Compass Core includes a powerful KSP (Kotlin Symbol Processing) plugin that validates extras at
compile time, ensuring type safety and preventing runtime errors.

#### Setup KSP Plugin

Add the plugin to your module's `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}

dependencies {
    implementation("com.usmonie.compass:core:0.2.1")
    ksp("com.usmonie.compass:ksp-plugin:0.2.1")
}
```

#### Annotate Your Extras and Screens

```kotlin
// Mark extras as validated
@ValidatedExtra
data class UserContext(
    val currentUser: User,
    val permissions: Set<Permission>,
    val sessionId: String
) : Extra

@ValidatedExtra
data class NavigationMetadata(
    val sourceScreen: String,
    val timestamp: Long,
    val analyticsData: Map<String, Any>
) : Extra

@ValidatedExtra
enum class ScreenMode : Extra {
    VIEW_ONLY, EDIT_MODE, ADMIN_MODE
}

// Declare screen requirements
@RequiresExtras(
    required = [UserContext::class],
    optional = [NavigationMetadata::class]
)
class ProfileScreen(
    private val userId: String,
    private val extras: Extra? = null
) : ScreenDestination(ScreenId("profile")) {
    // Implementation
}

@RequiresExtras(
    required = [UserContext::class, ScreenMode::class],
    optional = [NavigationMetadata::class]
)
class EditProfileScreen : ScreenDestination(ScreenId("editProfile")) {
    // Implementation
}
```

#### Validate Navigation Calls

```kotlin
class NavigationController {
    
    // This will be validated at compile time
    @ProvidesExtras(
        targetScreen = "profile",
        provides = [UserContext::class, NavigationMetadata::class]
    )
    fun navigateToProfile(navController: NavController) {
        val userContext = UserContext(
            currentUser = getCurrentUser(),
            permissions = getUserPermissions(),
            sessionId = getSessionId()
        )
        
        val metadata = NavigationMetadata(
            sourceScreen = "dashboard",
            timestamp = System.currentTimeMillis(),
            analyticsData = mapOf("action" to "profile_view")
        )
        
        // KSP validates this matches ProfileScreen requirements
        navController.navigate(
            screenId = ScreenId("profile"),
            options = NavOptions(
                extras = userContext, // ✅ Validated at compile time
                params = buildParams {
                    putString("userId", "123")
                }.toStringMap()
            )
        )
    }
    
    // This will cause a compile error in strict mode
    fun navigateToProfileWithoutContext(navController: NavController) {
        navController.navigate(
            screenId = ScreenId("profile"),
            options = NavOptions(extras = null) // ❌ ERROR: Missing required UserContext
        )
    }
}
```

#### Configuration Options

Configure the plugin behavior:

```kotlin
@ExtrasValidationConfig(
    strict = true, // Fail compilation on validation errors
    generateHelpers = true, // Generate type-safe helper functions  
    generatedPackage = "com.myapp.generated" // Package for generated code
)
object ExtrasConfig
```

#### Generated Helper Functions

When `generateHelpers = true`, the KSP plugin generates type-safe accessor functions:

```kotlin
// Generated automatically by KSP
fun Extra?.getProfileScreenExtras(): UserContext? = this as? UserContext

fun Extra?.validateProfileScreenExtras(): Boolean {
    val extras = this as? UserContext ?: return false
    // Generated validation logic
    return true
}

fun validateExtrasForScreen(screenId: String, extras: Extra?): Boolean {
    return when (screenId) {
        "profile" -> extras is UserContext
        "editProfile" -> extras is UserContext // More complex validation for multiple requirements
        else -> true
    }
}

// Usage in your code
fun handleNavigation(extras: Extra?) {
    // Type-safe access
    val userContext = extras.getProfileScreenExtras()
    
    // Validation
    val isValid = extras.validateProfileScreenExtras()
    
    // Screen-specific validation
    val isValidForScreen = validateExtrasForScreen("profile", extras)
}
```

#### Advanced Usage Patterns

##### Screen Factory Validation

```kotlin
fun createProfileScreen(
    storeInBackStack: Boolean,
    params: ScatterMap<String, String>?,
    @FromExtras(UserContext::class) extras: Extra?
): ScreenDestination {
    val userContext = extras.getProfileScreenExtras()
        ?: throw IllegalArgumentException("ProfileScreen requires UserContext extra")
    
    return ProfileScreen(
        userId = TypedParams.fromStringMap(params).get<String>("userId") ?: "",
        userContext = userContext
    )
}
```

##### Multiple Extras with Wrapper

```kotlin
// Wrapper for multiple extras
@ValidatedExtra
data class MultipleExtras(
    val userContext: UserContext,
    val mode: ScreenMode,
    val metadata: NavigationMetadata? = null
) : Extra

@RequiresExtras(required = [MultipleExtras::class])
class ComplexScreen : ScreenDestination(ScreenId("complex")) {
    // Implementation
}

@ProvidesExtras(
    targetScreen = "complex",
    provides = [MultipleExtras::class]
)
fun navigateToComplex(navController: NavController) {
    val extras = MultipleExtras(
        userContext = UserContext(/* ... */),
        mode = ScreenMode.EDIT_MODE,
        metadata = NavigationMetadata(/* ... */)
    )
    
    navController.navigate(
        screenId = ScreenId("complex"),
        options = NavOptions(extras = extras)
    )
}
```

##### Inheritance Support

```kotlin
@ValidatedExtra
open class BaseContext(val sessionId: String) : Extra

@ValidatedExtra
class AdminContext(
    sessionId: String,
    val adminLevel: Int
) : BaseContext(sessionId)

@RequiresExtras(required = [BaseContext::class])
class SomeScreen : ScreenDestination(ScreenId("some")) {
    // Accepts BaseContext or any subtype like AdminContext
}
```

#### Compilation Validation Results

The KSP plugin provides detailed feedback during compilation:

```
✅ Validated navigation to profile: required=[UserContext], provided=[UserContext, NavigationMetadata]
⚠️  Unknown extras provided to screen 'profile': [UnknownExtra] at NavigationController.kt:45
❌ Missing required extras for screen 'editProfile': [ScreenMode] at NavigationController.kt:67
```

#### Benefits of KSP Validation

1. **Compile-Time Safety**: Catch extras mismatches before runtime
2. **Documentation**: Annotations serve as documentation for screen requirements
3. **IDE Support**: Generated functions provide autocomplete and type checking
4. **Refactoring Safety**: Changes to extras requirements are validated across the codebase
5. **Performance**: No runtime overhead, all validation happens at compile time

#### Migration from Runtime Validation

```kotlin
// Before: Runtime validation
class ProfileScreen(private val extras: Extra?) : ScreenDestination(ScreenId("profile")) {
    @Composable
    override fun Content() {
        val userContext = extras as? UserContext
            ?: return ErrorContent("Missing user context") // Runtime error
        
        ProfileContent(userContext = userContext)
    }
}

// After: Compile-time validation
@RequiresExtras(required = [UserContext::class])
class ProfileScreen(private val extras: Extra?) : ScreenDestination(ScreenId("profile")) {
    @Composable
    override fun Content() {
        // Guaranteed to be UserContext by KSP validation
        val userContext = extras.getProfileScreenExtras()!! 
        
        ProfileContent(userContext = userContext)
    }
}
```

This KSP plugin significantly improves the developer experience by catching extras-related errors at
compile time and generating helpful utility functions for type-safe extras handling.

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
