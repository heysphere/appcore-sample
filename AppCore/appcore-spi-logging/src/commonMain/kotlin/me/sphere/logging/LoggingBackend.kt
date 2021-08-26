package me.sphere.logging

interface LoggingBackend {
    fun log(level: LoggingLevel, message: String)
    fun log(level: LoggingLevel, exception: Throwable)
    fun logMetric(name: String, value: Long)
}
