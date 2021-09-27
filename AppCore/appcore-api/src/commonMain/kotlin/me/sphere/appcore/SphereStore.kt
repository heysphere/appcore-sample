package me.sphere.appcore

import me.sphere.appcore.usecases.NotificationListUseCase


interface SphereStore {
    val isActive: Boolean

    val notificationListUseCase: NotificationListUseCase

    fun destroy()
    fun close()
}
