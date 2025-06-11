package com.usmonie.compass.state

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StateViewModelTest {

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Test State
    data class TestState(val count: Int = 0) : State

    // Test Actions
    sealed class TestAction : Action {
        object Increment : TestAction()
        object Decrement : TestAction()
    }

    // Test Events
    sealed class TestEvent : Event {
        object Incremented : TestEvent()
        object Decremented : TestEvent()
    }

    // Test Effects
    sealed class TestEffect : Effect {
        object ShowToast : TestEffect()
    }

    @Test
    fun `should update state correctly when action is processed`() = runTest(testDispatcher) {
        val viewModel = createStateViewModel<TestState, TestAction, TestEvent, TestEffect>(
            initialState = TestState(0),
            processAction = { action, _ ->
                when (action) {
                    TestAction.Increment -> TestEvent.Incremented
                    TestAction.Decrement -> TestEvent.Decremented
                }
            },
            handleEvent = { event, state ->
                when (event) {
                    TestEvent.Incremented -> if (state.count >= 9) TestEffect.ShowToast else null
                    TestEvent.Decremented -> null
                }
            },
            reduce = { event ->
                when (event) {
                    TestEvent.Incremented -> copy(count = count + 1)
                    TestEvent.Decremented -> copy(count = count - 1)
                }
            }
        )

        // Initial state should be 0
        assertEquals(0, viewModel.state.value.count)

        // Handle increment action
        viewModel.handleAction(TestAction.Increment)

        assertEquals(1, viewModel.state.value.count)

        // Handle decrement action
        viewModel.handleAction(TestAction.Decrement)

        assertEquals(0, viewModel.state.value.count)

        viewModel.onDispose()
    }

    @Test
    fun `should work with FlowStateViewModel for multiple events`() = runTest(testDispatcher) {
        val viewModel = stateViewModel<TestState, TestAction, TestEvent, TestEffect>(
            initialState = TestState(0),
            processAction = { action, _ ->
                when (action) {
                    TestAction.Increment -> flowOf(TestEvent.Incremented, TestEvent.Incremented)
                    TestAction.Decrement -> flowOf(TestEvent.Decremented)
                }
            },
            handleEvent = { _, _ -> null },
            reduce = { event ->
                when (event) {
                    TestEvent.Incremented -> copy(count = count + 1)
                    TestEvent.Decremented -> copy(count = count - 1)
                }
            }
        )

        assertEquals(0, viewModel.state.value.count)

        // This should increment twice (because of flowOf(TestEvent.Incremented, TestEvent.Incremented))
        viewModel.handleAction(TestAction.Increment)

        assertEquals(2, viewModel.state.value.count)

        viewModel.onDispose()
    }
}

class ContentStateTest {

    @Test
    fun `should handle ContentState operations correctly`() {
        val successState: ContentState<String> = ContentState.Success("Hello")
        val errorState: ContentState<String> =
            ContentState.Error(object : ErrorState(RuntimeException("Test")) {})
        val loadingState: ContentState<String> = ContentState.Loading()

        // Test onSuccess
        var successCalled = false
        successState.onSuccess { data ->
            assertEquals("Hello", data)
            successCalled = true
        }
        assertEquals(true, successCalled)

        // Test onError
        var errorCalled = false
        errorState.onError<String, ErrorState> { error ->
            assertEquals("Test", error.message)
            errorCalled = true
        }
        assertEquals(true, errorCalled)

        // Test onLoading
        var loadingCalled = false
        loadingState.onLoading {
            loadingCalled = true
        }
        assertEquals(true, loadingCalled)

        // Test map
        val mappedSuccess = successState.map { it.uppercase() }
        assertEquals("HELLO", (mappedSuccess as ContentState.Success).data)

        // Test updateData
        val updatedSuccess = successState.updateData { "$it World" }
        assertEquals("Hello World", (updatedSuccess as ContentState.Success).data)
    }
}