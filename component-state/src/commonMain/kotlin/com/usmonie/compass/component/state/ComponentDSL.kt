package com.usmonie.compass.component.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.usmonie.compass.state.State
import com.usmonie.compass.state.Action
import com.usmonie.compass.state.Effect
import com.usmonie.compass.state.Event
import com.usmonie.compass.state.StateViewModel
import com.usmonie.compass.state.createStateViewModel
import kotlinx.coroutines.CoroutineScope

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
            viewModel = viewModel ?: createStateViewModel(
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
