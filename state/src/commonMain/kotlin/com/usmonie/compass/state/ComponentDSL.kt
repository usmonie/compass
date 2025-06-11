package com.usmonie.compass.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * Simple state component DSL for components that only need basic state management
 */
public class SimpleStateComponentBuilder<S : State> {
    private var initialState: S? = null
    private var content: (@Composable (S, (S) -> Unit) -> Unit)? = null

    public fun initialState(state: S) {
        initialState = state
    }

    public fun content(composable: @Composable (S, (S) -> Unit) -> Unit) {
        content = composable
    }

    public fun build(): SimpleStateComponentDefinition<S> {
        return SimpleStateComponentDefinition(
            initialState = requireNotNull(initialState) { "Initial state must be provided" },
            content = requireNotNull(content) { "Content composable must be provided" }
        )
    }
}

/**
 * Simple state component definition for basic state management
 */
public class SimpleStateComponentDefinition<S : State>(
    private val initialState: S,
    private val content: @Composable (S, (S) -> Unit) -> Unit
) {
    // Simple actions and events for basic state updates
    private sealed class SimpleAction<S : State> : Action {
        data class UpdateState<S : State>(val newState: S) : SimpleAction<S>()
    }

    private sealed class SimpleEvent<S : State> : Event {
        data class StateUpdated<S : State>(val newState: S) : SimpleEvent<S>()
    }

    /**
     * Creates a Composable component instance
     */
    @Composable
    public fun Component() {
        val viewModel = remember {
            createStateViewModel<S, SimpleAction<S>, SimpleEvent<S>, Nothing>(
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
        }

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

    /**
     * Creates a Composable component instance with custom initial state
     */
    @Composable
    public fun Component(customInitialState: S) {
        val viewModel = remember(customInitialState) {
            createStateViewModel<S, SimpleAction<S>, SimpleEvent<S>, Nothing>(
                initialState = customInitialState,
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
        }

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
}

/**
 * DSL function to create a simple reusable state component
 */
public inline fun <S : State> simpleStateComponent(
    builder: SimpleStateComponentBuilder<S>.() -> Unit
): SimpleStateComponentDefinition<S> {
    val componentBuilder = SimpleStateComponentBuilder<S>()
    componentBuilder.apply(builder)
    return componentBuilder.build()
}

/**
 * State component builder with more customization options
 */
public class StateComponentBuilder<S : State, A : Action, V : Event, F : Effect> {
    private var viewModel: StateViewModel<S, A, V, F>? = null
    private var initialStateProvider: (() -> S)? = null
    private var processAction: (suspend CoroutineScope.(A, S) -> V)? = null
    private var handleEvent: ((V, S) -> F?)? = null
    private var reduce: (S.(V) -> S)? = null
    private var content: (@Composable (S, (A) -> Unit) -> Unit)? = null
    private var onEffect: (suspend (F) -> Unit)? = null
    private var init: (StateViewModel<S, A, V, F>.() -> Unit)? = null

    public fun initialStateProvider(provider: () -> S) {
        initialStateProvider = provider
    }

    public fun processAction(processor: suspend CoroutineScope.(A, S) -> V) {
        processAction = processor
    }

    public fun viewModel(viewModel: () -> StateViewModel<S, A, V, F>) {
        this.viewModel = viewModel()
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

    public fun init(initializer: StateViewModel<S, A, V, F>.() -> Unit) {
        init = initializer
    }

    public fun build(): StateComponentDefinition<S, A, V, F> {
        return StateComponentDefinition(
            viewModel = viewModel ?: stateViewModel(
                initialState = requireNotNull(initialStateProvider) { "Initial state provider must be provided" }(),
                processAction = requireNotNull(processAction) { "Action processor must be provided" },
                handleEvent = requireNotNull(handleEvent) { "Event handler must be provided" },
                reduce = requireNotNull(reduce) { "State reducer must be provided" },
                init = init ?: {},
            ),
            content = requireNotNull(content) { "Content composable must be provided" },
            onEffect = onEffect ?: {},
        )
    }
}

/**
 * State component definition with lazy initialization and customization
 */
public class StateComponentDefinition<S : State, A : Action, V : Event, F : Effect>(
    private val viewModel: StateViewModel<S, A, V, F>,
    private val content: @Composable (S, (A) -> Unit) -> Unit,
    private val onEffect: suspend (F) -> Unit,
) {
    /**
     * Creates a Composable component instance with lazy state initialization
     */
    @Composable
    public fun Component() {
        StateContent(
            viewModel = viewModel,
            onEffect = onEffect,
            content = content
        )
    }

    /**
     * Creates a Composable component instance with custom parameters
     */
    @Composable
    public fun Component(
        customInitialStateProvider: (() -> S)? = null,
        customOnEffect: (suspend (F) -> Unit)? = null
    ) {
        StateContent(
            viewModel = viewModel,
            onEffect = customOnEffect ?: onEffect,
            content = content
        )
    }
}

/**
 * DSL function to create an advanced reusable state component
 */
public inline fun <S : State, A : Action, V : Event, F : Effect> stateComponent(
    builder: StateComponentBuilder<S, A, V, F>.() -> Unit
): StateComponentDefinition<S, A, V, F> {
    val componentBuilder = StateComponentBuilder<S, A, V, F>()
    componentBuilder.apply(builder)
    return componentBuilder.build()
}

/**
 * State component builder with more customization options
 */
public class FlowStateComponentBuilder<S : State, A : Action, V : Event, F : Effect> {
    private var viewModel: FlowStateViewModel<S, A, V, F>? = null
    private var initialStateProvider: (() -> S)? = null
    private var processAction: (suspend CoroutineScope.(A, S) -> Flow<V>)? = null
    private var handleEvent: ((V, S) -> F?)? = null
    private var reduce: (S.(V) -> S)? = null
    private var content: (@Composable (S, (A) -> Unit) -> Unit)? = null
    private var onEffect: (suspend (F) -> Unit)? = null
    private var init: (FlowStateViewModel<S, A, V, F>.() -> Unit)? = null

    public fun initialStateProvider(provider: () -> S) {
        initialStateProvider = provider
    }

    public fun processAction(processor: suspend CoroutineScope.(A, S) -> Flow<V>) {
        processAction = processor
    }

    public fun viewModel(viewModel: () -> FlowStateViewModel<S, A, V, F>) {
        this.viewModel = viewModel()
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

    public fun init(initializer: FlowStateViewModel<S, A, V, F>.() -> Unit) {
        init = initializer
    }

    public fun build(): FlowStateComponentDefinition<S, A, V, F> {
        return FlowStateComponentDefinition(
            viewModel = viewModel ?: flowStateViewModel(
                initialState = requireNotNull(initialStateProvider) { "Initial state provider must be provided" }(),
                processAction = requireNotNull(processAction) { "Action processor must be provided" },
                handleEvent = requireNotNull(handleEvent) { "Event handler must be provided" },
                reduce = requireNotNull(reduce) { "State reducer must be provided" },
                init = init ?: {},
            ),

            content = requireNotNull(content) { "Content composable must be provided" },
            onEffect = onEffect ?: {},
        )
    }
}

/**
 * State component definition with lazy initialization and customization
 */
public class FlowStateComponentDefinition<S : State, A : Action, V : Event, F : Effect>(
    private val viewModel: FlowStateViewModel<S, A, V, F>,
    private val content: @Composable (S, (A) -> Unit) -> Unit,
    private val onEffect: suspend (F) -> Unit,
) {
    /**
     * Creates a Composable component instance with lazy state initialization
     */
    @Composable
    public fun Component() {

        StateContent(
            viewModel = viewModel,
            onEffect = onEffect,
            content = content
        )
    }

    /**
     * Creates a Composable component instance with custom parameters
     */
    @Composable
    public fun Component(
        customInitialStateProvider: (() -> S)? = null,
        customOnEffect: (suspend (F) -> Unit)? = null
    ) {
        StateContent(
            viewModel = viewModel,
            onEffect = customOnEffect ?: onEffect,
            content = content
        )
    }
}

/**
 * DSL function to create an advanced reusable state component
 */
public inline fun <S : State, A : Action, V : Event, F : Effect> flowStateComponent(
    builder: StateComponentBuilder<S, A, V, F>.() -> Unit
): StateComponentDefinition<S, A, V, F> {
    val componentBuilder = StateComponentBuilder<S, A, V, F>()
    componentBuilder.apply(builder)
    return componentBuilder.build()
}