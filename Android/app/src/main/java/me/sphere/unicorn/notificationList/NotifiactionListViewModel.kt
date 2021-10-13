package me.sphere.unicorn.notificationList

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import me.sphere.appcore.dataSource.PagingDataSource
import me.sphere.appcore.dataSource.PagingState
import me.sphere.appcore.usecases.Notification
import me.sphere.appcore.usecases.NotificationActionUseCase
import me.sphere.appcore.usecases.NotificationListUseCase
import me.sphere.flowredux.Effect.Companion.withEffect
import me.sphere.flowredux.Effect.Companion.withoutEffect
import me.sphere.flowredux.Reducer
import me.sphere.flowredux.Result
import me.sphere.flowredux.Store
import me.sphere.flowredux.android.StoreViewModel
import javax.inject.Inject

@HiltViewModel
class NotificationListViewModel @Inject constructor(
    useCase: NotificationListUseCase,
    notificationActionUseCase: NotificationActionUseCase
) : StoreViewModel<NotificationListState, NotificationListAction>() {
    override val store = Store(
        initialState = NotificationListState(),
        reducer = NotificationListReducer(
            useCase.notifications(shouldShowAll = false),
            notificationActionUseCase
        ),
        initialAction = NotificationListAction.LoadNotifications,
        dispatcher = Dispatchers.Main.immediate,
    )
}

private class NotificationListReducer(
    private val notificationDataSource: PagingDataSource<Notification>,
    private val notificationActionUseCase: NotificationActionUseCase
) : Reducer<NotificationListState, NotificationListAction> {
    override fun reduce(
        state: NotificationListState, action: NotificationListAction
    ): Result<NotificationListState, NotificationListAction> = when (action) {
        NotificationListAction.LoadNotifications -> state.withEffect(loadNotifications())
        is SideEffects.NotificationsLoaded -> state.copy(state = action.pagingState).withoutEffect()
        NotificationListAction.LoadNextPage -> {
            notificationDataSource.next()
            state.withoutEffect()
        }
        NotificationListAction.Retry -> {
            notificationDataSource.reload()
            state.withoutEffect()
        }
        is NotificationListAction.MarkAsRead -> {
            notificationActionUseCase.markAsRead(action.id)
            state.withoutEffect()
        }
    }

    private fun loadNotifications() = notificationDataSource.state.map {
        SideEffects.NotificationsLoaded(it)
    }
}

data class NotificationListState(
    val state: PagingState<Notification>? = null
)

sealed class NotificationListAction {
    object LoadNotifications : NotificationListAction()
    object LoadNextPage : NotificationListAction()
    object Retry : NotificationListAction()
    data class MarkAsRead(val id: String) : NotificationListAction()
}

private sealed class SideEffects : NotificationListAction() {
    data class NotificationsLoaded(
        val pagingState: PagingState<Notification>
    ) : SideEffects()
}