package com.usmonie.compass.component.state

import androidx.compose.runtime.Composable
import com.usmonie.compass.state.Action
import com.usmonie.compass.state.ActionProcessor
import com.usmonie.compass.state.Effect
import com.usmonie.compass.state.Event
import com.usmonie.compass.state.EventHandler
import com.usmonie.compass.state.State
import com.usmonie.compass.state.StateManager
import com.usmonie.compass.state.StateViewModel
import com.usmonie.compass.state.SubscriptionKey
import com.usmonie.compass.state.createStateViewModel
import kotlinx.coroutines.CoroutineScope

/**
 * State component builder with more customization options
 */
public class StateComponentBuilder<P, S : State, A : Action, V : Event, F : Effect> {
    private var viewModel: StateViewModel<S, A, V, F>? = null
    private var initialStateProvider: (() -> S)? = null
    private var processAction: (suspend CoroutineScope.(
        action: A,
        state: S,
        emit: suspend (V) -> Unit,
        launchFlow: suspend (key: SubscriptionKey, block: suspend CoroutineScope.() -> Unit) -> Unit,
    ) -> Unit)? = null

    private var handleEvent: ((V, S) -> F?)? = null
    private var reduce: (S.(V) -> S)? = null
    private var content: (@Composable (P, S, (A) -> Unit) -> Unit)? = null
    private var onEffect: (@Composable (S, F?) -> Unit)? = null
    private var init: (StateViewModel<S, A, V, F>.() -> Unit)? = null

    public fun viewModel(viewModel: () -> StateViewModel<S, A, V, F>) {
        this.viewModel = viewModel()
    }

    public fun initialStateProvider(provider: () -> S) {
        initialStateProvider = provider
    }

    public fun processAction(
        processor: suspend CoroutineScope.(
            A,
            S,
            emit: suspend (V) -> Unit,
            launchFlow: suspend (key: SubscriptionKey, block: suspend CoroutineScope.() -> Unit) -> Unit,
        ) -> Unit,
    ) {
        processAction = processor
    }

    public fun processAction(actionProcessor: ActionProcessor<A, S, V>) {
        processAction = { scope, action, state, emit, launchFlow ->
            actionProcessor.process(scope, action, state, emit, launchFlow)
        }
    }

    public fun handleEvent(handler: (V, S) -> F?) {
        handleEvent = handler
    }

    public fun handleEvent(handler: EventHandler<V, S, F>) {
        handleEvent = { event, state ->
            handler.handle(event, state)
        }
    }

    public fun reduce(reducer: S.(V) -> S) {
        reduce = reducer
    }

    public fun reduce(manager: StateManager<S, V>) {
        reduce = { event ->
            manager.reduce(this, event)
        }
    }

    public fun content(composable: @Composable (P, S, (A) -> Unit) -> Unit) {
        content = composable
    }

    public fun onEffect(handler: @Composable (S, F?) -> Unit) {
        onEffect = handler
    }

    public fun init(initializer: StateViewModel<S, A, V, F>.() -> Unit) {
        init = initializer
    }

    public fun build(): StateComponentDefinition<P, S, A, V, F> {
        return StateComponentDefinition(
            viewModel = viewModel ?: createStateViewModel(
                initialState = requireNotNull(initialStateProvider) { "Initial state provider must be provided" }(),
                processAction = requireNotNull(processAction) { "Action processor must be provided" },
                handleEvent = requireNotNull(handleEvent) { "Event handler must be provided" },
                reduce = requireNotNull(reduce) { "State reducer must be provided" },
                init = init ?: {},
            ),
            content = requireNotNull(content) { "Content composable must be provided" },
            onEffect = onEffect ?: { _, _ -> },
        )
    }
}

/**
 * State component definition with lazy initialization and customization
 */
public class StateComponentDefinition<P, S : State, A : Action, V : Event, F : Effect>(
    private val viewModel: StateViewModel<S, A, V, F>,
    private val content: @Composable (P, S, (A) -> Unit) -> Unit,
    private val onEffect: @Composable (S, F?) -> Unit,
) {
    /**
     * Creates a Composable component instance with lazy state initialization
     */
    @Composable
    public fun Component(params: P) {
        StateContent(
            viewModel = viewModel,
            onEffect = onEffect,
            content = { state, onAction -> content(params, state, onAction) },
        )
    }

    /**
     * Creates a Composable component instance with custom parameters
     */
    @Composable
    public fun Component(
        params: P,
        customInitialStateProvider: (() -> S)? = null,
        customOnEffect: (@Composable (S, F?) -> Unit)? = null,
    ) {
        StateContent(
            viewModel = viewModel,
            onEffect = customOnEffect ?: onEffect,
            content = { state, onAction -> content(params, state, onAction) },
        )
    }
}

/**
 * DSL function to create an advanced reusable state component
 */
public inline fun <P, S : State, A : Action, V : Event, F : Effect> stateComponent(
    builder: StateComponentBuilder<P, S, A, V, F>.() -> Unit,
): StateComponentDefinition<P, S, A, V, F> {
    val componentBuilder = StateComponentBuilder<P, S, A, V, F>()
    componentBuilder.apply(builder)
    return componentBuilder.build()
}

/**
 * DSL функция с опциональными параметрами
 */
public inline fun <P, S : State, A : Action, V : Event, F : Effect> stateComponent(
    actionProcessor: ActionProcessor<A, S, V>? = null,
    eventHandler: EventHandler<V, S, F>? = null,
    stateManager: StateManager<S, V>? = null,
    builder: StateComponentBuilder<P, S, A, V, F>.() -> Unit,
): StateComponentDefinition<P, S, A, V, F> {
    val screenBuilder = StateComponentBuilder<P, S, A, V, F>()

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
    return screenBuilder.build()
}