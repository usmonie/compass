# Getting Started with Compass State Management

This guide will walk you through setting up and using Compass State Management in your Kotlin
Multiplatform project. By the end of this guide, you'll have a solid understanding of the MVI
architecture and how to implement it using Compass State.

## 📋 Prerequisites

- Kotlin Multiplatform project setup
- Basic knowledge of Jetpack Compose
- Understanding of coroutines and flows (helpful but not required)

## 🎯 What You'll Build

We'll create a simple todo app that demonstrates all the core concepts:

- Adding, editing, and deleting todos
- Filtering todos by status
- Loading state management
- Error handling

## 📦 Step 1: Add Dependencies

Add the following to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.usmonie.compass:state:0.2.1")
    
    // For testing
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("junit:junit:4.13.2")
}
```

## 🏗️ Step 2: Understanding MVI Architecture

Before we start coding, let's understand the MVI pattern used by Compass State:

```
┌─────────────┐    ┌──────────────┐    ┌─────────────┐    ┌─────────────┐
│    User     │───▶│    Action    │───▶│   Process   │───▶│    Event    │
│ Interaction │    │              │    │   Action    │    │             │
└─────────────┘    └──────────────┘    └─────────────┘    └─────────────┘
                                                                   │
┌─────────────┐    ┌──────────────┐    ┌─────────────┐            │
│     UI      │◀───│     State    │◀───│   Reduce    │◀───────────┘
│ Recompose   │    │              │    │    State    │
└─────────────┘    └──────────────┘    └─────────────┘
                           │
                           ▼
                   ┌─────────────┐
                   │   Effect    │
                   │ (Optional)  │
                   └─────────────┘
```

- **State**: Immutable data representing your UI
- **Action**: User intents (button clicks, text input)
- **Event**: Internal events that happened as a result of actions
- **Effect**: One-time side effects (navigation, toasts)

## 🧱 Step 3: Define Your Data Models

First, let's create our todo data model:

```kotlin
data class Todo(
    val id: String,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class TodoFilter {
    ALL, ACTIVE, COMPLETED
}
```

## 📝 Step 4: Define MVI Components

### State

```kotlin
data class TodoState(
    val todos: ContentState<List<Todo>> = ContentState.Loading(),
    val filter: TodoFilter = TodoFilter.ALL,
    val isAddingTodo: Boolean = false,
    val editingTodo: Todo? = null
) : State {
    val filteredTodos: List<Todo>
        get() = when (val todosState = todos) {
            is ContentState.Success -> {
                when (filter) {
                    TodoFilter.ALL -> todosState.data
                    TodoFilter.ACTIVE -> todosState.data.filter { !it.isCompleted }
                    TodoFilter.COMPLETED -> todosState.data.filter { it.isCompleted }
                }
            }
            else -> emptyList()
        }
}
```

### Actions

```kotlin
sealed class TodoAction : Action {
    object LoadTodos : TodoAction()
    data class AddTodo(val title: String, val description: String) : TodoAction()
    data class UpdateTodo(val todo: Todo) : TodoAction()
    data class DeleteTodo(val todoId: String) : TodoAction()
    data class ToggleTodoComplete(val todoId: String) : TodoAction()
    data class SetFilter(val filter: TodoFilter) : TodoAction()
    data class StartEditingTodo(val todo: Todo) : TodoAction()
    object CancelEditingTodo : TodoAction()
}
```

### Events

```kotlin
sealed class TodoEvent : Event {
    object LoadingStarted : TodoEvent()
    data class TodosLoaded(val todos: List<Todo>) : TodoEvent()
    data class LoadingFailed(val error: String) : TodoEvent()
    data class TodoAdded(val todo: Todo) : TodoEvent()
    data class TodoUpdated(val todo: Todo) : TodoEvent()
    data class TodoDeleted(val todoId: String) : TodoEvent()
    data class FilterChanged(val filter: TodoFilter) : TodoEvent()
    data class EditingStarted(val todo: Todo) : TodoEvent()
    object EditingCancelled : TodoEvent()
}
```

### Effects

```kotlin
sealed class TodoEffect : Effect {
    data class ShowMessage(val message: String) : TodoEffect()
    object ScrollToTop : TodoEffect()
    data class ShowUndoOption(val todo: Todo) : TodoEffect()
}
```

## 🧠 Step 5: Create the ViewModel

```kotlin
class TodoViewModel(
    private val todoRepository: TodoRepository
) : StateViewModel<TodoState, TodoAction, TodoEvent, TodoEffect>(
    initialState = TodoState()
) {
    
    init {
        // Load todos when ViewModel is created
        handleAction(TodoAction.LoadTodos)
    }

    override suspend fun processAction(action: TodoAction): TodoEvent = when (action) {
        TodoAction.LoadTodos -> {
            try {
                val todos = todoRepository.getAllTodos()
                TodoEvent.TodosLoaded(todos)
            } catch (e: Exception) {
                TodoEvent.LoadingFailed(e.message ?: "Failed to load todos")
            }
        }
        
        is TodoAction.AddTodo -> {
            val newTodo = Todo(
                id = UUID.randomUUID().toString(),
                title = action.title,
                description = action.description
            )
            todoRepository.addTodo(newTodo)
            TodoEvent.TodoAdded(newTodo)
        }
        
        is TodoAction.UpdateTodo -> {
            todoRepository.updateTodo(action.todo)
            TodoEvent.TodoUpdated(action.todo)
        }
        
        is TodoAction.DeleteTodo -> {
            todoRepository.deleteTodo(action.todoId)
            TodoEvent.TodoDeleted(action.todoId)
        }
        
        is TodoAction.ToggleTodoComplete -> {
            val currentTodos = (state.value.todos as? ContentState.Success)?.data ?: return TodoEvent.LoadingFailed("No todos loaded")
            val todo = currentTodos.find { it.id == action.todoId } ?: return TodoEvent.LoadingFailed("Todo not found")
            val updatedTodo = todo.copy(isCompleted = !todo.isCompleted)
            todoRepository.updateTodo(updatedTodo)
            TodoEvent.TodoUpdated(updatedTodo)
        }
        
        is TodoAction.SetFilter -> TodoEvent.FilterChanged(action.filter)
        
        is TodoAction.StartEditingTodo -> TodoEvent.EditingStarted(action.todo)
        
        TodoAction.CancelEditingTodo -> TodoEvent.EditingCancelled
    }

    override fun TodoState.reduce(event: TodoEvent): TodoState = when (event) {
        TodoEvent.LoadingStarted -> copy(todos = ContentState.Loading())
        
        is TodoEvent.TodosLoaded -> copy(todos = ContentState.Success(event.todos))
        
        is TodoEvent.LoadingFailed -> copy(
            todos = ContentState.Error(SimpleErrorState(Exception(event.error)))
        )
        
        is TodoEvent.TodoAdded -> {
            when (val currentTodos = todos) {
                is ContentState.Success -> copy(
                    todos = ContentState.Success(currentTodos.data + event.todo),
                    isAddingTodo = false
                )
                else -> this
            }
        }
        
        is TodoEvent.TodoUpdated -> {
            when (val currentTodos = todos) {
                is ContentState.Success -> copy(
                    todos = ContentState.Success(
                        currentTodos.data.map { 
                            if (it.id == event.todo.id) event.todo else it 
                        }
                    ),
                    editingTodo = null
                )
                else -> this
            }
        }
        
        is TodoEvent.TodoDeleted -> {
            when (val currentTodos = todos) {
                is ContentState.Success -> copy(
                    todos = ContentState.Success(
                        currentTodos.data.filter { it.id != event.todoId }
                    )
                )
                else -> this
            }
        }
        
        is TodoEvent.FilterChanged -> copy(filter = event.filter)
        
        is TodoEvent.EditingStarted -> copy(editingTodo = event.todo)
        
        TodoEvent.EditingCancelled -> copy(editingTodo = null)
    }

    override suspend fun handleEvent(event: TodoEvent): TodoEffect? = when (event) {
        is TodoEvent.TodoAdded -> TodoEffect.ShowMessage("Todo added: ${event.todo.title}")
        
        is TodoEvent.TodoDeleted -> {
            val deletedTodo = (state.value.todos as? ContentState.Success)?.data
                ?.find { it.id == event.todoId }
            deletedTodo?.let { TodoEffect.ShowUndoOption(it) }
        }
        
        is TodoEvent.LoadingFailed -> TodoEffect.ShowMessage("Error: ${event.error}")
        
        else -> null
    }
}

// Simple error state implementation
class SimpleErrorState(error: Throwable) : com.usmonie.compass.state.ErrorState(error)
```

## 🗄️ Step 6: Create a Simple Repository

```kotlin
interface TodoRepository {
    suspend fun getAllTodos(): List<Todo>
    suspend fun addTodo(todo: Todo)
    suspend fun updateTodo(todo: Todo)
    suspend fun deleteTodo(todoId: String)
}

// Simple in-memory implementation for this example
class InMemoryTodoRepository : TodoRepository {
    private val todos = mutableListOf<Todo>()

    override suspend fun getAllTodos(): List<Todo> {
        // Simulate network delay
        kotlinx.coroutines.delay(1000)
        return todos.toList()
    }

    override suspend fun addTodo(todo: Todo) {
        todos.add(todo)
    }

    override suspend fun updateTodo(todo: Todo) {
        val index = todos.indexOfFirst { it.id == todo.id }
        if (index >= 0) {
            todos[index] = todo
        }
    }

    override suspend fun deleteTodo(todoId: String) {
        todos.removeAll { it.id == todoId }
    }
}
```

## 🎨 Step 7: Create the UI

### Main Todo Screen

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    viewModel: TodoViewModel = remember { 
        TodoViewModel(InMemoryTodoRepository()) 
    }
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    StateContent(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is TodoEffect.ShowMessage -> {
                    snackbarMessage = effect.message
                }
                is TodoEffect.ShowUndoOption -> {
                    snackbarMessage = "Todo deleted"
                    // In a real app, you'd implement undo functionality here
                }
                TodoEffect.ScrollToTop -> {
                    // Implement scroll to top
                }
            }
        }
    ) { state, onAction ->
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("My Todos") },
                    actions = {
                        TodoFilterChips(
                            currentFilter = state.filter,
                            onFilterChanged = { filter ->
                                onAction(TodoAction.SetFilter(filter))
                            }
                        )
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Todo")
                }
            },
            snackbarHost = {
                snackbarMessage?.let { message ->
                    LaunchedEffect(message) {
                        snackbarMessage = null
                    }
                    SnackbarHost(
                        hostState = remember { SnackbarHostState() }.apply {
                            LaunchedEffect(message) {
                                showSnackbar(message)
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            
            when (val todosState = state.todos) {
                is ContentState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading todos...")
                        }
                    }
                }
                
                is ContentState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.filteredTodos, key = { it.id }) { todo ->
                            TodoItem(
                                todo = todo,
                                onToggleComplete = { 
                                    onAction(TodoAction.ToggleTodoComplete(todo.id)) 
                                },
                                onEdit = { 
                                    onAction(TodoAction.StartEditingTodo(todo)) 
                                },
                                onDelete = { 
                                    onAction(TodoAction.DeleteTodo(todo.id)) 
                                }
                            )
                        }
                        
                        if (state.filteredTodos.isEmpty()) {
                            item {
                                EmptyTodosMessage(filter = state.filter)
                            }
                        }
                    }
                }
                
                is ContentState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorMessage(
                            message = todosState.error.message,
                            onRetry = { onAction(TodoAction.LoadTodos) }
                        )
                    }
                }
            }
        }
        
        // Add Todo Dialog
        if (showAddDialog) {
            AddTodoDialog(
                onDismiss = { showAddDialog = false },
                onAddTodo = { title, description ->
                    onAction(TodoAction.AddTodo(title, description))
                    showAddDialog = false
                }
            )
        }
        
        // Edit Todo Dialog
        state.editingTodo?.let { todo ->
            EditTodoDialog(
                todo = todo,
                onDismiss = { onAction(TodoAction.CancelEditingTodo) },
                onSaveTodo = { updatedTodo ->
                    onAction(TodoAction.UpdateTodo(updatedTodo))
                }
            )
        }
    }
}
```

### Todo Item Component

```kotlin
@Composable
fun TodoItem(
    todo: Todo,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggleComplete() }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (todo.isCompleted) {
                        androidx.compose.ui.text.style.TextDecoration.LineThrough
                    } else null
                )
                
                if (todo.description.isNotBlank()) {
                    Text(
                        text = todo.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                    contentDescription = "Edit"
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                    contentDescription = "Delete"
                )
            }
        }
    }
}
```

### Helper Components

```kotlin
@Composable
fun TodoFilterChips(
    currentFilter: TodoFilter,
    onFilterChanged: (TodoFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        TodoFilter.values().forEach { filter ->
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterChanged(filter) },
                label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun EmptyTodosMessage(filter: TodoFilter) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = when (filter) {
                TodoFilter.ALL -> "No todos yet"
                TodoFilter.ACTIVE -> "No active todos"
                TodoFilter.COMPLETED -> "No completed todos"
            },
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = if (filter == TodoFilter.ALL) "Add one using the + button" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(32.dp)
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
fun AddTodoDialog(
    onDismiss: () -> Unit,
    onAddTodo: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Todo") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAddTodo(title, description) },
                enabled = title.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditTodoDialog(
    todo: Todo,
    onDismiss: () -> Unit,
    onSaveTodo: (Todo) -> Unit
) {
    var title by remember { mutableStateOf(todo.title) }
    var description by remember { mutableStateOf(todo.description) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Todo") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    onSaveTodo(todo.copy(title = title, description = description))
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

## 🧪 Step 8: Write Tests

```kotlin
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class TodoViewModelTest {
    
    private val repository = InMemoryTodoRepository()
    private val viewModel = TodoViewModel(repository)

    @Test
    fun `initial state should be loading`() {
        val state = viewModel.state.value
        assertTrue(state.todos is ContentState.Loading)
        assertEquals(TodoFilter.ALL, state.filter)
    }

    @Test
    fun `adding todo should update state`() = runTest {
        // Wait for initial loading to complete
        // In a real test, you'd use TestCoroutineScheduler
        
        viewModel.handleAction(TodoAction.AddTodo("Test Todo", "Test Description"))
        
        val state = viewModel.state.value
        val todos = (state.todos as? ContentState.Success)?.data
        assertNotNull(todos)
        assertEquals(1, todos?.size)
        assertEquals("Test Todo", todos?.first()?.title)
    }

    @Test
    fun `filtering should work correctly`() = runTest {
        // Add some todos
        viewModel.handleAction(TodoAction.AddTodo("Active Todo", ""))
        viewModel.handleAction(TodoAction.AddTodo("Completed Todo", ""))
        
        // Complete one todo
        val state = viewModel.state.value
        val todos = (state.todos as? ContentState.Success)?.data
        val todoToComplete = todos?.find { it.title == "Completed Todo" }
        if (todoToComplete != null) {
            viewModel.handleAction(TodoAction.ToggleTodoComplete(todoToComplete.id))
        }
        
        // Test filtering
        viewModel.handleAction(TodoAction.SetFilter(TodoFilter.ACTIVE))
        assertEquals(1, viewModel.state.value.filteredTodos.size)
        
        viewModel.handleAction(TodoAction.SetFilter(TodoFilter.COMPLETED))
        assertEquals(1, viewModel.state.value.filteredTodos.size)
        
        viewModel.handleAction(TodoAction.SetFilter(TodoFilter.ALL))
        assertEquals(2, viewModel.state.value.filteredTodos.size)
    }
}
```

## 🎉 Congratulations!

You've successfully built a complete todo app using Compass State Management! Here's what you've
learned:

### Key Concepts Covered

1. **MVI Architecture**: Clean separation between UI and business logic
2. **State Management**: Immutable state with type-safe updates
3. **Content State**: Elegant handling of loading/success/error states
4. **Effects**: One-time side effects for UI interactions
5. **Testing**: Pure functions make testing straightforward

### Next Steps

1. **Explore Navigation**: Integrate with Compass Navigation for multi-screen apps
2. **Add Persistence**: Replace the in-memory repository with a real database
3. **Enhance UI**: Add animations and better visual feedback
4. **Advanced Patterns**: Explore `FlowStateViewModel` for complex async operations

### Best Practices Learned

- Keep states immutable
- Make actions descriptive and focused
- Use pure functions for state reduction
- Separate business logic from UI logic
- Test individual components in isolation

Ready to build more complex applications? Check out the [API Documentation](API.md)
and [Examples](EXAMPLES.md) for advanced patterns and real-world use cases!