package com.usmonie.compass.screen.state

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavEntry
import com.usmonie.compass.component.state.SimpleStateContent
import com.usmonie.compass.component.state.StateContent
import com.usmonie.compass.core.navigation.ScreenId
import com.usmonie.compass.state.Action
import com.usmonie.compass.state.Effect
import com.usmonie.compass.state.Event
import com.usmonie.compass.state.FlowStateViewModel
import com.usmonie.compass.state.State
import com.usmonie.compass.state.StateViewModel
import com.usmonie.compass.state.createStateViewModel
import com.usmonie.compass.state.flowStateViewModel
import com.usmonie.compass.state.stateViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * DSL for creating a complete state-managed screen with minimal boilerplate
 */
public class StateScreenBuilder<S : State, A : Action, V : Event, F : Effect> {
    private var initialState: S? = null
    private var processAction: (suspend CoroutineScope.(A, S) -> V)? = null
    private var handleEvent: ((V, S) -> F?)? = null
    private var reduce: (S.(V) -> S)? = null
    private var content: (@Composable (S, (A) -> Unit) -> Unit)? = null
    private var onEffect: (suspend (F) -> Unit)? = null

    public fun initialState(state: S) {
        initialState = state
    }

    public fun processAction(processor: suspend CoroutineScope.(A, S) -> V) {
        processAction = processor
    }

    public fun handleEvent(handler: (V, S) -> F?) {
        handleEvent = handler
    }

    public fun reduce(reducer: S.(V) -> S) {
        reduce = reducer
    }

    public fun content(composable: @Composable (S, (A) -> Unit) -> Unit) {
        content = composable
    }

    public fun onEffect(handler: suspend (F) -> Unit) {
        onEffect = handler
    }

    public fun build(
        screenId: ScreenId,
        storeInBackStack: Boolean
    ): StateScreenDestination<S, A, V, F> {
        return StateScreenDestination(
            id = screenId,
            storeInBackStack = storeInBackStack,
            viewModel = stateViewModel(
                initialState = requireNotNull(initialState) { "Initial state must be provided" },
                processAction = requireNotNull(processAction) { "Action processor must be provided" },
                handleEvent = requireNotNull(handleEvent) { "Event handler must be provided" },
                reduce = requireNotNull(reduce) { "State reducer must be provided" }
            ),
            content = requireNotNull(content) { "Content composable must be provided" },
            onEffect = onEffect ?: {}
        )
    }
}

/**
 * Screen destination that integrates with state management
 */
public class StateScreenDestination<S : State, A : Action, V : Event, F : Effect>(
    public val id: ScreenId,
    public val storeInBackStack: Boolean,
    private val viewModel: StateViewModel<S, A, V, F>,
    private val content: @Composable (S, (A) -> Unit) -> Unit,
    private val onEffect: suspend (F) -> Unit
) {

    @Composable
    public fun Content() {
        StateContent(
            viewModel = viewModel,
            onEffect = onEffect,
            content = content
        )
    }

    public fun onCleared() {
        viewModel.onDispose()
    }
}

/**
 * DSL function to create a state-managed screen
 */
public inline fun <S : State, A : Action, V : Event, F : Effect> stateScreen(
    id: ScreenId,
    storeInBackStack: Boolean = true,
    builder: StateScreenBuilder<S, A, V, F>.() -> Unit
): StateScreenDestination<S, A, V, F> {
    val screenBuilder = StateScreenBuilder<S, A, V, F>()
    screenBuilder.apply(builder)
    return screenBuilder.build(id, storeInBackStack)
}

/**
 * DSL function to create a state-managed screen with string ID
 */
public inline fun <S : State, A : Action, V : Event, F : Effect> stateScreen(
    id: String,
    storeInBackStack: Boolean = true,
    builder: StateScreenBuilder<S, A, V, F>.() -> Unit
): StateScreenDestination<S, A, V, F> = stateScreen(ScreenId(id), storeInBackStack, builder)

/**
 * DSL for creating a complete state-managed screen with minimal boilerplate and flow actions
 */
public class FlowStateScreenBuilder<S : State, A : Action, V : Event, F : Effect> {
    private var initialState: S? = null
    private var processAction: (suspend CoroutineScope.(A, S) -> Flow<V>)? = null
    private var handleEvent: ((V, S) -> F?)? = null
    private var reduce: (S.(V) -> S)? = null
    private var content: (@Composable (S, (A) -> Unit) -> Unit)? = null
    private var onEffect: (suspend (F) -> Unit)? = null

    public fun initialState(state: S) {
        initialState = state
    }

    public fun processAction(processor: suspend CoroutineScope.(A, S) -> Flow<V>) {
        processAction = processor
    }

    public fun handleEvent(handler: (V, S) -> F?) {
        handleEvent = handler
    }

    public fun reduce(reducer: S.(V) -> S) {
        reduce = reducer
    }

    public fun content(composable: @Composable (S, (A) -> Unit) -> Unit) {
        content = composable
    }

    public fun onEffect(handler: suspend (F) -> Unit) {
        onEffect = handler
    }

    public fun build(
        screenId: ScreenId,
        storeInBackStack: Boolean
    ): FlowStateScreenDestination<S, A, V, F> {
        return FlowStateScreenDestination(
            id = screenId,
            storeInBackStack = storeInBackStack,
            viewModel = flowStateViewModel(
                initialState = requireNotNull(initialState) { "Initial state must be provided" },
                processAction = requireNotNull(processAction) { "Action processor must be provided" },
                handleEvent = requireNotNull(handleEvent) { "Event handler must be provided" },
                reduce = requireNotNull(reduce) { "State reducer must be provided" }
            ),
            content = requireNotNull(content) { "Content composable must be provided" },
            onEffect = onEffect ?: {}
        )
    }
}

/**
 * Screen destination that integrates with state management and flow actions.
 */
public class FlowStateScreenDestination<S : State, A : Action, V : Event, F : Effect>(
    public val id: ScreenId,
    public val storeInBackStack: Boolean,
    private val viewModel: FlowStateViewModel<S, A, V, F>,
    private val content: @Composable (S, (A) -> Unit) -> Unit,
    private val onEffect: suspend (F) -> Unit
) {

    @Composable
    public fun Content() {
        StateContent(
            viewModel = viewModel,
            onEffect = onEffect,
            content = content
        )
    }

    public fun onCleared() {
        viewModel.onDispose()
    }
}

/**
 * DSL function to create a state-managed screen with actions flow.
 */
public inline fun <S : State, A : Action, V : Event, F : Effect> flowStateScreen(
    id: ScreenId,
    storeInBackStack: Boolean = true,
    builder: FlowStateScreenBuilder<S, A, V, F>.() -> Unit
): FlowStateScreenDestination<S, A, V, F> {
    val screenBuilder = FlowStateScreenBuilder<S, A, V, F>()
    screenBuilder.apply(builder)
    return screenBuilder.build(id, storeInBackStack)
}

/**
 * DSL function to create a state-managed screen with string ID and actions flow.
 */
public inline fun <S : State, A : Action, V : Event, F : Effect> flowStateScreen(
    id: String,
    storeInBackStack: Boolean = true,
    builder: FlowStateScreenBuilder<S, A, V, F>.() -> Unit
): FlowStateScreenDestination<S, A, V, F> = flowStateScreen(ScreenId(id), storeInBackStack, builder)

/**
 * Simple state screen DSL for screens that only need basic state management
 */
public class SimpleStateScreenBuilder<S : State> {
    private var initialState: S? = null
    private var content: (@Composable (S, (S) -> Unit) -> Unit)? = null

    public fun initialState(state: S) {
        initialState = state
    }

    public fun content(composable: @Composable (S, (S) -> Unit) -> Unit) {
        content = composable
    }

    public fun build(
        screenId: ScreenId,
        storeInBackStack: Boolean
    ): SimpleStateScreenDestination<S> {
        return SimpleStateScreenDestination(
            id = screenId,
            storeInBackStack = storeInBackStack,
            initialState = requireNotNull(initialState) { "Initial state must be provided" },
            content = requireNotNull(content) { "Content composable must be provided" }
        )
    }
}

/**
 * Simple state screen destination for basic state management
 */
public class SimpleStateScreenDestination<S : State>(
    public val id: ScreenId,
    public val storeInBackStack: Boolean,
    private val initialState: S,
    private val content: @Composable (S, (S) -> Unit) -> Unit
) {

    // Simple actions and events for basic state updates
    private sealed class SimpleAction<S : State> :
        Action {
        data class UpdateState<S : State>(val newState: S) :
            SimpleAction<S>()
    }

    private sealed class SimpleEvent<S : State> :
        Event {
        data class StateUpdated<S : State>(val newState: S) :
            SimpleEvent<S>()
    }

    private val viewModel = createStateViewModel<S, SimpleAction<S>, SimpleEvent<S>, Nothing>(
        initialState = initialState,
        processAction = { action, _ ->
            when (action) {
                is SimpleAction.UpdateState -> SimpleEvent.StateUpdated(action.newState)
            }
        },
        handleEvent = { _, _ -> null },
        reduce = { event ->
            when (event) {
                is SimpleEvent.StateUpdated -> event.newState
            }
        }
    )

    @Composable
    public fun Content() {
        SimpleStateContent(
            viewModel = viewModel,
            onEffect = {},
            content = { state, _ ->
                content(state) { newState ->
                    viewModel.handleAction(SimpleAction.UpdateState<S>(newState))
                }
            }
        )
    }

    public fun onCleared() {
        viewModel.onDispose()
    }
}

/**
 * DSL function to create a simple state screen
 */
public inline fun <S : State> simpleStateScreen(
    id: ScreenId,
    storeInBackStack: Boolean = true,
    builder: SimpleStateScreenBuilder<S>.() -> Unit
): SimpleStateScreenDestination<S> {
    val screenBuilder = SimpleStateScreenBuilder<S>()
    screenBuilder.apply(builder)
    return screenBuilder.build(id, storeInBackStack)
}

/**
 * DSL function to create a simple state screen with string ID
 */
public inline fun <S : State> simpleStateScreen(
    id: String,
    storeInBackStack: Boolean = true,
    builder: SimpleStateScreenBuilder<S>.() -> Unit
): SimpleStateScreenDestination<S> = simpleStateScreen(ScreenId(id), storeInBackStack, builder)

/**
 * DSL for building navigation graphs with androidx.navigation3 integration
 */
public class Navigation3GraphBuilder {
    private val entryProvider = StateNavEntryProvider()
    
    /**
     * Add a state-managed screen to the navigation graph
     */
    public fun <S : State, A : Action, V : Event, F : Effect> screen(
        screenId: ScreenId,
        initialState: S,
        processAction: suspend CoroutineScope.(A, S) -> V,
        handleEvent: (V, S) -> F?,
        reduce: S.(V) -> S,
        onEffect: suspend (F) -> Unit = {},
        metadata: Map<String, Any> = emptyMap(),
        content: @Composable (S, (A) -> Unit) -> Unit
    ) {
        entryProvider.registerScreen(
            screenId = screenId,
            viewModelFactory = {
                stateViewModel(
                    initialState = initialState,
                    processAction = processAction,
                    handleEvent = handleEvent,
                    reduce = reduce
                )
            },
            content = { viewModel: StateViewModel<S, A, V, F> ->
                StateContent(
                    viewModel = viewModel,
                    onEffect = onEffect,
                    content = content
                )
            },
            metadata = metadata
        )
    }
    
    /**
     * Add a flow state-managed screen to the navigation graph
     */
    public fun <S : State, A : Action, V : Event, F : Effect> flowScreen(
        screenId: ScreenId,
        initialState: S,
        processAction: suspend CoroutineScope.(A, S) -> Flow<V>,
        handleEvent: (V, S) -> F?,
        reduce: S.(V) -> S,
        onEffect: suspend (F) -> Unit = {},
        metadata: Map<String, Any> = emptyMap(),
        content: @Composable (S, (A) -> Unit) -> Unit
    ) {
        entryProvider.registerFlowScreen(
            screenId = screenId,
            viewModelFactory = {
                flowStateViewModel(
                    initialState = initialState,
                    processAction = processAction,
                    handleEvent = handleEvent,
                    reduce = reduce
                )
            },
            content = { viewModel ->
                StateContent(
                    viewModel = viewModel,
                    onEffect = onEffect,
                    content = content
                )
            },
            metadata = metadata
        )
    }
    
    /**
     * Add a simple state screen to the navigation graph
     */
    public fun <S : State> simpleScreen(
        screenId: ScreenId,
        initialState: S,
        metadata: Map<String, Any> = emptyMap(),
        content: @Composable (S, (S) -> Unit) -> Unit
    ) {
        screen<S, SimpleAction<S>, SimpleEvent<S>, Nothing>(
            screenId = screenId,
            initialState = initialState,
            processAction = { action, _ ->
                when (action) {
                    is SimpleAction.UpdateState -> SimpleEvent.StateUpdated(action.newState)
                }
            },
            handleEvent = { _, _ -> null },
            reduce = { event ->
                when (event) {
                    is SimpleEvent.StateUpdated -> event.newState
                }
            },
            metadata = metadata,
            content = { state, _ ->
                content(state) { newState ->
                    // This would need access to the action handler
                    // In practice, you'd use LocalStateViewModel.current
                }
            }
        )
    }
    
    /**
     * Build the entry provider
     */
    public fun build(): (Any) -> NavEntry<Any> = entryProvider.build()
    
    // Internal classes for simple state management
    private sealed class SimpleAction<S : State> : Action {
        data class UpdateState<S : State>(val newState: S) : SimpleAction<S>()
    }
    
    private sealed class SimpleEvent<S : State> : Event {
        data class StateUpdated<S : State>(val newState: S) : SimpleEvent<S>()
    }
}

/**
 * DSL function to create a navigation graph with androidx.navigation3
 */
public inline fun navigation3Graph(
    builder: Navigation3GraphBuilder.() -> Unit
): (Any) -> NavEntry<Any> {
    val graphBuilder = Navigation3GraphBuilder()
    graphBuilder.apply(builder)
    return graphBuilder.build()
}

/**
 * Extension functions for EntryProviderBuilder to add state screens
 */
public fun <S : State, A : Action, V : Event, F : Effect> EntryProviderBuilder<*>.stateScreen(
    screenId: ScreenId,
    initialState: S,
    processAction: suspend CoroutineScope.(A, S) -> V,
    handleEvent: (V, S) -> F?,
    reduce: S.(V) -> S,
    onEffect: suspend (F) -> Unit = {},
    metadata: Map<String, Any> = emptyMap(),
    content: @Composable (S, (A) -> Unit) -> Unit
) {
    val viewModel = stateViewModel(
        initialState = initialState,
        processAction = processAction,
        handleEvent = handleEvent,
        reduce = reduce
    )
    
    addEntryProvider(
        key = screenId,
        contentKey = screenId.id,
        metadata = metadata
    ) { _ ->
        StateContent(
            viewModel = viewModel,
            onEffect = onEffect,
            content = content
        )
    }
}
