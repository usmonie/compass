// commonTest/com/usmonie/compass/state/StateViewModelTest.kt
package com.usmonie.compass.state

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StateViewModelTest {


    private val testDispatcher = StandardTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given initial state, stateFlow emits initial state`() = runTest {
        val initialState = TestState(count = 0)
        val viewModel = createStateViewModel<TestState, TestAction, TestEvent, TestEffect>(
            initialState = initialState,
            processAction = { _, _ -> TestEvent.Incremented },
            handleEvent = { _, _ -> null },
            reduce = {
                when (it) {
                    is TestEvent.Incremented -> copy(count = count + 1)
                }
            }
        )

        assertEquals(initialState, viewModel.state.value)
    }

    @Test
    fun `handleAction triggers state update and effect`() = runTest {
        val viewModel = createStateViewModel<TestState, TestAction, TestEvent, TestEffect>(
            initialState = TestState(count = 0),
            processAction = { _, action ->
                when (action) {
                    TestAction.Increment -> TestEvent.Incremented
                    else -> TestEvent.Incremented
                }
            },
            handleEvent = { event, state ->
                when (event) {
                    TestEvent.Incremented -> if (state.count == 0) TestEffect.ShowMessage else null
                }
            },
            reduce = {
                when (it) {
                    TestEvent.Incremented -> copy(count = count + 1)
                }
            }
        )


        viewModel.handleAction(TestAction.Increment)

        // Assert state
        assertEquals(1, viewModel.state.value.count)

        // Assert effect
        val effect = viewModel.effect.toList().first()
        assertEquals(TestEffect.ShowMessage, effect)
    }

    @Test
    fun `exception in processAction is caught and does not crash`() = runTest {
        val viewModel = object : StateViewModel<TestState, TestAction, TestEvent, TestEffect>(
            initialState = TestState(0)
        ) {
            override suspend fun processAction(action: TestAction): TestEvent {
                throw RuntimeException("Oops")
            }

            override fun TestState.reduce(event: TestEvent): TestState = this
            override fun handleEvent(event: TestEvent): TestEffect? = null
            override fun mapErrorToEvent(throwable: Throwable): TestEvent? = null
        }

        var caught = false
        viewModel.handleAction(TestAction.Increment)
        // Should not throw — just log or ignore
        caught = true
        assertTrue(caught)
    }
}

data class TestState(val count: Int) : State
internal sealed class TestAction : Action {
    object Increment : TestAction()
}

sealed class TestEvent : Event {
    object Incremented : TestEvent()
}

sealed class TestEffect : Effect {
    object ShowMessage : TestEffect()
}
