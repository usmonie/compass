# Migration Guide & Examples

This document provides migration guides and practical examples for using the Compass State
Management library.

## Migration from Other Patterns

### From Traditional MVVM

**Before (Traditional MVVM):**

```kotlin
class UserViewModel : ViewModel() {
    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadUser(id: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val user = repository.getUser(id)
                _user.value = user
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```

**After (Compass State):**

```kotlin
// Define state, actions, events
data class UserState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) : State

sealed class UserAction : Action {
    data class LoadUser(val id: String) : UserAction()
}

sealed class UserEvent : Event {
    object LoadingStarted : UserEvent()
    data class UserLoaded(val user: User) : UserEvent()
    data class LoadingFailed(val error: String) : UserEvent()
}

// ViewModel
class UserViewModel : StateViewModel<UserState, UserAction, UserEvent, Nothing>(
    initialState = UserState()
) {
    override suspend fun processAction(action: UserAction): UserEvent = when (action) {
        is UserAction.LoadUser -> {
            try {
                val user = repository.getUser(action.id)
                UserEvent.UserLoaded(user)
            } catch (e: Exception) {
                UserEvent.LoadingFailed(e.message ?: "Unknown error")
            }
        }
    }

    override fun UserState.reduce(event: UserEvent): UserState = when (event) {
        UserEvent.LoadingStarted -> copy(isLoading = true, error = null)
        is UserEvent.UserLoaded -> copy(user = event.user, isLoading = false, error = null)
        is UserEvent.LoadingFailed -> copy(isLoading = false, error = event.error)
    }

    override suspend fun handleEvent(event: UserEvent): Nothing? = null
}
```

### From Redux-style State Management

**Before (Redux-style):**

```kotlin
// Actions
sealed class Action {
    data class LoadUsers(val query: String) : Action()
    data class SetLoading(val loading: Boolean) : Action()
    data class SetUsers(val users: List<User>) : Action()
    data class SetError(val error: String?) : Action()
}

// Reducer
fun userReducer(state: UserState, action: Action): UserState = when (action) {
    is Action.SetLoading -> state.copy(isLoading = action.loading)
    is Action.SetUsers -> state.copy(users = action.users)
    is Action.SetError -> state.copy(error = action.error)
    else -> state
}

// Side effects handling manually
class UserStore {
    fun dispatch(action: Action) {
        when (action) {
            is Action.LoadUsers -> {
                dispatch(Action.SetLoading(true))
                // Manual async handling...
            }
        }
    }
}
```

**After (Compass State):**

```kotlin
// Cleaner separation of user actions vs internal events
sealed class UserAction : Action {
    data class LoadUsers(val query: String) : UserAction()
}

sealed class UserEvent : Event {
    object LoadingStarted : UserEvent()
    data class UsersLoaded(val users: List<User>) : UserEvent()
    data class LoadingFailed(val error: String) : UserEvent()
}

// Automatic async handling with built-in structure
class UserViewModel : StateViewModel<UserState, UserAction, UserEvent, Nothing>(
    initialState = UserState()
) {
    override suspend fun processAction(action: UserAction): UserEvent = when (action) {
        is UserAction.LoadUsers -> {
            try {
                val users = repository.searchUsers(action.query)
                UserEvent.UsersLoaded(users)
            } catch (e: Exception) {
                UserEvent.LoadingFailed(e.message ?: "Search failed")
            }
        }
    }
    
    // Pure, predictable state reduction
    override fun UserState.reduce(event: UserEvent): UserState = when (event) {
        UserEvent.LoadingStarted -> copy(isLoading = true, error = null)
        is UserEvent.UsersLoaded -> copy(users = event.users, isLoading = false)
        is UserEvent.LoadingFailed -> copy(error = event.error, isLoading = false)
    }
}
```

## Real-World Examples

### Example 1: Shopping Cart

```kotlin
data class Product(val id: String, val name: String, val price: Double)
data class CartItem(val product: Product, val quantity: Int)

data class CartState(
    val items: List<CartItem> = emptyList(),
    val total: Double = 0.0,
    val isLoading: Boolean = false
) : State

sealed class CartAction : Action {
    data class AddItem(val product: Product) : CartAction()
    data class RemoveItem(val productId: String) : CartAction()
    data class UpdateQuantity(val productId: String, val quantity: Int) : CartAction()
    object ClearCart : CartAction()
    object Checkout : CartAction()
}

sealed class CartEvent : Event {
    data class ItemAdded(val product: Product) : CartEvent()
    data class ItemRemoved(val productId: String) : CartEvent()
    data class QuantityUpdated(val productId: String, val quantity: Int) : CartEvent()
    object CartCleared : CartEvent()
    object CheckoutStarted : CartEvent()
    object CheckoutCompleted : CartEvent()
}

sealed class CartEffect : Effect {
    object NavigateToCheckout : CartEffect()
    data class ShowMessage(val message: String) : CartEffect()
    object NavigateToOrderConfirmation : CartEffect()
}

class CartViewModel(
    private val checkoutService: CheckoutService
) : StateViewModel<CartState, CartAction, CartEvent, CartEffect>(
    initialState = CartState()
) {
    override suspend fun processAction(action: CartAction): CartEvent = when (action) {
        is CartAction.AddItem -> CartEvent.ItemAdded(action.product)
        is CartAction.RemoveItem -> CartEvent.ItemRemoved(action.productId)
        is CartAction.UpdateQuantity -> CartEvent.QuantityUpdated(action.productId, action.quantity)
        CartAction.ClearCart -> CartEvent.CartCleared
        CartAction.Checkout -> {
            try {
                checkoutService.processOrder(state.value.items)
                CartEvent.CheckoutCompleted
            } catch (e: Exception) {
                throw e // Let error handling deal with this
            }
        }
    }

    override fun CartState.reduce(event: CartEvent): CartState = when (event) {
        is CartEvent.ItemAdded -> {
            val existingItem = items.find { it.product.id == event.product.id }
            val newItems = if (existingItem != null) {
                items.map { 
                    if (it.product.id == event.product.id) 
                        it.copy(quantity = it.quantity + 1) 
                    else it 
                }
            } else {
                items + CartItem(event.product, 1)
            }
            copy(items = newItems, total = calculateTotal(newItems))
        }
        
        is CartEvent.ItemRemoved -> {
            val newItems = items.filter { it.product.id != event.productId }
            copy(items = newItems, total = calculateTotal(newItems))
        }
        
        is CartEvent.QuantityUpdated -> {
            val newItems = if (event.quantity <= 0) {
                items.filter { it.product.id != event.productId }
            } else {
                items.map { 
                    if (it.product.id == event.productId) 
                        it.copy(quantity = event.quantity) 
                    else it 
                }
            }
            copy(items = newItems, total = calculateTotal(newItems))
        }
        
        CartEvent.CartCleared -> copy(items = emptyList(), total = 0.0)
        CartEvent.CheckoutStarted -> copy(isLoading = true)
        CartEvent.CheckoutCompleted -> CartState() // Reset to empty cart
    }

    override suspend fun handleEvent(event: CartEvent): CartEffect? = when (event) {
        is CartEvent.ItemAdded -> CartEffect.ShowMessage("${event.product.name} added to cart")
        CartEvent.CheckoutCompleted -> CartEffect.NavigateToOrderConfirmation
        else -> null
    }

    private fun calculateTotal(items: List<CartItem>): Double = 
        items.sumOf { it.product.price * it.quantity }
}

// Usage in Compose
@Composable
fun CartScreen(viewModel: CartViewModel) {
    StatefulComponent(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is CartEffect.ShowMessage -> {
                    // Show snackbar or toast
                }
                CartEffect.NavigateToCheckout -> {
                    // Navigate to checkout
                }
                CartEffect.NavigateToOrderConfirmation -> {
                    // Navigate to order confirmation
                }
            }
        }
    ) { state, onAction ->
        Column {
            LazyColumn {
                items(state.items) { item ->
                    CartItemRow(
                        item = item,
                        onUpdateQuantity = { quantity ->
                            onAction(CartAction.UpdateQuantity(item.product.id, quantity))
                        },
                        onRemove = {
                            onAction(CartAction.RemoveItem(item.product.id))
                        }
                    )
                }
            }
            
            Text("Total: $${state.total}")
            
            Button(
                onClick = { onAction(CartAction.Checkout) },
                enabled = state.items.isNotEmpty() && !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Checkout")
                }
            }
        }
    }
}
```

### Example 2: Form with Validation

```kotlin
data class UserForm(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = ""
)

data class FieldError(val field: String, val message: String)

data class FormState(
    val form: UserForm = UserForm(),
    val errors: List<FieldError> = emptyList(),
    val isSubmitting: Boolean = false,
    val isValid: Boolean = false
) : State

sealed class FormAction : Action {
    data class UpdateName(val name: String) : FormAction()
    data class UpdateEmail(val email: String) : FormAction()
    data class UpdatePassword(val password: String) : FormAction()
    data class UpdateConfirmPassword(val confirmPassword: String) : FormAction()
    object SubmitForm : FormAction()
}

sealed class FormEvent : Event {
    data class FormUpdated(val form: UserForm) : FormEvent()
    data class ValidationCompleted(val errors: List<FieldError>) : FormEvent()
    object SubmissionStarted : FormEvent()
    object SubmissionCompleted : FormEvent()
    data class SubmissionFailed(val error: String) : FormEvent()
}

sealed class FormEffect : Effect {
    object NavigateToHome : FormEffect()
    data class ShowError(val message: String) : FormEffect()
}

class FormViewModel(
    private val userService: UserService,
    private val validator: FormValidator
) : FlowStateViewModel<FormState, FormAction, FormEvent, FormEffect>(
    initialState = FormState()
) {
    override suspend fun processAction(action: FormAction): Flow<FormEvent> = flow {
        when (action) {
            is FormAction.UpdateName -> {
                val newForm = state.value.form.copy(name = action.name)
                emit(FormEvent.FormUpdated(newForm))
                emit(FormEvent.ValidationCompleted(validator.validate(newForm)))
            }
            
            is FormAction.UpdateEmail -> {
                val newForm = state.value.form.copy(email = action.email)
                emit(FormEvent.FormUpdated(newForm))
                emit(FormEvent.ValidationCompleted(validator.validate(newForm)))
            }
            
            is FormAction.UpdatePassword -> {
                val newForm = state.value.form.copy(password = action.password)
                emit(FormEvent.FormUpdated(newForm))
                emit(FormEvent.ValidationCompleted(validator.validate(newForm)))
            }
            
            is FormAction.UpdateConfirmPassword -> {
                val newForm = state.value.form.copy(confirmPassword = action.confirmPassword)
                emit(FormEvent.FormUpdated(newForm))
                emit(FormEvent.ValidationCompleted(validator.validate(newForm)))
            }
            
            FormAction.SubmitForm -> {
                emit(FormEvent.SubmissionStarted)
                try {
                    userService.createUser(state.value.form)
                    emit(FormEvent.SubmissionCompleted)
                } catch (e: Exception) {
                    emit(FormEvent.SubmissionFailed(e.message ?: "Submission failed"))
                }
            }
        }
    }

    override fun FormState.reduce(event: FormEvent): FormState = when (event) {
        is FormEvent.FormUpdated -> copy(form = event.form)
        is FormEvent.ValidationCompleted -> copy(
            errors = event.errors,
            isValid = event.errors.isEmpty()
        )
        FormEvent.SubmissionStarted -> copy(isSubmitting = true)
        FormEvent.SubmissionCompleted -> this // State handled by effect
        is FormEvent.SubmissionFailed -> copy(isSubmitting = false)
    }

    override suspend fun handleEffect(event: FormEvent): FormEffect? = when (event) {
        FormEvent.SubmissionCompleted -> FormEffect.NavigateToHome
        is FormEvent.SubmissionFailed -> FormEffect.ShowError(event.error)
        else -> null
    }
}
```

### Example 3: Search with Debouncing

```kotlin
data class SearchState(
    val query: String = "",
    val results: ContentState<List<SearchResult>> = ContentState.Loading(),
    val recentSearches: List<String> = emptyList()
) : State

sealed class SearchAction : Action {
    data class UpdateQuery(val query: String) : SearchAction()
    data class Search(val query: String) : SearchAction()
    data class SelectResult(val result: SearchResult) : SearchAction()
    object ClearResults : SearchAction()
}

sealed class SearchEvent : Event {
    data class QueryUpdated(val query: String) : SearchEvent()
    object SearchStarted : SearchEvent()
    data class SearchCompleted(val results: List<SearchResult>) : SearchEvent()
    data class SearchFailed(val error: String) : SearchEvent()
    data class ResultSelected(val result: SearchResult) : SearchEvent()
    object ResultsCleared : SearchEvent()
}

sealed class SearchEffect : Effect {
    data class NavigateToResult(val result: SearchResult) : SearchEffect()
}

class SearchViewModel(
    private val searchService: SearchService
) : FlowStateViewModel<SearchState, SearchAction, SearchEvent, SearchEffect>(
    initialState = SearchState()
) {
    
    override suspend fun processAction(action: SearchAction): Flow<SearchEvent> = flow {
        when (action) {
            is SearchAction.UpdateQuery -> {
                emit(SearchEvent.QueryUpdated(action.query))
                // Debounce search
                delay(300)
                if (action.query == state.value.query && action.query.isNotBlank()) {
                    emit(SearchEvent.SearchStarted)
                    try {
                        val results = searchService.search(action.query)
                        emit(SearchEvent.SearchCompleted(results))
                    } catch (e: Exception) {
                        emit(SearchEvent.SearchFailed(e.message ?: "Search failed"))
                    }
                }
            }
            
            is SearchAction.Search -> {
                emit(SearchEvent.SearchStarted)
                try {
                    val results = searchService.search(action.query)
                    emit(SearchEvent.SearchCompleted(results))
                } catch (e: Exception) {
                    emit(SearchEvent.SearchFailed(e.message ?: "Search failed"))
                }
            }
            
            is SearchAction.SelectResult -> {
                emit(SearchEvent.ResultSelected(action.result))
            }
            
            SearchAction.ClearResults -> {
                emit(SearchEvent.ResultsCleared)
            }
        }
    }

    override fun SearchState.reduce(event: SearchEvent): SearchState = when (event) {
        is SearchEvent.QueryUpdated -> copy(query = event.query)
        SearchEvent.SearchStarted -> copy(results = ContentState.Loading())
        is SearchEvent.SearchCompleted -> copy(
            results = ContentState.Success(event.results),
            recentSearches = (listOf(query) + recentSearches).take(10)
        )
        is SearchEvent.SearchFailed -> copy(
            results = ContentState.Error(SimpleErrorState(Exception(event.error)))
        )
        is SearchEvent.ResultSelected -> this
        SearchEvent.ResultsCleared -> copy(results = ContentState.Loading())
    }

    override suspend fun handleEffect(event: SearchEvent): SearchEffect? = when (event) {
        is SearchEvent.ResultSelected -> SearchEffect.NavigateToResult(event.result)
        else -> null
    }
}

class SimpleErrorState(error: Throwable) : ErrorState(error)
```

## Testing Examples

### Unit Testing ViewModels

```kotlin
class CartViewModelTest {
    private lateinit var viewModel: CartViewModel
    private val mockCheckoutService = mockk<CheckoutService>()

    @Before
    fun setup() {
        viewModel = CartViewModel(mockCheckoutService)
    }

    @Test
    fun `adding item should update cart state`() = runTest {
        val product = Product("1", "Test Product", 10.0)
        
        viewModel.handleAction(CartAction.AddItem(product))
        
        val state = viewModel.state.value
        assertEquals(1, state.items.size)
        assertEquals(product, state.items.first().product)
        assertEquals(10.0, state.total)
    }

    @Test
    fun `adding same item twice should increase quantity`() = runTest {
        val product = Product("1", "Test Product", 10.0)
        
        viewModel.handleAction(CartAction.AddItem(product))
        viewModel.handleAction(CartAction.AddItem(product))
        
        val state = viewModel.state.value
        assertEquals(1, state.items.size)
        assertEquals(2, state.items.first().quantity)
        assertEquals(20.0, state.total)
    }

    @Test
    fun `checkout should emit navigation effect`() = runTest {
        val product = Product("1", "Test Product", 10.0)
        coEvery { mockCheckoutService.processOrder(any()) } returns Unit
        
        viewModel.handleAction(CartAction.AddItem(product))
        
        val effects = mutableListOf<CartEffect>()
        val job = launch {
            viewModel.effect.collect { effects.add(it) }
        }
        
        viewModel.handleAction(CartAction.Checkout)
        
        assertEquals(CartEffect.NavigateToOrderConfirmation, effects.last())
        job.cancel()
    }
}
```

### Integration Testing with Compose

```kotlin
@RunWith(AndroidJUnit4::class)
class CartScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `should display cart items`() {
        val viewModel = CartViewModel(mockk())
        val product = Product("1", "Test Product", 10.0)
        
        // Pre-populate cart
        viewModel.handleAction(CartAction.AddItem(product))
        
        composeTestRule.setContent {
            CartScreen(viewModel = viewModel)
        }
        
        composeTestRule.onNodeWithText("Test Product").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total: $10.0").assertIsDisplayed()
    }
    
    @Test
    fun `should handle checkout button click`() {
        val mockCheckoutService = mockk<CheckoutService>()
        coEvery { mockCheckoutService.processOrder(any()) } returns Unit
        
        val viewModel = CartViewModel(mockCheckoutService)
        val product = Product("1", "Test Product", 10.0)
        viewModel.handleAction(CartAction.AddItem(product))
        
        composeTestRule.setContent {
            CartScreen(viewModel = viewModel)
        }
        
        composeTestRule.onNodeWithText("Checkout").performClick()
        
        coVerify { mockCheckoutService.processOrder(any()) }
    }
}
```

## Performance Optimization Tips

### 1. Use ContentState for Heavy Operations

```kotlin
data class ExpensiveDataState(
    val data: ContentState<List<ComplexObject>> = ContentState.Loading()
) : State

// Update only when needed
fun ExpensiveDataState.updateIfSuccess(
    update: (List<ComplexObject>) -> List<ComplexObject>
): ExpensiveDataState = copy(
    data = data.updateData(update)
)
```

### 2. Batch State Updates with FlowStateViewModel

```kotlin
override suspend fun processAction(action: BatchAction): Flow<BatchEvent> = flow {
    when (action) {
        is BatchAction.LoadMultipleResources -> {
            emit(BatchEvent.LoadingStarted)
            
            // Emit multiple events in sequence
            try {
                val users = async { loadUsers() }
                val posts = async { loadPosts() }
                val comments = async { loadComments() }
                
                emit(BatchEvent.UsersLoaded(users.await()))
                emit(BatchEvent.PostsLoaded(posts.await()))
                emit(BatchEvent.CommentsLoaded(comments.await()))
            } catch (e: Exception) {
                emit(BatchEvent.LoadingFailed(e.message ?: "Loading failed"))
            }
        }
    }
}
```

### 3. Selective State Observation

```kotlin
@Composable
fun OptimizedComponent(viewModel: MyViewModel) {
    // Only observe specific parts of state
    val isLoading by remember {
        derivedStateOf { viewModel.state.value.isLoading }
    }
    
    val items by remember {
        derivedStateOf { viewModel.state.value.items }
    }
    
    // UI updates only when these specific values change
    if (isLoading) {
        LoadingSpinner()
    } else {
        ItemList(items = items)
    }
}
```

This migration guide should help teams transition to the Compass State Management library and
understand how to implement common patterns effectively.