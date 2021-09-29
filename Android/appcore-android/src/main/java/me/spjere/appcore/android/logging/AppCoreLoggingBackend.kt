package me.spjere.appcore.android.logging

import android.util.Log
import me.sphere.logging.LoggingBackend
import me.sphere.logging.LoggingLevel
import timber.log.Timber

class AppCoreLoggingBackend : LoggingBackend {

    override fun log(level: LoggingLevel, message: String) {
        when (level) {
            LoggingLevel.Error -> Log.e("AppCore","AppCore $message")
            LoggingLevel.Info -> Log.i("AppCore","AppCore $message")
        }
    }

    override fun log(level: LoggingLevel, exception: Throwable) {
        when (level) {
            LoggingLevel.Error -> {
                Log.e("AppCore", "")
                Log.e("AppCore", exception.toString())
            }
            LoggingLevel.Info -> Log.i("AppCore", exception.toString())
        }
    }

    override fun logMetric(name: String, value: Long) {
        Timber.i("AppCore - Metric: $name - $value")
    }
}
