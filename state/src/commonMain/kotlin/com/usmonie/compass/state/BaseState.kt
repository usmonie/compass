package com.usmonie.compass.state


/**
 * Base interface for representing the state in MVI architecture.
 * All state implementations should be immutable and stable for Compose.
 *
 * States represent the current condition of your application or screen at any given moment.
 * They should be immutable data structures to ensure predictable state management and
 * proper Compose recomposition behavior.
 *
 * Example implementations:
 * ```kotlin
 * @Immutable
 * data class UserProfileState(
 *     val user: User? = null,
 *     val isLoading: Boolean = false,
 *     val error: String? = null
 * ) : State
 *
 * @Immutable
 * data class CounterState(
 *     val count: Int = 0,
 *     val maxReached: Boolean = false
 * ) : State
 * ```
 */
public interface State

/**
 * A specialized state that represents an error condition.
 * Provides a consistent way to handle errors across the application.
 *
 * This abstract class provides a standardized way to represent error states
 * throughout your application, ensuring consistent error handling and messaging.
 *
 * Example usage:
 * ```kotlin
 * class NetworkErrorState(error: Throwable) : ErrorState(error)
 * class ValidationErrorState(error: Throwable) : ErrorState(error)
 *
 * // Usage in your state
 * data class LoginState(
 *     val loginError: ErrorState? = null,
 *     val isLoading: Boolean = false
 * ) : State
 * ```
 */
public abstract class ErrorState(error: Throwable) : State {
    /**
     * The error message, defaulting to "Unknown error" if null
     */
    public val message: String = error.message ?: "Unknown error"

    /**
     * The original throwable for detailed error handling
     */
    public val throwable: Throwable = error
}

/**
 * Base interface for representing events that occur in MVI architecture.
 * Events are the result of processing actions and trigger state changes.
 *
 * Events represent internal occurrences within your application that should
 * result in state changes. They are produced by ActionProcessors and consumed
 * by StateManagers to create new states.
 *
 * Events should be:
 * - Immutable data classes
 * - Descriptive of what happened
 * - Contain any data needed for state reduction
 *
 * Example implementations:
 * ```kotlin
 * sealed class UserEvent : Event {
 *     object LoadingStarted : UserEvent()
 *     data class UserLoaded(val user: User) : UserEvent()
 *     data class LoadingFailed(val error: Throwable) : UserEvent()
 * }
 * ```
 */
public interface Event

/**
 * Base interface for representing user actions in MVI architecture.
 * Actions represent user intents and are processed to generate events.
 *
 * Actions represent what the user wants to do. They should be descriptive
 * and contain all the information needed to process the user's intent.
 *
 * Actions should be:
 * - Immutable data classes or objects
 * - Named with verbs describing user intent
 * - Contain parameters needed for processing
 *
 * Example implementations:
 * ```kotlin
 * sealed class UserAction : Action {
 *     object LoadUser : UserAction()
 *     data class UpdateName(val newName: String) : UserAction()
 *     data class SaveUser(val user: User) : UserAction()
 * }
 * ```
 */
public interface Action

/**
 * Base interface for representing side effects in MVI architecture.
 * Effects are one-time events that don't change state (navigation, showing toasts, etc.)
 *
 * Effects represent one-time side effects that should happen as a result of
 * state changes or events, but don't directly modify the state themselves.
 * Common examples include navigation, showing toasts, or triggering animations.
 *
 * Effects should be:
 * - Immutable data classes or objects
 * - Represent one-time occurrences
 * - Not modify application state directly
 *
 * Example implementations:
 * ```kotlin
 * sealed class UserEffect : Effect {
 *     object NavigateToHome : UserEffect()
 *     data class ShowToast(val message: String) : UserEffect()
 *     data class ShareContent(val content: String) : UserEffect()
 * }
 * ```
 */
public interface Effect
