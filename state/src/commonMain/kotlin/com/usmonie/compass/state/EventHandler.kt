package com.usmonie.compass.state

/**
 * Interface for handling events and optionally producing side effects in MVI architecture.
 *
 * EventHandler is responsible for determining what side effects should occur as a result
 * of specific events. It examines events and the current state to decide whether any
 * effects (like navigation, showing toasts, etc.) should be triggered.
 *
 * This component separates side effect logic from state reduction logic, maintaining
 * clean separation of concerns in the MVI pattern.
 *
 * @param Event The type of events this handler can process
 * @param State The type of the current application state
 * @param Effect The type of side effects this handler can produce
 *
 * Example usage:
 * ```kotlin
 * val eventHandler = EventHandler<UserEvent, UserState, UserEffect> { event, state ->
 *     when (event) {
 *         is UserEvent.LoginSuccessful -> UserEffect.NavigateToHome
 *         is UserEvent.LoginFailed -> UserEffect.ShowToast(event.error.message)
 *         is UserEvent.UserLoaded -> {
 *             if (state.isFirstTimeUser) UserEffect.ShowWelcomeDialog else null
 *         }
 *         else -> null
 *     }
 * }
 * ```
 */
public fun interface EventHandler<in Event : com.usmonie.compass.state.Event, in State : com.usmonie.compass.state.State, out Effect : com.usmonie.compass.state.Effect> {
    /**
     * Handles an event and optionally produces a side effect based on the current state.
     *
     * This method is called after an event has been processed and state has been updated.
     * It can examine both the event and the current state to determine if any side effects
     * should be triggered.
     *
     * @param event The event that occurred
     * @param state The current application state
     * @return An effect to be executed, or null if no side effect is needed
     */
    public fun handle(event: Event, state: State): Effect?
}
