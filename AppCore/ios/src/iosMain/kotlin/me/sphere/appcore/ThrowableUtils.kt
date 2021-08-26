package me.sphere.appcore

import me.sphere.appcore.utils.freeze
import me.sphere.appcore.utils.frozenLambda
import platform.Foundation.NSException

val Throwable.name: String
    get() = this::class.simpleName!!

val Throwable.kotlinDescription: String
    get() = toString()

/**
 * Rethrow Kotlin uncaught exceptions as NSException, so that crash reporting can process it.
 */
fun installAppCoreUncaughtExceptionHandler() {
    val original = setUnhandledExceptionHook(
        frozenLambda<Throwable, Unit> {
            it.printStackTrace()
            AppCoreKotlinException(it).raise()
        }
    )

    check(original == null)
}

class AppCoreKotlinException(throwable: Throwable)
    : NSException("me.sphere.appcore.${throwable.name}", throwable.message, null)
{
    private val returnAddresses: List<Long>
    private val symbols: Array<String>

    init {
        returnAddresses = throwable.getStackTraceAddresses()
        symbols = throwable.getStackTrace()
        freeze()
    }

    override fun callStackReturnAddresses(): List<*> = returnAddresses
    override fun callStackSymbols(): List<*> = symbols.toList()
}
