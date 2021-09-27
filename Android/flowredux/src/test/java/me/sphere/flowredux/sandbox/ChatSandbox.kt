package me.sphere.flowredux.sandbox

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import me.sphere.flowredux.Effect.Companion.withSideEffect
import me.sphere.flowredux.Effect.Companion.withoutEffect
import me.sphere.flowredux.Reducer
import me.sphere.flowredux.Reducer.Companion.combine
import me.sphere.flowredux.Store

private data class ChatMessagesState(
    val isLoading: Boolean = false,
    val messages: List<Message> = emptyList()
)

private data class Message(
    val id: Int,
    val body: String
)

private val messages = listOf(
    Message(1, "message_#1"),
    Message(2, "message_#2"),
    Message(3, "message_#3")
)

private sealed class ChatMessagesActions {

    data class LoadMessages(val chatId: String) : ChatMessagesActions()

    data class MessagesLoaded(val messages: List<Message>) : ChatMessagesActions()
}

private val messagesReducer = Reducer<ChatMessagesState, ChatMessagesActions> { state, action ->
    when (action) {
        is ChatMessagesActions.LoadMessages -> state.withSideEffect {
            ChatMessagesActions.MessagesLoaded(messages) /*replace with network call */
        }
        is ChatMessagesActions.MessagesLoaded -> state.copy(
            isLoading = false,
            messages = action.messages
        ).withoutEffect()
    }
}

private data class ChatToolbarState(
    val isUserTyping: Boolean = false,
    val title: String = ""
)

private sealed class ChatToolbarActions {

    data class TitleChanged(val title: String) : ChatToolbarActions()

    data class UserTyping(val isUserTyping: Boolean) : ChatToolbarActions()
}

private val toolbarReducer = Reducer<ChatToolbarState, ChatToolbarActions> { state, action ->
    when (action) {
        is ChatToolbarActions.TitleChanged -> state.copy(title = action.title).withoutEffect()
        is ChatToolbarActions.UserTyping -> state.copy(isUserTyping = action.isUserTyping).withoutEffect()
    }
}

private data class ChatState(
    val chatId: String,
    val contentState: ChatMessagesState = ChatMessagesState(),
    val toolbarState: ChatToolbarState = ChatToolbarState(),
    val isInitialized: Boolean = false
)

private sealed class ChatActions {

    object StartChat : ChatActions()

    data class ChatMessagesAction(val action: ChatMessagesActions) : ChatActions()

    data class ChatToolbarAction(val action: ChatToolbarActions) : ChatActions()
}

private val chatReducer = Reducer<ChatState, ChatActions> { state, action ->
    when (action) {
        is ChatActions.StartChat -> state.withSideEffect {
            ChatActions.ChatMessagesAction(ChatMessagesActions.LoadMessages(state.chatId))
        }
        else -> state.withoutEffect()
    }
}

private val screenReducer = combine(
    chatReducer,

    messagesReducer.transform(
        toLocalState = { it.contentState },
        toLocalAction = { if (it is ChatActions.ChatMessagesAction) it.action else null },
        toParentAction = { ChatActions.ChatMessagesAction(it) },
        toParentState = { chatState, messagesState -> chatState.copy(contentState = messagesState) }
    ),

    toolbarReducer.transform(
        toLocalState = { it.toolbarState },
        toLocalAction = { if (it is ChatActions.ChatToolbarAction) it.action else null },
        toParentAction = { ChatActions.ChatToolbarAction(it) },
        toParentState = { chatState, toolbarState -> chatState.copy(toolbarState = toolbarState) }
    )
)

fun main() = runBlocking {

    val testDispatcher = TestCoroutineDispatcher()
    val chatId = "chat_id"

    val store = Store(
        initialState = ChatState(chatId),
        reducer = screenReducer,
        testDispatcher
    )

    println("Screen Store subscription has started")

    val job = launch(testDispatcher) {
        store.state.collect { state ->
            println("Got State from ScreenStore ===> $state")
        }
    }

    delay(300)

    println("Sending ChatActions.StartChat")
    store.dispatch(ChatActions.StartChat)

    delay(1500)

    println("Get scope of messages store")

    val messagesScope = CoroutineScope(testDispatcher)

    val messagesStore: Store<ChatMessagesState, ChatMessagesActions> = store.scope(
        toLocalState = { it.contentState },
        fromLocalAction = { ChatActions.ChatMessagesAction(it) },
        coroutineScope = messagesScope
    )

    val todoStoreJob = launch(testDispatcher) {
        messagesStore.state.collect { state ->
            println("Got State from MessagesStore -----> $state")
        }
    }

    delay(1000)

    val toolbarScope = CoroutineScope(testDispatcher)

    val toolbarStore: Store<ChatToolbarState, ChatToolbarActions> = store.scope(
        toLocalState = { it.toolbarState },
        fromLocalAction = { ChatActions.ChatToolbarAction(it) },
        coroutineScope = toolbarScope
    )

    println("Change the toolbar title")
    toolbarStore.dispatch(ChatToolbarActions.TitleChanged("new title"))

    delay(1000)
    // close the subscription
    job.cancel()
    todoStoreJob.cancel()
}
