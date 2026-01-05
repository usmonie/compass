package com.usmonie.compass.state

import kotlinx.coroutines.CoroutineScope

public open class BaseStateViewModel<S : State, in A : Action, V : Event, out F : Effect>(
    initialState: S,
    private val stateManager: StateManager<S, V>,
    private val actionProcessor: ActionProcessor<A, S, V>,
    private val eventHandler: EventHandler<V, S, F>,
    init: () -> Unit = {},
) : StateViewModel<S, A, V, F>(initialState) {

    init {
        init()
    }

    override fun handleEvent(event: V): F? = eventHandler.handle(event, state.value)

    override fun S.reduce(event: V): S = stateManager.reduce(this, event)

    override suspend fun processAction(
        action: A,
        state: S,
        emit: suspend (V) -> Unit,
        launchFlow: suspend (key: SubscriptionKey, block: suspend CoroutineScope.() -> Unit) -> Unit,
    ): Unit = actionProcessor.process(viewModelScope, action, state, emit, launchFlow)
}
