package com.usmonie.compass.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class FlowController {

    private val jobs = mutableMapOf<SubscriptionKey, Job>()

    fun launch(
        key: SubscriptionKey,
        scope: CoroutineScope,
        block: suspend CoroutineScope.() -> Unit
    ) {
        jobs[key]?.cancel()

        jobs[key] = scope.launch {
            block()
        }
    }

    fun stop(key: SubscriptionKey) {
        jobs.remove(key)?.cancel()
    }

    fun stopAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }

    fun isRunning(key: SubscriptionKey): Boolean =
        jobs[key]?.isActive == true
}