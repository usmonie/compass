@file:OptIn(ExperimentalCoroutinesApi::class)
@file:Suppress("IllegalIdentifier")

import com.usmonie.compass.screen.state.stateScreen
import com.usmonie.compass.state.Action
import com.usmonie.compass.state.Effect
import com.usmonie.compass.state.Event
import com.usmonie.compass.state.State
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NavigationFlowTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun stateScreenCreatesDestinationWithCorrectViewModel() = runTest {
        val screenId = TestScreenId("profile")
        val destination = stateScreen<TestScreenId, TestState, TestAction, TestEvent, TestEffect>(screenId) {
            initialState(TestState(count = 0))
            processAction { _, action ->
                when (action) {
                    TestAction.Increment -> TestEvent.Incremented
                    else -> TestEvent.Incremented
                }
            }
            handleEvent { _, _ -> TestEffect.ShowMessage }
            reduce { when (it) { is TestEvent.Incremented -> copy(count = count + 1) } }
            content { _, _ ->

            }
        }

        assertNotNull(destination)
        assertEquals(screenId, destination.key)
        assertTrue(destination.storeInBackStack)

        // Trigger action
        destination.viewModel.handleAction(TestAction.Increment)
        advanceUntilIdle()

        assertEquals(1, destination.viewModel.state.value.count)

        // Collect effect
        val effects = mutableListOf<TestEffect>()
        val job = launch {
            destination.viewModel.effect.toList(effects)
        }
        advanceUntilIdle()
        assertEquals(1, effects.size)
        assertEquals(TestEffect.ShowMessage, effects[0])
        job.cancel()
    }

    @Test
    fun checkStoreInBackStackFlagIsRespected() {
        val screenId = TestScreenId("modal")
        val destination = stateScreen<TestScreenId, TestState, TestAction, TestEvent, TestEffect>(screenId, storeInBackStack = false) {
            initialState(TestState(0))
            processAction { _, _, -> TestEvent.Incremented }
            handleEvent { _, _ -> null }
            reduce { copy(count = count + 1) }
            content { _, _ -> }
        }

        assertFalse(destination.storeInBackStack)
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
