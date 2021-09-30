package me.sphere.appcore

import me.sphere.appcore.usecases.NotificationActionUseCase
import me.sphere.appcore.usecases.NotificationInfoUseCase
import me.sphere.appcore.usecases.NotificationListUseCase


interface SphereStore {
    val isActive: Boolean

    val notificationListUseCase: NotificationListUseCase
    val notificationInfoUseCase: NotificationInfoUseCase
    val notificationActionUseCase: NotificationActionUseCase

    fun destroy()
    fun close()
}
