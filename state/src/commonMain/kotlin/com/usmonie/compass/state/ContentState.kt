package com.usmonie.compass.state


/**
 * A sealed class that represents different content loading states in a type-safe way.
 *
 * ContentState provides a convenient way to handle the common pattern of loading,
 * success, and error states when fetching or processing data. It encapsulates
 * the state of an asynchronous operation and its result.
 *
 * This class is particularly useful for UI states where you need to show loading
 * indicators, display data when available, or show error messages when operations fail.
 *
 * @param T The type of data being loaded
 * @param item The data item, null for Loading and Error states
 *
 * Example usage:
 * ```kotlin
 * data class UserListState(
 *     val users: ContentState<List<User>> = ContentState.Loading()
 * ) : State
 *
 * // In your UI
 * when (val usersState = state.users) {
 *     is ContentState.Loading -> CircularProgressIndicator()
 *     is ContentState.Success -> LazyColumn {
 *         items(usersState.data) { user -> UserItem(user) }
 *     }
 *     is ContentState.Error -> ErrorMessage(usersState.error.message)
 * }
 * ```
 */
public sealed class ContentState<T>(public val item: T?) {
    /**
     * Represents a successful state with loaded data.
     *
     * @param data The successfully loaded data
     */
    public data class Success<T>(val data: T) : ContentState<T>(data)

    /**
     * Represents an error state with error information.
     *
     * @param error The error that occurred during loading
     */
    public data class Error<T, E : ErrorState>(val error: E) : ContentState<T>(null)

    /**
     * Represents a loading state where data is being fetched.
     */
    public class Loading<T> : ContentState<T>(null)
}

/**
 * Updates the data within a ContentState if it's in Success state.
 *
 * This function allows you to transform the data inside a ContentState.Success
 * while preserving the state type for Loading and Error states.
 *
 * @param onSuccess A function to transform the data if the state is Success
 * @return A new ContentState with updated data, or the original state if not Success
 *
 * Example usage:
 * ```kotlin
 * val updatedState = state.users.updateData { userList ->
 *     userList.filter { it.isActive }
 * }
 * ```
 */
public inline fun <T> ContentState<T>.updateData(
    onSuccess: (T) -> T,
): ContentState<T> {
    return when (this) {
        is ContentState.Error<*, *> -> this
        is ContentState.Loading -> this
        is ContentState.Success -> ContentState.Success(onSuccess(data))
    }
}
