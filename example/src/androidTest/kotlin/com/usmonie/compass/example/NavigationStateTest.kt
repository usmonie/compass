package com.usmonie.compass.example

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.usmonie.compass.screen.state.CompassSaveableState
import com.usmonie.compass.screen.state.navigation.ScreenId
import com.usmonie.compass.screen.state.stateEntry
import com.usmonie.compass.state.ViewModelStore
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class NavigationStateTest {

    @Before
    fun setUp() {
        ViewModelStore.clear()
    }

    @After
    fun tearDown() {
        ViewModelStore.clear()
    }

    // ── Core regression: re-navigating to a popped screen shows fresh state ──

    @Test
    fun popAndReNavigate_loginScreenShowsFreshState() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(CounterScreen)
            setContent { TestNavHost(backStack) }
            waitForIdle()

            // Push LoginScreen and enter email
            runOnUiThread { backStack.add(LoginScreen) }
            waitForIdle()

            onNodeWithTag("login_email_field").performTextReplacement("test@example.com")
            waitForIdle()
            onNodeWithTag("login_email_field").assertTextContains("test@example.com")

            // Pop LoginScreen
            runOnUiThread { backStack.removeLastOrNull() }
            waitForIdle()

            // Push LoginScreen again — must show EMPTY email (fresh ViewModel)
            runOnUiThread { backStack.add(LoginScreen) }
            waitForIdle()

            onNodeWithTag("login_email_field")
                .assert(emptyEditableText())
        }

    @Test
    fun popAndReNavigate_counterScreenShowsFreshState() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(CounterScreen)
            setContent { TestNavHost(backStack) }
            waitForIdle()

            // Increment counter twice
            onNodeWithTag("counter_increment_btn").performClick()
            onNodeWithTag("counter_increment_btn").performClick()
            waitForIdle()
            onNodeWithTag("counter_count_text").assertTextContains("2")

            // Navigate away and pop back — same screen instance stays, counter preserved
            runOnUiThread { backStack.add(LoginScreen) }
            waitForIdle()
            runOnUiThread { backStack.removeLastOrNull() }
            waitForIdle()

            // We popped to CounterScreen — its VM was NOT removed, count is still 2
            onNodeWithTag("counter_count_text").assertTextContains("2")

            // Now navigate away from CounterScreen entirely, then re-navigate
            runOnUiThread { backStack.add(LoginScreen) }
            waitForIdle()
            runOnUiThread {
                backStack.removeLastOrNull() // LoginScreen popped
                backStack.removeLastOrNull() // CounterScreen popped
                backStack.add(CounterScreen)
            }
            waitForIdle()

            // CounterScreen is new — counter must be back to 0
            onNodeWithTag("counter_count_text").assertTextContains("0")
        }

    // ── State preservation: screen remaining in backstack keeps its state ──

    @Test
    fun screenInBackstack_retainsState_whenCoveredByAnotherScreen() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(CounterScreen)
            setContent { TestNavHost(backStack) }
            waitForIdle()

            // Increment counter while on CounterScreen
            onNodeWithTag("counter_increment_btn").performClick()
            onNodeWithTag("counter_increment_btn").performClick()
            onNodeWithTag("counter_increment_btn").performClick()
            waitForIdle()
            onNodeWithTag("counter_count_text").assertTextContains("3")

            // Push LoginScreen on top (CounterScreen stays in backstack)
            runOnUiThread { backStack.add(LoginScreen) }
            waitForIdle()

            // Pop LoginScreen (return to CounterScreen)
            runOnUiThread { backStack.removeLastOrNull() }
            waitForIdle()

            // CounterScreen state is intact
            onNodeWithTag("counter_count_text").assertTextContains("3")
        }

    @Test
    fun typedText_onBackstackScreen_survivesRoundTrip() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(CounterScreen)
            setContent { TestNavHost(backStack) }
            waitForIdle()

            // Type into the counter's text field
            onNodeWithTag("counter_text_field").performTextReplacement("hello")
            waitForIdle()

            // Push and pop LoginScreen
            runOnUiThread { backStack.add(LoginScreen) }
            waitForIdle()
            runOnUiThread { backStack.removeLastOrNull() }
            waitForIdle()

            // Text still present on CounterScreen
            onNodeWithTag("counter_text_field").assertTextContains("hello")
        }

    // ── Multiple screens: deep stack pops clear all removed VMs ──

    @Test
    fun clearBackstack_thenReNavigate_allScreensFresh() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(CounterScreen)
            setContent { TestNavHost(backStack) }
            waitForIdle()

            // Build up state on CounterScreen
            onNodeWithTag("counter_increment_btn").performClick()
            onNodeWithTag("counter_increment_btn").performClick()
            waitForIdle()
            onNodeWithTag("counter_count_text").assertTextContains("2")

            // Push LoginScreen and type email
            runOnUiThread { backStack.add(LoginScreen) }
            waitForIdle()
            onNodeWithTag("login_email_field").performTextReplacement("stale@example.com")
            waitForIdle()

            // Clear entire stack and start fresh with CounterScreen
            runOnUiThread {
                backStack.clear()
                backStack.add(CounterScreen)
            }
            waitForIdle()

            // CounterScreen is brand new — count is 0
            onNodeWithTag("counter_count_text").assertTextContains("0")

            // Navigate back to LoginScreen — it must be fresh too
            runOnUiThread { backStack.add(LoginScreen) }
            waitForIdle()
            onNodeWithTag("login_email_field").assert(emptyEditableText())
        }

    // ── ViewModelStore sync: directly verifies lifecycle contract ──

    @Test
    fun compassSaveableState_disposesViewModel_whenScreenPopped() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(CounterScreen, LoginScreen)
            setContent { TestNavHost(backStack) }
            waitForIdle()

            // Capture the VM reference for LoginScreen before popping
            val loginVmBeforePop = ViewModelStore.getOrPut(LoginScreen) { error("VM must exist") }

            // Pop LoginScreen
            runOnUiThread { backStack.removeLastOrNull() }
            waitForIdle()

            // After pop, ViewModelStore must have cleared LoginScreen's entry.
            // Requesting it again should call the factory (fresh instance).
            var factoryCalled = false
            val vmAfterPop = ViewModelStore.getOrPut(LoginScreen) {
                factoryCalled = true
                error("We only check factory was called")
            }.let { factoryCalled }

            // If the sync worked, the old VM is gone and the factory would be invoked.
            // We verify indirectly: the UI shows empty email when we re-navigate.
            runOnUiThread { backStack.add(LoginScreen) }
            waitForIdle()
            onNodeWithTag("login_email_field").assert(emptyEditableText())
        }

    @Test
    fun viewModelStore_sync_doesNotAffect_activeScreens() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(CounterScreen)
            setContent { TestNavHost(backStack) }
            waitForIdle()

            onNodeWithTag("counter_increment_btn").performClick()
            onNodeWithTag("counter_increment_btn").performClick()
            waitForIdle()

            // Manually call sync with only CounterScreen active — VM should survive
            runOnUiThread { ViewModelStore.sync(listOf(CounterScreen)) }
            waitForIdle()

            onNodeWithTag("counter_count_text").assertTextContains("2")
        }

    // ── Navigation with replace ──

    @Test
    fun replaceNavigation_disposesReplacedScreenState() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(CounterScreen)
            setContent { TestNavHost(backStack) }
            waitForIdle()

            onNodeWithTag("counter_increment_btn").performClick()
            waitForIdle()
            onNodeWithTag("counter_count_text").assertTextContains("1")

            // Replace CounterScreen with LoginScreen (remove current top, push new)
            runOnUiThread {
                backStack.removeLastOrNull()
                backStack.add(LoginScreen)
            }
            waitForIdle()

            onNodeWithTag("login_email_field").assertExists()

            // Now replace LoginScreen back with CounterScreen — must be fresh
            runOnUiThread {
                backStack.removeLastOrNull()
                backStack.add(CounterScreen)
            }
            waitForIdle()

            onNodeWithTag("counter_count_text").assertTextContains("0")
        }

    // ── Composable helpers ──

    @Composable
    private fun TestNavHost(backStack: SnapshotStateList<ScreenId>) {
        MaterialTheme {
            CompassSaveableState(backStack.toList()) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                    entryProvider = entryProvider {
                        stateEntry<CounterScreen, CounterState, CounterAction, CounterEvent, CounterEffect> {
                            buildCounterScreen(onNavigate = { backStack.add(LoginScreen) })
                        }
                        stateEntry<LoginScreen, LoginState, LoginAction, LoginEvent, LoginEffect> {
                            buildLoginScreen(
                                onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                                onLoginSuccess = {},
                            )
                        }
                    },
                )
            }
        }
    }

    private fun emptyEditableText(): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
}
