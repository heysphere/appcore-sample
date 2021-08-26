package me.sphere.appcore


interface SphereStore {
    val isActive: Boolean

    fun destroy()
    fun close()
}
