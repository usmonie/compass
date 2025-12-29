package com.usmonie.compass.screen.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.usmonie.compass.component.state.StateContent
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
import com.usmonie.compass.state.SubscriptionKey
import com.usmonie.compass.state.createStateViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * DSL for creating a complete state-managed screen with minimal boilerplate
 */
public class StateScreenBuilder<K : ScreenId, S : State, A : Action, V : Event, F : Effect> {
    private var initialState: S? = null
    private var processAction: (suspend CoroutineScope.(
        action: A,
        state: S,
        emit: suspend (V) -> Unit,
        launchFlow: suspend (key: SubscriptionKey, block: suspend CoroutineScope.() -> Unit) -> Unit,
    ) -> Unit)? = null
    private var handleEvent: ((V, S) -> F?)? = null
    private var reduce: (S.(V) -> S)? = null
    private var content: (@Composable (S, (A) -> Unit) -> Unit)? = null
    private var onEffect: (@Composable (S, F?) -> Unit)? = null

    public fun initialState(state: S) {
        initialState = state
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

    public fun content(composable: @Composable (S, (A) -> Unit) -> Unit) {
        content = composable
    }

    public fun onEffect(handler: @Composable (S, F?) -> Unit) {
        onEffect = handler
    }

    public fun build(
        screenId: K,
        storeInBackStack: Boolean,
    ): StateScreenDestination<K, S, A, V, F> {
        return StateScreenDestination(
            id = screenId,
            storeInBackStack = storeInBackStack,
            viewModel = createStateViewModel(
                initialState = requireNotNull(initialState) { "Initial state must be provided" },
                processAction = requireNotNull(processAction) { "Action processor must be provided" },
                handleEvent = requireNotNull(handleEvent) { "Event handler must be provided" },
                reduce = requireNotNull(reduce) { "State reducer must be provided" }
            ),
            content = requireNotNull(content) { "Content composable must be provided" },
            onEffect = onEffect ?: { _, _ -> }
        )
    }
}

/**
 * Screen destination that integrates with state management
 */
@Immutable
public class StateScreenDestination<K : ScreenId, S : State, A : Action, V : Event, F : Effect>(
    id: K,
    storeInBackStack: Boolean,
    internal val viewModel: StateViewModel<S, A, V, F>,
    private val content: @Composable (S, (A) -> Unit) -> Unit,
    private val onEffect: @Composable (S, F?) -> Unit,
) : ScreenDestination<K>(id, storeInBackStack) {

    @Composable
    override fun Content() {
        StateContent(
            viewModel = viewModel,
            onEffect = onEffect,
            content = content
        )
    }
}


/**
 * Creates a [Saver] for any class annotated with @Serializable.
 *
 * This enables the state to be used with `rememberSaveable`.
 *
 * Usage:
 *   val myState = rememberSaveable(stateSaver = serializableSaver<MyState>()) { MyState() }
 */
internal inline fun <reified T : Any> serializableSaver(
    json: Json = Json,
): Saver<T, Any> {
    val serializer: KSerializer<T> = serializer()
    return listSaver(
        save = { value ->
            listOf(json.encodeToString(serializer, value))
        },
        restore = { list ->
            list.firstOrNull()?.let { json.decodeFromString(serializer, it) }
        }
    )
}