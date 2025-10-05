internal data class User(val id: String, val name: String)

// model/ApiClient.kt
internal object ApiClient {
    suspend fun fetchUser(userId: String): User {
        // Simulate network delay
        kotlinx.coroutines.delay(1000)
        return User(userId, "John Doe")
    }
}