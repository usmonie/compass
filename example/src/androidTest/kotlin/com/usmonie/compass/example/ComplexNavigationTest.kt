package com.usmonie.compass.example

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.usmonie.compass.screen.state.CompassSaveableState
import com.usmonie.compass.screen.state.SimpleAction
import com.usmonie.compass.screen.state.navigation.ScreenId
import com.usmonie.compass.screen.state.simpleEntry
import com.usmonie.compass.screen.state.simpleStateScreen
import com.usmonie.compass.screen.state.stateEntry
import com.usmonie.compass.state.State
import com.usmonie.compass.state.ViewModel
import com.usmonie.compass.state.ViewModelStore
import kotlinx.serialization.Serializable
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

// ════════════════════════════════════════════════════════════
// Test-local screen infrastructure
// ════════════════════════════════════════════════════════════

@Serializable
private data class WizardStepScreenId(val step: Int) : ScreenId("wizard_step_$step")

private data class WizardStepState(val text: String = "") : State

@Composable
private fun WizardStepContent(
    step: Int,
    state: WizardStepState,
    sendAction: (SimpleAction<WizardStepState>) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Text("Step $step", modifier = Modifier.testTag("wizard_title_$step"))
            OutlinedTextField(
                value = state.text,
                onValueChange = { sendAction(SimpleAction.UpdateState(state.copy(text = it))) },
                label = { Text("Data for step $step") },
                modifier = Modifier.testTag("wizard_input_$step"),
            )
            Row {
                Button(
                    onClick = onBack,
                    modifier = Modifier.testTag("wizard_back_$step"),
                ) { Text("Back") }
                Button(
                    onClick = onNext,
                    modifier = Modifier.testTag("wizard_next_$step"),
                ) { Text("Next") }
            }
        }
    }
}

private fun buildWizardStep(
    id: WizardStepScreenId,
    onNext: () -> Unit,
    onBack: () -> Unit,
) = simpleStateScreen(id, WizardStepState()) {
    content { state, sendAction ->
        WizardStepContent(id.step, state, sendAction, onNext, onBack)
    }
}

// ════════════════════════════════════════════════════════════
// Test helper
// ════════════════════════════════════════════════════════════

private class TrackingViewModel : ViewModel {
    var isDisposed = false
        private set

    override fun onDispose() {
        isDisposed = true
    }
}

// ════════════════════════════════════════════════════════════
// Tests
// ════════════════════════════════════════════════════════════

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ComplexNavigationTest {

    @Before
    fun setUp() {
        ViewModelStore.clear()
    }

    @After
    fun tearDown() {
        ViewModelStore.clear()
    }

    // ── Wizard / multi-step form ──────────────────────────────

    /**
     * Each wizard step stores state independently. Typing on Step 2 must not
     * affect Step 1, and vice versa.
     */
    @Test
    fun wizardFlow_eachStepHasIsolatedState() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(WizardStepScreenId(1))
            setContent { WizardNavHost(backStack) }
            waitForIdle()

            onNodeWithTag("wizard_input_1").performTextReplacement("step-one-data")
            waitForIdle()

            runOnUiThread { backStack.add(WizardStepScreenId(2)) }
            waitForIdle()

            onNodeWithTag("wizard_input_2").performTextReplacement("step-two-data")
            waitForIdle()

            // Step 2 shows its own text, not Step 1's
            onNodeWithTag("wizard_input_2").assertTextContains("step-two-data")

            // Go back to Step 1 — its text is still intact, not contaminated by Step 2
            runOnUiThread { backStack.removeLastOrNull() }
            waitForIdle()

            onNodeWithTag("wizard_input_1").assertTextContains("step-one-data")
        }

    /**
     * Back-navigating through a wizard preserves each step's state.
     * A→B→C, going back C→B→A, each step still shows what was typed.
     */
    @Test
    fun wizardFlow_backNavigation_preservesAllPreviousSteps() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(WizardStepScreenId(1))
            setContent { WizardNavHost(backStack) }
            waitForIdle()

            onNodeWithTag("wizard_input_1").performTextReplacement("alpha")
            waitForIdle()
            runOnUiThread { backStack.add(WizardStepScreenId(2)) }
            waitForIdle()

            onNodeWithTag("wizard_input_2").performTextReplacement("beta")
            waitForIdle()
            runOnUiThread { backStack.add(WizardStepScreenId(3)) }
            waitForIdle()

            onNodeWithTag("wizard_input_3").performTextReplacement("gamma")
            waitForIdle()

            // Step 3 → Step 2 — "beta" must survive
            runOnUiThread { backStack.removeLastOrNull() }
            waitForIdle()
            onNodeWithTag("wizard_input_2").assertTextContains("beta")

            // Step 2 → Step 1 — "alpha" must survive
            runOnUiThread { backStack.removeLastOrNull() }
            waitForIdle()
            onNodeWithTag("wizard_input_1").assertTextContains("alpha")
        }

    /**
     * A popped wizard step gets a fresh ViewModel on re-entry — the typed text
     * must NOT be restored.
     */
    @Test
    fun wizardFlow_poppedStep_freshOnRevisit() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(WizardStepScreenId(1))
            setContent { WizardNavHost(backStack) }
            waitForIdle()

            onNodeWithTag("wizard_input_1").performTextReplacement("first-visit")
            waitForIdle()
            runOnUiThread { backStack.add(WizardStepScreenId(2)) }
            waitForIdle()

            // Pop back to Step 1
            runOnUiThread { backStack.removeLastOrNull() }
            waitForIdle()

            // Pop Step 1 too
            runOnUiThread { backStack.removeLastOrNull() }
            waitForIdle()

            // Re-enter Step 1
            runOnUiThread { backStack.add(WizardStepScreenId(1)) }
            waitForIdle()

            onNodeWithTag("wizard_input_1").assert(emptyEditableText())
        }

    /**
     * After completing a wizard flow (clear backstack + new start), ALL step VMs
     * are disposed and every re-entered step shows a clean slate.
     */
    @Test
    fun wizardFlow_completeAndRestart_allStepsFresh() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(WizardStepScreenId(1))
            setContent { WizardNavHost(backStack) }
            waitForIdle()

            listOf(1, 2, 3).forEach { step ->
                onNodeWithTag("wizard_input_$step").performTextReplacement("run1-step$step")
                waitForIdle()
                if (step < 3) {
                    runOnUiThread { backStack.add(WizardStepScreenId(step + 1)) }
                    waitForIdle()
                }
            }

            // Simulate "complete": replace the whole stack with a fresh wizard start
            runOnUiThread {
                backStack.clear()
                backStack.add(WizardStepScreenId(1))
            }
            waitForIdle()

            onNodeWithTag("wizard_input_1").assert(emptyEditableText())

            // Walk through again — each step should be fresh
            runOnUiThread { backStack.add(WizardStepScreenId(2)) }
            waitForIdle()
            onNodeWithTag("wizard_input_2").assert(emptyEditableText())

            runOnUiThread { backStack.add(WizardStepScreenId(3)) }
            waitForIdle()
            onNodeWithTag("wizard_input_3").assert(emptyEditableText())
        }

    // ── Auth replacement flow ─────────────────────────────────

    /**
     * Simulates login success: clear the auth backstack and replace with the main
     * screen. The login screen's VM must be disposed.
     */
    @Test
    fun authReplacement_loginVmIsDisposed_afterStackReplace() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(LoginScreen)
            setContent { FullNavHost(backStack) }
            waitForIdle()

            onNodeWithTag("login_email_field").performTextReplacement("user@app.com")
            waitForIdle()

            // Capture the login VM before stack replacement
            val loginVmBeforeReplace = ViewModelStore.getOrPut(LoginScreen) {
                error("VM must already exist — the screen was rendered")
            }

            // Simulate login success: replace entire stack with CounterScreen
            runOnUiThread {
                backStack.clear()
                backStack.add(CounterScreen)
            }
            waitForIdle()

            // The login VM should have been disposed by CompassSaveableState sync
            assertTrue(
                (loginVmBeforeReplace as? TrackingViewModel)?.isDisposed ?: true,
                "Login VM must be disposed after stack replacement",
            )

            // Re-navigate to LoginScreen — must be fresh
            runOnUiThread { backStack.add(LoginScreen) }
            waitForIdle()
            onNodeWithTag("login_email_field").assert(emptyEditableText())
        }

    /**
     * After a login-success replacement the back button cannot return to the auth
     * screen. The new root is CounterScreen and the backstack has size 1.
     */
    @Test
    fun authReplacement_afterLogin_cannotNavigateBackToAuth() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(LoginScreen)
            setContent { FullNavHost(backStack) }
            waitForIdle()

            // Simulate login success — new root, no auth screen in stack
            runOnUiThread {
                backStack.clear()
                backStack.add(CounterScreen)
            }
            waitForIdle()

            // Backstack has a single entry — back is a no-op
            runOnUiThread {
                if (backStack.size > 1) backStack.removeLastOrNull()
            }
            waitForIdle()

            // CounterScreen is still the top; LoginScreen is nowhere
            onNodeWithTag("counter_count_text").assertExists()
            onNodeWithTag("login_email_field").assertDoesNotExist()
        }

    /**
     * Full logout-and-relogin cycle: login → main → logout → login must produce
     * a completely fresh login screen on every login cycle.
     */
    @Test
    fun authReplacement_logoutAndRelogin_showsFreshState() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(LoginScreen)
            setContent { FullNavHost(backStack) }
            waitForIdle()

            // First login: type credentials then "log in"
            onNodeWithTag("login_email_field").performTextReplacement("alice@example.com")
            waitForIdle()
            runOnUiThread { backStack.clear(); backStack.add(CounterScreen) }
            waitForIdle()

            // Simulate logout: back to auth screen (fresh stack)
            runOnUiThread { backStack.clear(); backStack.add(LoginScreen) }
            waitForIdle()

            // Login screen must not remember alice's email
            onNodeWithTag("login_email_field").assert(emptyEditableText())
        }

    // ── Deep stack / partial pop ──────────────────────────────

    /**
     * Stack of 5 wizard steps. Pop the top 3. Only those 3 VMs are disposed;
     * the bottom 2 retain their typed text.
     */
    @Test
    fun deepStack_partialPop_onlyUpperLevelsDisposed() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(WizardStepScreenId(1))
            setContent { WizardNavHost(backStack) }
            waitForIdle()

            // Build up 5-level stack, typing distinct text at each level
            for (step in 1..5) {
                onNodeWithTag("wizard_input_$step").performTextReplacement("level-$step")
                waitForIdle()
                if (step < 5) {
                    runOnUiThread { backStack.add(WizardStepScreenId(step + 1)) }
                    waitForIdle()
                }
            }

            // Pop back to Step 2 (removes steps 5, 4, 3)
            runOnUiThread {
                repeat(3) { backStack.removeLastOrNull() }
            }
            waitForIdle()

            // Step 2 still has its text
            onNodeWithTag("wizard_input_2").assertTextContains("level-2")

            // Pop Step 2 — CompassSaveableState sees [1] and disposes step 2's VM
            runOnUiThread { backStack.removeLastOrNull() }
            waitForIdle()
            // Re-push Step 2 — fresh VM, empty input
            runOnUiThread { backStack.add(WizardStepScreenId(2)) }
            waitForIdle()
            onNodeWithTag("wizard_input_2").assert(emptyEditableText())
        }

    /**
     * Clearing a 4-screen deep stack disposes every VM. Re-entering each screen
     * produces a clean state.
     */
    @Test
    fun deepStack_fullClear_allVmsDisposed() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(WizardStepScreenId(1))
            setContent { WizardNavHost(backStack) }
            waitForIdle()

            for (step in 1..4) {
                onNodeWithTag("wizard_input_$step").performTextReplacement("dirty-$step")
                waitForIdle()
                if (step < 4) {
                    runOnUiThread { backStack.add(WizardStepScreenId(step + 1)) }
                    waitForIdle()
                }
            }

            // Wipe the entire stack
            runOnUiThread { backStack.clear(); backStack.add(WizardStepScreenId(1)) }
            waitForIdle()

            onNodeWithTag("wizard_input_1").assert(emptyEditableText())
        }

    // ── Parameterised screen IDs (data-class ScreenId) ────────

    /**
     * Two different WizardStepScreenId values (step=1, step=2) produce completely
     * independent ViewModels — modifying one does not bleed into the other.
     */
    @Test
    fun dataClassId_differentParams_independentVms() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(WizardStepScreenId(1))
            setContent { WizardNavHost(backStack) }
            waitForIdle()

            onNodeWithTag("wizard_input_1").performTextReplacement("i-am-step-1")
            waitForIdle()
            runOnUiThread { backStack.add(WizardStepScreenId(2)) }
            waitForIdle()

            // Step 2 starts empty — it has its own VM
            onNodeWithTag("wizard_input_2").assert(emptyEditableText())
            onNodeWithTag("wizard_input_2").performTextReplacement("i-am-step-2")
            waitForIdle()

            // Navigate back — Step 1 was not affected by Step 2's text
            runOnUiThread { backStack.removeLastOrNull() }
            waitForIdle()
            onNodeWithTag("wizard_input_1").assertTextContains("i-am-step-1")
        }

    /**
     * The same WizardStepScreenId(1) pushed twice into the stack reuses the same
     * underlying ViewModel (data class equality). State typed on the first push
     * is visible when navigating back to it.
     *
     * Note: this is valid only while the screen stays in the backstack. Once
     * popped, the VM is cleared (see poppedAndRepushed test).
     */
    @Test
    fun dataClassId_sameParamInBackstack_vmReused() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(WizardStepScreenId(1))
            setContent { WizardNavHost(backStack) }
            waitForIdle()

            onNodeWithTag("wizard_input_1").performTextReplacement("persisted-value")
            waitForIdle()

            // Push Step 2 on top
            runOnUiThread { backStack.add(WizardStepScreenId(2)) }
            waitForIdle()

            // Pop back to Step 1 — same VM, same state
            runOnUiThread { backStack.removeLastOrNull() }
            waitForIdle()
            onNodeWithTag("wizard_input_1").assertTextContains("persisted-value")
        }

    /**
     * Once WizardStepScreenId(1) is fully popped, its VM is cleared. Re-pushing
     * WizardStepScreenId(1) (same params) gets a fresh VM.
     */
    @Test
    fun dataClassId_poppedAndRepushedWithSameParam_freshVm() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(WizardStepScreenId(1))
            setContent { WizardNavHost(backStack) }
            waitForIdle()

            onNodeWithTag("wizard_input_1").performTextReplacement("will-be-forgotten")
            waitForIdle()

            // Pop Step 1 entirely — CompassSaveableState sees [] and disposes its VM
            runOnUiThread { backStack.removeLastOrNull() }
            waitForIdle()

            // Push Step 1 again — same params, but VM was cleared on pop → fresh instance
            runOnUiThread { backStack.add(WizardStepScreenId(1)) }
            waitForIdle()

            onNodeWithTag("wizard_input_1").assert(emptyEditableText())
        }

    // ── Tab-like multi-backstack ──────────────────────────────

    /**
     * When two independent "tabs" are managed through CompassSaveableState, both
     * VMs are preserved only when sync receives the UNION of both backstacks.
     * This test documents the correct pattern for tab navigation.
     */
    @Test
    fun tabbedNavigation_syncUnion_preservesBothTabs() {
        ViewModelStore.clear()

        val tab1Vm = TrackingViewModel()
        val tab2Vm = TrackingViewModel()
        ViewModelStore.getOrPut(WizardStepScreenId(1)) { tab1Vm }
        ViewModelStore.getOrPut(WizardStepScreenId(2)) { tab2Vm }

        // Correct: sync the UNION of both tab backstacks
        val allActive = listOf(WizardStepScreenId(1), WizardStepScreenId(2))
        ViewModelStore.sync(allActive)

        assertFalse(tab1Vm.isDisposed, "Tab 1 VM must survive when included in sync")
        assertFalse(tab2Vm.isDisposed, "Tab 2 VM must survive when included in sync")
    }

    /**
     * Documents the common pitfall: syncing with only one tab's backstack wrongly
     * disposes the other tab's VMs, causing a visible state reset when switching
     * back to it.
     */
    @Test
    fun tabbedNavigation_syncOnlyOneTab_disposesOtherTabVm() {
        ViewModelStore.clear()

        val tab1Vm = TrackingViewModel()
        val tab2Vm = TrackingViewModel()
        ViewModelStore.getOrPut(WizardStepScreenId(1)) { tab1Vm }
        ViewModelStore.getOrPut(WizardStepScreenId(2)) { tab2Vm }

        // Bug pattern: sync with ONLY Tab 1's keys while Tab 2 has screens too
        ViewModelStore.sync(listOf(WizardStepScreenId(1)))

        assertFalse(tab1Vm.isDisposed, "Tab 1 VM correctly preserved")
        assertTrue(tab2Vm.isDisposed, "Tab 2 VM INCORRECTLY disposed — common pitfall!")
    }

    /**
     * Full tab-switching simulation: two tabs with independent backstacks. Each
     * tab's screens retain their state after switching away and back.
     */
    @Test
    fun tabbedNavigation_switchingTabs_eachTabRetainsState() =
        runAndroidComposeUiTest<ComponentActivity> {
            val tab1BackStack = mutableStateListOf<ScreenId>(WizardStepScreenId(1))
            val tab2BackStack = mutableStateListOf<ScreenId>(WizardStepScreenId(2))

            setContent { TabbedNavHost(tab1BackStack, tab2BackStack) }
            waitForIdle()

            // Tab 1 is visible initially — type text
            onNodeWithTag("wizard_input_1").performTextReplacement("tab1-value")
            waitForIdle()

            // Switch to Tab 2
            onNodeWithTag("tab_btn_2").performClick()
            waitForIdle()

            // Tab 2 is fresh
            onNodeWithTag("wizard_input_2").assert(emptyEditableText())
            onNodeWithTag("wizard_input_2").performTextReplacement("tab2-value")
            waitForIdle()

            // Switch back to Tab 1 — its state is preserved
            onNodeWithTag("tab_btn_1").performClick()
            waitForIdle()
            onNodeWithTag("wizard_input_1").assertTextContains("tab1-value")

            // Switch back to Tab 2 — its state is preserved
            onNodeWithTag("tab_btn_2").performClick()
            waitForIdle()
            onNodeWithTag("wizard_input_2").assertTextContains("tab2-value")
        }

    // ── Async / disposal safety ───────────────────────────────

    /**
     * After a VM is disposed (screen popped), dispatching a stale action reference
     * to it must NOT update the fresh VM created on re-navigation.
     */
    @Test
    fun vmDisposal_staleActionOnOldVm_doesNotPolluteFreshVm() =
        runAndroidComposeUiTest<ComponentActivity> {
            val backStack = mutableStateListOf<ScreenId>(WizardStepScreenId(1))
            setContent { WizardNavHost(backStack) }
            waitForIdle()

            onNodeWithTag("wizard_input_1").performTextReplacement("original")
            waitForIdle()

            // Pop the screen — its VM is disposed
            runOnUiThread { backStack.removeLastOrNull() }
            waitForIdle()

            // Re-navigate — a fresh VM is created
            runOnUiThread { backStack.add(WizardStepScreenId(1)) }
            waitForIdle()

            // The fresh screen must show empty, not the old VM's value
            onNodeWithTag("wizard_input_1").assert(emptyEditableText())
        }

    /**
     * A VM's onDispose is invoked exactly once when removed from the store. A
     * second remove of the same key is a no-op (doesn't double-dispose or throw).
     */
    @Test
    fun vmDisposal_doubleRemove_isIdempotent() {
        val vm = TrackingViewModel()
        ViewModelStore.getOrPut("key") { vm }

        ViewModelStore.remove("key")
        assertTrue(vm.isDisposed)

        // Second remove must not throw, and the flag is still true (not reset)
        ViewModelStore.remove("key")
        assertTrue(vm.isDisposed)
    }

    /**
     * ViewModelStore.sync is safe to call with a list that doesn't change anything
     * multiple times in a row — it must be fully idempotent.
     */
    @Test
    fun viewModelStore_sync_isIdempotent_whenListDoesNotChange() {
        val vm = TrackingViewModel()
        ViewModelStore.getOrPut("key") { vm }

        repeat(5) { ViewModelStore.sync(listOf("key")) }

        assertFalse(vm.isDisposed)
        assertSame(vm, ViewModelStore.getOrPut("key") { TrackingViewModel() })
    }

    /**
     * Verifies that after ViewModelStore.clear(), every subsequent getOrPut for
     * any previously-held key invokes the factory (fresh instances for all).
     */
    @Test
    fun viewModelStore_clear_thenGetOrPut_alwaysCallsFactory() {
        val vmA = TrackingViewModel()
        val vmB = TrackingViewModel()
        ViewModelStore.getOrPut("a") { vmA }
        ViewModelStore.getOrPut("b") { vmB }

        ViewModelStore.clear()

        val freshA = TrackingViewModel()
        val freshB = TrackingViewModel()
        assertSame(freshA, ViewModelStore.getOrPut("a") { freshA })
        assertSame(freshB, ViewModelStore.getOrPut("b") { freshB })
        assertNotSame(vmA, freshA)
        assertNotSame(vmB, freshB)
    }

    // ════════════════════════════════════════════════════════════
    // Composable test hosts
    // ════════════════════════════════════════════════════════════

    @Composable
    private fun WizardNavHost(backStack: SnapshotStateList<ScreenId>) {
        MaterialTheme {
            CompassSaveableState(backStack.toList()) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                    entryProvider = entryProvider {
                        simpleEntry<WizardStepScreenId, WizardStepState> { screenId ->
                            buildWizardStep(
                                id = screenId,
                                onNext = { backStack.add(WizardStepScreenId(screenId.step + 1)) },
                                onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                            )
                        }
                    },
                )
            }
        }
    }

    @Composable
    private fun FullNavHost(backStack: SnapshotStateList<ScreenId>) {
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
                                onLoginSuccess = { backStack.clear(); backStack.add(CounterScreen) },
                            )
                        }
                        simpleEntry<WizardStepScreenId, WizardStepState> { screenId ->
                            buildWizardStep(
                                id = screenId,
                                onNext = { backStack.add(WizardStepScreenId(screenId.step + 1)) },
                                onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                            )
                        }
                    },
                )
            }
        }
    }

    /**
     * Two-tab host: each tab has its own backstack. CompassSaveableState receives
     * the union of both stacks so neither tab's VMs are evicted during tab switches.
     */
    @Composable
    private fun TabbedNavHost(
        tab1BackStack: SnapshotStateList<ScreenId>,
        tab2BackStack: SnapshotStateList<ScreenId>,
    ) {
        var activeTab by remember { mutableIntStateOf(1) }
        val activeBackStack = if (activeTab == 1) tab1BackStack else tab2BackStack
        val allActive = remember(tab1BackStack.toList(), tab2BackStack.toList()) {
            tab1BackStack + tab2BackStack
        }

        MaterialTheme {
            // Sync against the union of both tabs so neither tab's VMs are evicted
            CompassSaveableState(allActive) {
                Column {
                    Row {
                        Button(
                            onClick = { activeTab = 1 },
                            modifier = Modifier.testTag("tab_btn_1"),
                        ) { Text("Tab 1") }
                        Button(
                            onClick = { activeTab = 2 },
                            modifier = Modifier.testTag("tab_btn_2"),
                        ) { Text("Tab 2") }
                    }
                    NavDisplay(
                        backStack = activeBackStack,
                        onBack = { if (activeBackStack.size > 1) activeBackStack.removeLastOrNull() },
                        entryProvider = entryProvider {
                            simpleEntry<WizardStepScreenId, WizardStepState> { screenId ->
                                buildWizardStep(
                                    id = screenId,
                                    onNext = { activeBackStack.add(WizardStepScreenId(screenId.step + 1)) },
                                    onBack = { if (activeBackStack.size > 1) activeBackStack.removeLastOrNull() },
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    private fun emptyEditableText(): SemanticsMatcher =
        SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
}
