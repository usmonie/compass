package com.usmonie.compass.state

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ViewModelStoreTest {

    @BeforeTest
    fun setUp() {
        ViewModelStore.clear()
    }

    @AfterTest
    fun tearDown() {
        ViewModelStore.clear()
    }

    @Test
    fun getOrPut_returnsSameInstance_forSameKey() {
        val key = "screen_a"
        val vm1 = ViewModelStore.getOrPut(key) { TrackingViewModel() }
        val vm2 = ViewModelStore.getOrPut(key) { TrackingViewModel() }
        assertSame(vm1, vm2)
    }

    @Test
    fun getOrPut_createsNewInstance_afterRemove() {
        val key = "screen_a"
        val vm1 = ViewModelStore.getOrPut(key) { TrackingViewModel() }
        ViewModelStore.remove(key)
        val vm2 = ViewModelStore.getOrPut(key) { TrackingViewModel() }
        assertNotSame(vm1, vm2)
    }

    @Test
    fun remove_callsOnDispose_onRemovedViewModel() {
        val key = "screen_a"
        val vm = TrackingViewModel()
        ViewModelStore.getOrPut(key) { vm }

        assertFalse(vm.isDisposed)
        ViewModelStore.remove(key)
        assertTrue(vm.isDisposed)
    }

    @Test
    fun remove_unknownKey_doesNotThrow() {
        ViewModelStore.remove("nonexistent_key")
    }

    @Test
    fun sync_removesViewModels_notInActiveKeys() {
        val keyA = "screen_a"
        val keyB = "screen_b"
        ViewModelStore.getOrPut(keyA) { TrackingViewModel() }
        val vmB = TrackingViewModel()
        ViewModelStore.getOrPut(keyB) { vmB }

        ViewModelStore.sync(listOf(keyA))

        // B was removed — factory is invoked for a fresh instance
        val fresh = TrackingViewModel()
        val retrieved = ViewModelStore.getOrPut(keyB) { fresh }
        assertSame(fresh, retrieved)
    }

    @Test
    fun sync_callsOnDispose_onRemovedViewModels() {
        val keyA = "screen_a"
        val keyB = "screen_b"
        ViewModelStore.getOrPut(keyA) { TrackingViewModel() }
        val vmB = TrackingViewModel()
        ViewModelStore.getOrPut(keyB) { vmB }

        ViewModelStore.sync(listOf(keyA))

        assertTrue(vmB.isDisposed)
    }

    @Test
    fun sync_doesNotDispose_viewModelsInActiveKeys() {
        val keyA = "screen_a"
        val keyB = "screen_b"
        val vmA = TrackingViewModel()
        ViewModelStore.getOrPut(keyA) { vmA }
        ViewModelStore.getOrPut(keyB) { TrackingViewModel() }

        ViewModelStore.sync(listOf(keyA))

        assertFalse(vmA.isDisposed)
        assertSame(vmA, ViewModelStore.getOrPut(keyA) { TrackingViewModel() })
    }

    @Test
    fun sync_withEmptyList_removesAllViewModels() {
        val vmA = TrackingViewModel()
        val vmB = TrackingViewModel()
        ViewModelStore.getOrPut("a") { vmA }
        ViewModelStore.getOrPut("b") { vmB }

        ViewModelStore.sync(emptyList())

        assertTrue(vmA.isDisposed)
        assertTrue(vmB.isDisposed)
    }

    @Test
    fun sync_invokesRemovedCallback_forEachRemovedKey() {
        val keyA = "screen_a"
        val keyB = "screen_b"
        val keyC = "screen_c"
        ViewModelStore.getOrPut(keyA) { TrackingViewModel() }
        ViewModelStore.getOrPut(keyB) { TrackingViewModel() }
        ViewModelStore.getOrPut(keyC) { TrackingViewModel() }

        val removed = mutableListOf<Any>()
        ViewModelStore.sync(listOf(keyA)) { removed.add(it) }

        assertEquals(2, removed.size)
        assertTrue(removed.containsAll(listOf(keyB, keyC)))
    }

    @Test
    fun sync_keepsAllViewModels_whenAllKeysActive() {
        val vmA = TrackingViewModel()
        val vmB = TrackingViewModel()
        ViewModelStore.getOrPut("a") { vmA }
        ViewModelStore.getOrPut("b") { vmB }

        val removed = mutableListOf<Any>()
        ViewModelStore.sync(listOf("a", "b")) { removed.add(it) }

        assertTrue(removed.isEmpty())
        assertFalse(vmA.isDisposed)
        assertFalse(vmB.isDisposed)
    }

    @Test
    fun clear_disposesAllViewModels_andEmptiesStore() {
        val vmA = TrackingViewModel()
        val vmB = TrackingViewModel()
        ViewModelStore.getOrPut("a") { vmA }
        ViewModelStore.getOrPut("b") { vmB }

        ViewModelStore.clear()

        assertTrue(vmA.isDisposed)
        assertTrue(vmB.isDisposed)
        // After clear, factory is called for any key
        val fresh = TrackingViewModel()
        assertSame(fresh, ViewModelStore.getOrPut("a") { fresh })
    }

    @Test
    fun sync_usesEquality_forKeyComparison() {
        // data class keys must be compared by equals, not reference
        data class ScreenKey(val name: String)

        val keyInstance1 = ScreenKey("auth")
        val keyInstance2 = ScreenKey("auth") // equal but not the same reference
        val vm = TrackingViewModel()
        ViewModelStore.getOrPut(keyInstance1) { vm }

        // sync with an equal key: VM should be preserved
        ViewModelStore.sync(listOf(keyInstance2))

        assertFalse(vm.isDisposed)
    }

    @Test
    fun getOrPut_afterClear_createsFreshViewModel() {
        val vm1 = TrackingViewModel()
        ViewModelStore.getOrPut("key") { vm1 }
        ViewModelStore.clear()

        val vm2 = TrackingViewModel()
        val retrieved = ViewModelStore.getOrPut("key") { vm2 }

        assertSame(vm2, retrieved)
        assertNotSame(vm1, retrieved)
    }

    private class TrackingViewModel : ViewModel {
        var isDisposed = false
            private set

        override fun onDispose() {
            isDisposed = true
        }
    }
}
