package me.sphere.unicorn.notificationDetails

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import me.sphere.appcore.dataSource.DataSource
import me.sphere.appcore.usecases.NotificationInfo
import me.sphere.appcore.usecases.NotificationInfoUseCase
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
     savedStateHandle: SavedStateHandle,
     notificationInfoUseCase: NotificationInfoUseCase
) : StoreViewModel<NotificationDetailsState, NotificationDetailsAction>() {

    private val arg = savedStateHandle.toNotificationDetailsArg()

    override val store: Store<NotificationDetailsState, NotificationDetailsAction> = Store(
        initialState = NotificationDetailsState(),
        reducer = SphereInfoReducer(notificationInfoUseCase.info(arg.notificationId)),
        dispatcher = Dispatchers.Main.immediate,
        initialAction = NotificationDetailsAction.LoadNotificationInfo
    )
}

private class SphereInfoReducer(
    private val notificationInfoDataSource: DataSource<NotificationInfo>
) : Reducer<NotificationDetailsState, NotificationDetailsAction> {

    override fun reduce(
        state: NotificationDetailsState,
        action: NotificationDetailsAction
    ): Result<NotificationDetailsState, NotificationDetailsAction> {
        return when (action) {
            is NotificationDetailsAction.LoadNotificationInfo -> state.withEffect(loadNotificationInfo())
            is SideEffects.NotificationInfoLoaded -> state.copy(info = action.info).withoutEffect()
        }
    }

    private fun loadNotificationInfo() = notificationInfoDataSource.state.map {
        SideEffects.NotificationInfoLoaded(it)
    }
}

data class NotificationDetailsState(
    val info : DataSource.State<NotificationInfo>? = null
)

sealed class NotificationDetailsAction {
    object LoadNotificationInfo : NotificationDetailsAction()
}

private sealed class SideEffects : NotificationDetailsAction() {

    data class NotificationInfoLoaded(val info : DataSource.State<NotificationInfo>) : SideEffects()
}