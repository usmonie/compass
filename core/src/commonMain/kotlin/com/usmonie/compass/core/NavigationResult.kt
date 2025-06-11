package com.usmonie.compass.core

import androidx.collection.MutableScatterMap
import androidx.collection.mutableScatterMapOf
import com.usmonie.compass.core.navigation.ScreenId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Base interface for all navigation results.
 * Ensures type-safety when passing results between screens.
 */
public interface NavigationResult {
    public val id: ScreenId
}

/**
 * Registry for managing result listeners and channels between screens.
 */
internal class NavigationResultRegistry {
    private val resultChannels = mutableScatterMapOf<ScreenId, Channel<NavigationResult>>()
    private val activeResults = MutableStateFlow<MutableScatterMap<ScreenId, NavigationResult>>(mutableScatterMapOf())

    /**
     * Registers a result listener for a specific screen.
     *
     * @param screenId Identifier of the screen expecting the result
     * @param channel Channel to receive the result
     */
    fun registerResultListener(screenId: ScreenId, channel: Channel<NavigationResult>) {
        resultChannels[screenId] = channel
    }

    /**
     * Removes a result listener for a specific screen.
     *
     * @param screenId Identifier of the screen
     */
    fun removeResultListener(screenId: ScreenId) {
        resultChannels.remove(screenId)
    }

    /**
     * Delivers a result to the registered listener.
     *
     * @param result The result to be delivered
     * @return true if the result was delivered successfully, false otherwise
     */
    suspend fun deliverResult(result: NavigationResult): Boolean {
        val channel = resultChannels[result.id] ?: return false
        return try {
            channel.send(result)
            activeResults.value = activeResults.value.apply { put(result.id, result) }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if there's an active result for a specific screen.
     *
     * @param screenId Identifier of the screen
     * @return The result if exists, null otherwise
     */
    fun getActiveResult(screenId: ScreenId): NavigationResult? = activeResults.value[screenId]

    /**
     * Clears all result channels and active results.
     */
    fun clear() {
        resultChannels.forEach { _, channel ->
            channel.close()
        }
        resultChannels.clear()
        activeResults.value = mutableScatterMapOf()
    }
}

/**
 * Common result types that can be used across different screens.
 */
public sealed class CommonNavigationResult : NavigationResult {
    public data class Success(override val id: ScreenId, val data: Any? = null) : CommonNavigationResult()
    public data class Cancelled(override val id: ScreenId) : CommonNavigationResult()
    public data class Error(override val id: ScreenId, val error: Throwable) : CommonNavigationResult()
}

/**
 * Helper class to create type-safe result contracts between screens.
 */
public abstract class NavigationResultContract<T : NavigationResult> {
    public abstract val screenId: ScreenId
    
    /**
     * Creates a success result with the given data.
     */
    public fun createSuccess(data: T? = null): CommonNavigationResult.Success {
        return CommonNavigationResult.Success(screenId, data)
    }
    
    /**
     * Creates a cancelled result.
     */
    public fun createCancelled(): CommonNavigationResult.Cancelled {
        return CommonNavigationResult.Cancelled(screenId)
    }
    
    /**
     * Creates an error result with the given throwable.
     */
    public fun createError(error: Throwable): CommonNavigationResult.Error {
        return CommonNavigationResult.Error(screenId, error)
    }
}

/**
 * Example usage of a custom result contract:
 *
 * ```kotlin
 * data class ProfileResult(
 *     override val id: String,
 *     val updatedName: String? = null,
 *     val updatedEmail: String? = null
 * ) : NavigationResult
 *
 * class ProfileResultContract : NavigationResultContract<ProfileResult>() {
 *     override val screenId: String = "profile_screen"
 *
 *     fun createProfileResult(name: String? = null, email: String? = null): ProfileResult {
 *         return ProfileResult(screenId, name, email)
 *     }
 * }
 * ```
 */