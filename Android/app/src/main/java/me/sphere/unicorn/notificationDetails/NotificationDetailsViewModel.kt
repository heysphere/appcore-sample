package me.sphere.unicorn.notificationDetails

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import me.sphere.flowredux.Effect.Companion.withEffect
import me.sphere.flowredux.Effect.Companion.withoutEffect
import me.sphere.flowredux.Reducer
import me.sphere.flowredux.Result
import me.sphere.flowredux.Store
import me.sphere.flowredux.android.StoreViewModel
import me.sphere.unicorn.ui.theme.RouteArgument.NotificationDetailsArg.Companion.toNotificationDetailsArg
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class NotificationDetailsViewModel @Inject constructor(
     savedStateHandle: SavedStateHandle
) : StoreViewModel<NotificationDetailsState, NotificationDetailsAction>() {

    private val arg = savedStateHandle.toNotificationDetailsArg()

    override val store: Store<NotificationDetailsState, NotificationDetailsAction> = Store(
        initialState = NotificationDetailsState(notificationId = arg.notificationId, 0),
        reducer = SphereInfoReducer(),
        dispatcher = Dispatchers.Main.immediate,
        initialAction = NotificationDetailsAction.LoadDummyTimer
    )
}

private class SphereInfoReducer() : Reducer<NotificationDetailsState, NotificationDetailsAction> {

    override fun reduce(
        state: NotificationDetailsState,
        action: NotificationDetailsAction
    ): Result<NotificationDetailsState, NotificationDetailsAction> {
        return when (action) {
            is NotificationDetailsAction.LoadDummyTimer -> state.withEffect(loadInfiniteCounter())
            is SideEffects.InfiniteLoaderValue -> state.copy(counter = action.count).withoutEffect()
        }
    }

    private fun loadInfiniteCounter() = createInfiniteCounter().map(SideEffects::InfiniteLoaderValue)

    private fun createInfiniteCounter() = flow {
        val count = AtomicInteger()
        while (true) {
            delay(1000)
            emit(count.getAndIncrement())
        }
    }
}

data class NotificationDetailsState(
    val notificationId: String,
    val counter: Int
)

sealed class NotificationDetailsAction {
    object LoadDummyTimer : NotificationDetailsAction()
}

private sealed class SideEffects : NotificationDetailsAction() {

    data class InfiniteLoaderValue(val count: Int) : SideEffects()
}