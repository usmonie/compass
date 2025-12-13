package com.usmonie.compass.screen.state

import androidx.collection.ScatterMap
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.usmonie.compass.core.Extra
import com.usmonie.compass.core.navigation.ScreenDestination
import com.usmonie.compass.core.navigation.ScreenId
import com.usmonie.compass.state.Action
import com.usmonie.compass.state.ActionProcessor
import com.usmonie.compass.state.Effect
import com.usmonie.compass.state.Event
import com.usmonie.compass.state.EventHandler
import com.usmonie.compass.state.State
import com.usmonie.compass.state.StateManager

/**
 * DSL function to create a state-managed screen
 */
public inline fun <K : ScreenId, S : State, A : Action, V : Event, F : Effect> stateScreen(
    id: K,
    storeInBackStack: Boolean = true,
    builder: StateScreenBuilder<K, S, A, V, F>.() -> Unit,
): StateScreenDestination<K, S, A, V, F> {
    val screenBuilder = StateScreenBuilder<K, S, A, V, F>()
    screenBuilder.apply(builder)
    return screenBuilder.build(id, storeInBackStack)
}

/**
 * DSL function to create a state-managed screen
 */
public inline fun <K : ScreenId, S : State, A : Action, V : Event, F : Effect> stateScreen(
    id: K,
    storeInBackStack: Boolean = true,
    actionProcessor: ActionProcessor<A, S, V>? = null,
    eventHandler: EventHandler<V, S, F>? = null,
    stateManager: StateManager<S, V>? = null,
    builder: StateScreenBuilder<K, S, A, V, F>.() -> Unit,
): StateScreenDestination<K, S, A, V, F> {
    val screenBuilder = StateScreenBuilder<K, S, A, V, F>()
    if (actionProcessor != null) {
        screenBuilder.processAction { action, state ->
            actionProcessor.process(this, action, state)
        }
    }
    if (eventHandler != null) {
        screenBuilder.handleEvent { event, state ->
            eventHandler.handle(event, state)
        }
    }
    if (stateManager != null) {
        screenBuilder.reduce { event ->
            stateManager.reduce(this, event)
        }
    }
    screenBuilder.apply(builder)
    return screenBuilder.build(id, storeInBackStack)
}

/**
 * DSL function to create a simple state screen
 */
public inline fun <K : ScreenId, S : State> simpleStateScreen(
    id: K,
    initialState: S,
    storeInBackStack: Boolean = true,
    builder: StateScreenBuilder<K, S, SimpleAction<S>, SimpleEvent<S>, SimpleEffect>.() -> Unit,
): StateScreenDestination<K, S, SimpleAction<S>, SimpleEvent<S>, SimpleEffect> {
    val screenBuilder = StateScreenBuilder<K, S, SimpleAction<S>, SimpleEvent<S>, SimpleEffect>()
    screenBuilder.apply {
        builder()
        initialState(initialState)
        processAction { action, _ ->
            when (action) {
                is SimpleAction.UpdateState<S> -> SimpleEvent.StateUpdated(action.newState)
            }
        }
        handleEvent { _, _ -> null }
        reduce {
            when (it) {
                is SimpleEvent.StateUpdated<S> -> it.newState
            }
        }
    }
    screenBuilder.apply(builder)
    return screenBuilder.build(id, storeInBackStack)
}

/**
 * Helper function to create a screen factory from a simple state screen
 */
public fun <K : ScreenId, S : State> StateScreenDestination<K, S, SimpleAction<S>, SimpleEvent<S>, SimpleEffect>.toFactory(): (Boolean, ScatterMap<String, String>?, Extra?) -> StateScreenDestination<K, S, SimpleAction<S>, SimpleEvent<S>, SimpleEffect> =
    { storeInBackStack: Boolean, params: ScatterMap<String, String>?, extra: Extra? ->
        // Create a copy with the provided parameters
        this
    }

public fun <K : ScreenId, S : State, A : Action, V : Event, F : Effect> EntryProviderScope<NavKey>.stateEntry(
    key: K,
    screenDestination: (K) -> StateScreenDestination<K, S, A, V, F>,
    metadata: Map<String, Any> = emptyMap(),
) {
    entry(
        key = key,
        metadata = metadata,
    ) {
        val screenDestination = screenDestination(it)
        screenDestination.Content()
    }
}

public fun <K : ScreenId> EntryProviderScope<NavKey>.entry(
    key: K,
    screenDestination: (K) -> ScreenDestination<K>,
    metadata: Map<String, Any> = emptyMap(),
) {
    entry(
        key = key,
        metadata = metadata,
    ) {
        val screenDestination = screenDestination(it)
        screenDestination.Content()
    }
}

public inline fun <reified K : ScreenId, S : State, A : Action, V : Event, F : Effect> EntryProviderScope<NavKey>.stateEntry(
    crossinline screenDestination: (K) -> StateScreenDestination<K, S, A, V, F>,
    metadata: Map<String, Any> = emptyMap(),
) {
    entry<K>(
        metadata = metadata,
    ) {
        val screenDestination = screenDestination(it)
        screenDestination.Content()
    }
}

public inline fun <reified K : ScreenId, S : State> EntryProviderScope<NavKey>.simpleEntry(
    crossinline screenDestination: (K) -> StateScreenDestination<K, S, SimpleAction<S>, SimpleEvent<S>, SimpleEffect>,
    metadata: Map<String, Any> = emptyMap(),
) {
    entry<K>(
        metadata = metadata,
    ) {
        val screenDestination = screenDestination(it)
        screenDestination.Content()
    }
}
