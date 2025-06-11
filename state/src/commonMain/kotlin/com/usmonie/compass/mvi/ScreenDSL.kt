package com.usmonie.compass.mvi

import androidx.collection.ScatterMap
import androidx.compose.runtime.Composable
import com.usmonie.compass.core.Extra
import com.usmonie.compass.core.navigation.ScreenDestination
import com.usmonie.compass.core.navigation.ScreenId
import com.usmonie.compass.state.Action
import com.usmonie.compass.state.Effect
import com.usmonie.compass.state.Event
import com.usmonie.compass.state.FlowStateViewModel
import com.usmonie.compass.state.SimpleStateContent
import com.usmonie.compass.state.State
import com.usmonie.compass.state.StateContent
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
    id: ScreenId,
    storeInBackStack: Boolean,
    private val viewModel: StateViewModel<S, A, V, F>,
    private val content: @Composable (S, (A) -> Unit) -> Unit,
    private val onEffect: suspend (F) -> Unit
) : ScreenDestination(id, storeInBackStack) {

    @Composable
    override fun Content() {
        StateContent(
            viewModel = viewModel,
            onEffect = onEffect,
            content = content
        )
    }

    override fun onCleared() {
        super.onCleared()
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
    id: ScreenId,
    storeInBackStack: Boolean,
    private val viewModel: FlowStateViewModel<S, A, V, F>,
    private val content: @Composable (S, (A) -> Unit) -> Unit,
    private val onEffect: suspend (F) -> Unit
) : ScreenDestination(id, storeInBackStack) {

    @Composable
    override fun Content() {
        StateContent(
            viewModel = viewModel,
            onEffect = onEffect,
            content = content
        )
    }

    override fun onCleared() {
        super.onCleared()
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
    id: ScreenId,
    storeInBackStack: Boolean,
    private val initialState: S,
    private val content: @Composable (S, (S) -> Unit) -> Unit
) : ScreenDestination(id, storeInBackStack) {

    // Simple actions and events for basic state updates
    private sealed class SimpleAction<S : State> : Action {
        data class UpdateState<S : State>(val newState: S) : SimpleAction<S>()
    }

    private sealed class SimpleEvent<S : State> : Event {
        data class StateUpdated<S : State>(val newState: S) : SimpleEvent<S>()
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
    override fun Content() {
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

    override fun onCleared() {
        super.onCleared()
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
 * Helper function to create a screen factory from a state screen
 */
public fun <S : State, A : Action, V : Event, F : Effect> FlowStateScreenDestination<S, A, V, F>.toFactory() =
    { storeInBackStack: Boolean, params: ScatterMap<String, String>?, extra: Extra? ->
        // Create a copy with the provided parameters
        this
    }

/**
 * Helper function to create a screen factory from a simple state screen
 */
public fun <S : State> SimpleStateScreenDestination<S>.toFactory() =
    { storeInBackStack: Boolean, params: ScatterMap<String, String>?, extra: Extra? ->
        // Create a copy with the provided parameters
        this
    }