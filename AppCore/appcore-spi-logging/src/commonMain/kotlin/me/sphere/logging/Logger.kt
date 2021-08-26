package me.sphere.logging

import me.sphere.appcore.utils.*

enum class LoggingLevel {
    Error, Info
}

class Logger(private val isErrorEnabled: Boolean, private val backend: LoggingBackend) {
    init { freeze() }

    fun info(message: () -> String) = autoreleasepool {
        if (isErrorEnabled) {
            backend.log(LoggingLevel.Info, message())
        }
    }

    fun exception(exception: Throwable) = autoreleasepool {
        if (isErrorEnabled) {
            backend.log(LoggingLevel.Error, exception)
        }
    }

    fun metric(name: String, value: Long) {
        backend.logMetric(name, value)
    }
}
