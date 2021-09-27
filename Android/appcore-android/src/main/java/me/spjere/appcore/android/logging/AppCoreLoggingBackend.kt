package me.spjere.appcore.android.logging

import me.sphere.logging.LoggingBackend
import me.sphere.logging.LoggingLevel
import timber.log.Timber

class AppCoreLoggingBackend() : LoggingBackend {

    override fun log(level: LoggingLevel, message: String) {
        when (level) {
            LoggingLevel.Error -> Timber.e("AppCore $message")
            LoggingLevel.Info -> Timber.i("AppCore $message")
        }
    }

    override fun log(level: LoggingLevel, exception: Throwable) {
        when (level) {
            LoggingLevel.Error -> {
                Timber.e("AppCore")
                Timber.e(exception)
            }
            LoggingLevel.Info -> Timber.i(exception, "AppCore")
        }
    }

    override fun logMetric(name: String, value: Long) {
        Timber.i("AppCore - Metric: $name - $value")
    }
}
