package me.sphere.flowredux.android

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import me.sphere.flowredux.Store

abstract class StoreViewModel<State, Action> : ViewModel() {

    protected abstract val store: Store<State, Action>

    val state: LiveData<State> by lazy {
        store.state.collectForLiveData(viewModelScope)
    }

    val stateFlow: StateFlow<State> by lazy {
        store.state as StateFlow
    }

    fun sendAction(action: Action) {
        store.dispatch(action)
    }

    override fun onCleared() {
        store.cancel()
        super.onCleared()
    }
}
