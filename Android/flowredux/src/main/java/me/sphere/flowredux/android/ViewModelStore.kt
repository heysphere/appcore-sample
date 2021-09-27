package me.sphere.flowredux.android

import androidx.lifecycle.LiveData
import me.sphere.flowredux.Store

interface ViewModelStore<State, Action> {

    val store: Store<State, Action>

    val state: LiveData<State>

    fun sendAction(action: Action) {
        store.dispatch(action)
    }

    fun disposeStore() {
        store.cancel()
    }
}
