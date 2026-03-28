package com.usmonie.compass.screen.state

import androidx.collection.ScatterMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.EntryProviderScope
import com.usmonie.compass.screen.state.navigation.ScreenDestination
import com.usmonie.compass.screen.state.navigation.ScreenId
import com.usmonie.compass.state.Action
import com.usmonie.compass.state.ActionProcessor
import com.usmonie.compass.state.Effect
import com.usmonie.compass.state.Event
import com.usmonie.compass.state.EventHandler
import com.usmonie.compass.state.State
import com.usmonie.compass.state.StateManager
import com.usmonie.compass.state.StateViewModel
import com.usmonie.compass.state.ViewModelStore
import kotlin.jvm.JvmSuppressWildcards

public val LocalSaveableStateHolder: ProvidableCompositionLocal<SaveableStateHolder?> = staticCompositionLocalOf<SaveableStateHolder?> { null }

@Composable
public fun CompassSaveableState(
    backStack: List<Any>? = null,
    content: @Composable () -> Unit
) {
    val holder = rememberSaveableStateHolder()
    
    if (backStack != null) {
        remember(backStack) {
            ViewModelStore.sync(backStack) { removedKey ->
                holder.removeState(removedKey.hashCode())
            }
        }
    }

    CompositionLocalProvider(LocalSaveableStateHolder provides holder) {
        content()
    }
}

@Composable
@PublishedApi
internal fun <K : ScreenId, D : ScreenDestination<K>> SaveableScreenContent(
    screenId: K,
    screenDestination: (K) -> D
) {
    val destination = remember(screenId) {
        ViewModelStore.getOrPut(screenId) { screenDestination(screenId) }
    }
    val holder = LocalSaveableStateHolder.current
    if (holder != null) {
        holder.SaveableStateProvider(screenId.hashCode()) {
            destination.Content()
        }
    } else {
        destination.Content()
    }
}

/**
 * DSL функция для создания state-managed screen
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
 * DSL функция с явными параметрами (использует ActionProcessor)
 */
public fun <K : ScreenId, S : State, A : Action, V : Event, F : Effect> stateScreen(
    id: K,
    storeInBackStack: Boolean = true,
    initialState: S,
    processor: ActionProcessor<A, S, V>,
    handler: EventHandler<V, S, F>,
    manager: StateManager<S, V>,
    onInit: (StateViewModel<S, A, V, F>.() -> Unit)? = null,
    onEffect: (@Composable (S, F?) -> Unit)? = null,
    screen: @Composable (S, (A) -> Unit) -> Unit,
): StateScreenDestination<K, S, A, V, F> {
    val screenBuilder = StateScreenBuilder<K, S, A, V, F>()
    screenBuilder.apply {
        initialState(initialState)
        onInit?.let(::init)
        processAction(processor)
        handleEvent(handler)
        reduce(manager)
        if (onEffect != null) {
            onEffect(onEffect)
        }
        content { state, onAction -> screen(state, onAction) }
    }
    return screenBuilder.build(id, storeInBackStack)
}

/**
 * DSL функция с опциональными параметрами
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
        screenBuilder.processAction(actionProcessor)
    }

    if (eventHandler != null) {
        screenBuilder.handleEvent(eventHandler)
    }

    if (stateManager != null) {
        screenBuilder.reduce(stateManager)
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
    val screenBuilder =
        StateScreenBuilder<K, S, SimpleAction<S>, SimpleEvent<S>, SimpleEffect>()
    screenBuilder.apply {
        builder()
        initialState(initialState)
        processAction { action, _, _, _ ->
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

public fun <K : ScreenId, S : State, A : Action, V : Event, F : Effect> EntryProviderScope<K>.stateEntry(
    key: K,
    screenDestination: (K) -> StateScreenDestination<K, S, A, V, F>,
    metadata: Map<String, Any> = emptyMap(),
) {
    entry(
        key = key,
        metadata = metadata,
    ) { screenId ->
        SaveableScreenContent(screenId, screenDestination)
    }
}

public fun <K : ScreenId> EntryProviderScope<K>.entry(
    key: K,
    screenDestination: (K) -> ScreenDestination<K>,
    metadata: Map<String, Any> = emptyMap(),
) {
    entry(
        key = key,
        metadata = metadata,
    ) { screenId ->
        SaveableScreenContent(screenId, screenDestination)
    }
}

public inline fun <reified K : ScreenId, S : State, A : Action, V : Event, F : Effect> EntryProviderScope<ScreenId>.stateEntry(
    metadata: Map<String, Any> = emptyMap(),
    noinline screenDestination: (K) -> StateScreenDestination<K, S, A, V, F>,
) {
    entry<K>(
        metadata = metadata,
    ) { screenId ->
        SaveableScreenContent(screenId, screenDestination)
    }
}

public inline fun <reified K : ScreenId> EntryProviderScope<ScreenId>.entry(
    noinline contentKey: (key: @JvmSuppressWildcards K) -> Any = { it.toString() },
    metadata: Map<String, Any> = emptyMap(),
    noinline screenDestination: (K) -> ScreenDestination<K>,
) {
    entry<K>(
        clazzContentKey = contentKey,
        metadata = metadata,
    ) { screenId ->
        SaveableScreenContent(screenId, screenDestination)
    }
}


public inline fun <reified K : ScreenId> EntryProviderScope<ScreenId>.screen(
    noinline contentKey: (key: @JvmSuppressWildcards K) -> Any = { it.toString() },
    metadata: Map<String, Any> = emptyMap(),
    noinline screenDestination: (K) -> ScreenDestination<K>,
) {
    entry<K>(
        clazzContentKey = contentKey,
        metadata = metadata,
    ) { screenId ->
        SaveableScreenContent(screenId, screenDestination)
    }
}

public inline fun <reified K : ScreenId, S : State> EntryProviderScope<ScreenId>.simpleEntry(
    metadata: Map<String, Any> = emptyMap(),
    noinline screenDestination: (K) -> StateScreenDestination<K, S, SimpleAction<S>, SimpleEvent<S>, SimpleEffect>,
) {
    entry<K>(
        metadata = metadata,
    ) { screenId ->
        SaveableScreenContent(screenId, screenDestination)
    }
}
