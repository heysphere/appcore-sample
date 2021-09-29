package me.sphere.appcore

import me.sphere.appcore.usecases.NotificationInfoUseCase
import me.sphere.appcore.usecases.NotificationListUseCase


interface SphereStore {
    val isActive: Boolean

    val notificationListUseCase: NotificationListUseCase
    val notificationInfoUseCase: NotificationInfoUseCase

    fun destroy()
    fun close()
}
