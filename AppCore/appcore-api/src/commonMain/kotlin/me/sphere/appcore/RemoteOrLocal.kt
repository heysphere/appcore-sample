package me.sphere.appcore

sealed class RemoteOrLocal<out Remote: Any, out Local: Any> {
    data class Remote<Value: Any>(val value: Value) : RemoteOrLocal<Value, Nothing>()
    data class Local<Value: Any>(val value: Value) : RemoteOrLocal<Nothing, Value>()
}
