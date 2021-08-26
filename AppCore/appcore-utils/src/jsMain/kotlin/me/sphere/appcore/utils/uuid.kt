package me.sphere.appcore.utils

@JsModule("uuid")
private external object JsUuid {
    fun v4(): String
}

actual fun uuid(): String = JsUuid.v4()
