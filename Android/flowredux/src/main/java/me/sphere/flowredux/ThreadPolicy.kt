package me.sphere.flowredux

import android.os.Looper

/**
 * This is a helper class to allow us to specify different threading strategies
 * based on each platform, android and JVM are ony supported for now
 */
internal interface ThreadPolicy {

    /**
     * Returns true if this operation is happening on the main thread
     */
    fun isMainThread(): Boolean
}

/**
 * Check if we are on the main android thread
 */
private object AndroidThreadPolicy : ThreadPolicy {
    override fun isMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }
}

/**
 * For the JVM always return true
 * This is used only for the unit tests
 */
private object JvmThreadPolicy : ThreadPolicy {
    override fun isMainThread(): Boolean {
        return true
    }
}

internal object DelegateThreadPolicy : ThreadPolicy {

    private val delegate: ThreadPolicy by lazy { getThreadPolicy() }

    override fun isMainThread() = delegate.isMainThread()

    private fun getThreadPolicy(): ThreadPolicy {
        // When running tests locally or from IDE this is going to return false
        val isDalvik = "Dalvik" == System.getProperty("java.vm.name")
        val isAndroid = isDalvik || !BuildConfig.DEBUG
        return if (isAndroid) {
            AndroidThreadPolicy
        } else {
            JvmThreadPolicy
        }
    }
}
