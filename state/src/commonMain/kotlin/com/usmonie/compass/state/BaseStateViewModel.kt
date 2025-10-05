package com.usmonie.compass.state

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

    override suspend fun processAction(action: A): V =
        actionProcessor.process(viewModelScope, action, state.value)

    override fun handleEvent(event: V): F? = eventHandler.handle(event, state.value)

    override fun S.reduce(event: V): S = stateManager.reduce(this, event)
}
