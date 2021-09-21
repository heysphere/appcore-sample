package me.sphere.unicorn.notificationDetails

import android.util.Log
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import me.sphere.unicorn.ui.theme.RouteArgument.NotificationDetailsArg.Companion.toNotificationDetailsArg
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class NotificationDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val arg = savedStateHandle.toNotificationDetailsArg()
    private val _state = MutableLiveData<NotificationDetailsState>(
        NotificationDetailsState(arg.notificationId, 0)
    )
    val state: LiveData<NotificationDetailsState> = _state

    init {
        viewModelScope.launch {
            createInfiniteCounter()
                .onCompletion {
                    Log.e("antonis88", "NotificationDetailsViewModel.flow disposed")
                }
                .onStart {
                    Log.e("antonis88", "NotificationDetailsViewModel.flow started")
                }
                .collect { int ->
                    reduce {
                        it.copy(counter = int)
                    }
                }
        }
    }


    private fun createInfiniteCounter() = flow {
        val count = AtomicInteger()
        while (true) {
            delay(1000)
            emit(count.getAndIncrement())
        }
    }

    private fun reduce(cb: (NotificationDetailsState) -> NotificationDetailsState) {
        val currentValue = state.value ?: return
        val newState = cb(currentValue)
        _state.postValue(newState)
    }
}

data class NotificationDetailsState(
    val notificationId: String,
    val counter: Int
)