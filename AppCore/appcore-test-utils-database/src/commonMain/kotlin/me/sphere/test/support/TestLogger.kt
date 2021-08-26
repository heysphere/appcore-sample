package me.sphere.test.support

import me.sphere.appcore.utils.freeze
import me.sphere.logging.*
import kotlin.native.concurrent.SharedImmutable

@SharedImmutable
val TestLogger = Logger(true, object : LoggingBackend {
    override fun log(level: LoggingLevel, message: String) {
        println("TestLogger [${level.name}] $message")
    }

    override fun log(level: LoggingLevel, exception: Throwable) {
        println("TestLogger [${level.name}] $exception")
        exception.printStackTrace()
    }

    override fun logMetric(name: String, value: Long) {}
}).freeze()
