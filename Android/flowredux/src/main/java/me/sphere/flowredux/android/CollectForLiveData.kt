package me.sphere.flowredux.android

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

fun <State> Flow<State>.collectForLiveData(
    coroutineScope: CoroutineScope,
    liveData: MutableLiveData<State> = MutableLiveData()
): MutableLiveData<State> {

    return liveData.also {
        coroutineScope.launch {
            collect { newState ->
                liveData.postValue(newState)
            }
        }
    }
}
