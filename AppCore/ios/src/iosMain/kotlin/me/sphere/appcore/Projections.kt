package me.sphere.appcore

import kotlinx.cinterop.autoreleasepool
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import me.sphere.appcore.utils.*
import platform.Foundation.NSArray

enum class DispatchPreference {
    MAIN, NON_MAIN;

    internal val dispatcher get() = when (this) {
        MAIN -> Dispatchers.Main.immediate
        NON_MAIN -> Dispatchers.Default
    }
}

/// Swift compiler segfault 11 workaround.
@OptIn(ExperimentalCoroutinesApi::class)
fun <T: Any> ListProjection<T>.launchUntyped(
    preference: DispatchPreference = DispatchPreference.MAIN,
    onNext: (NSArray) -> Unit,
    onCompletion: (Throwable?) -> Unit
): Job
    = CoroutineScope(preference.dispatcher).freeze().run {
    launch {
        onCompletion { onCompletion(it?.freeze()) }
            .collect { onNext(it.freeze() as NSArray) }
        }
    }

@OptIn(ExperimentalCoroutinesApi::class)
fun <T: Any> Projection<T>.launch(
    preference: DispatchPreference = DispatchPreference.MAIN,
    onEach: (T) -> Unit,
    onCompletion: (Throwable?) -> Unit
): Job = CoroutineScope(preference.dispatcher).freeze().run {
    launch {
        onCompletion { onCompletion(it?.freeze()) }
            .collect { onEach(it.freeze()) }
    }
}

fun <T: Any> Single<T>.launch(
    preference: DispatchPreference = DispatchPreference.MAIN,
    onSuccess: (T) -> Unit,
    onFailure: (Throwable) -> Unit
): Job = CoroutineScope(preference.dispatcher).freeze().run {
    launch {
        onEach { onSuccess(it.freeze()) }
            .catch { onFailure(it.freeze()) }
            .collect()
    }
}
