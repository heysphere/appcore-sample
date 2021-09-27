package me.sphere.flowredux.sandbox

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import me.sphere.flowredux.*

private data class AppState(
    val todoState: TodoState = TodoState()
)

private sealed class AppAction {

    object StartApp : AppAction()

    data class Todo(val todoAction: TodoAction) : AppAction()
}

private data class TodoState(
    val todos: List<Todo>? = null,
    val isLoading: Boolean = false
)

private data class Todo(
    val id: Int,
    val name: String
)

private sealed class TodoAction {

    object LoadTodos : TodoAction()

    data class TodosLoaded(
        val todoList: List<Todo>
    ) : TodoAction()

    object ClearTodos : TodoAction()
}

private val todos = listOf<Todo>(
    Todo(1, "first todo"),
    Todo(2, "second todo")
)

private val todoReducer = Reducer<TodoState, TodoAction> { state, action ->
    when (action) {
        is TodoAction.LoadTodos -> Result(state, Effect.effect { TodoAction.TodosLoaded(todos) })
        is TodoAction.TodosLoaded -> Result(
            state.copy(todos = action.todoList, isLoading = false), emptyEffect()
        )
        is TodoAction.ClearTodos -> Result(
            state.copy(todos = null), emptyEffect()
        )
    }
}

private val appReducer = Reducer<AppState, AppAction> { state, action ->
    when (action) {
        is AppAction.StartApp -> Result(
            state,
            Effect.effect<AppAction> { AppAction.Todo(TodoAction.LoadTodos) }
        )
        is AppAction.Todo -> Result(state, emptyEffect())
    }
}

private val rootReducer = appReducer.combine(
    todoReducer.transform(
        toLocalState = { it.todoState },
        toLocalAction = { if (it is AppAction.Todo) it.todoAction else null },
        toParentAction = { AppAction.Todo(it) },
        toParentState = { appState, todoState -> appState.copy(todoState) }
    )
)

fun main() = runBlocking {

    val testDispatcher = TestCoroutineDispatcher()

    val store = Store(
        initialState = AppState(),
        reducer = rootReducer,
        testDispatcher
    )

    val job = launch(testDispatcher) {
        store.state.collect { state ->
            println("Got State from AppStore ===> $state")
        }
    }

    println("Store subscription has started")

    delay(300)

    println("Sending AppAction.StartApp")
    store.dispatch(AppAction.StartApp)

    delay(1500)

    println("Get scope of todo store")

    val todoScope = CoroutineScope(testDispatcher)

    val todoStore: Store<TodoState, TodoAction> = store.scope<TodoState, TodoAction>(
        toLocalState = { it.todoState },
        fromLocalAction = { AppAction.Todo(it) },
        coroutineScope = todoScope
    )

    val todoStoreJob = launch(testDispatcher) {
        todoStore.state.collect { state ->
            println("Got State from TodoStore -----> $state")
        }
    }

    delay(1000)

    println("Clear the todos from the todoStore")
    todoStore.dispatch(TodoAction.ClearTodos)

    // close the subscription
    job.cancel()
    todoStoreJob.cancel()
}
