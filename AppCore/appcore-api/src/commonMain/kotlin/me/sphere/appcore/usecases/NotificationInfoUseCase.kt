package me.sphere.appcore.usecases

import me.sphere.appcore.dataSource.DataSource

interface NotificationInfoUseCase {
    fun info(id: String): DataSource<NotificationInfo>
}

data class NotificationInfo(
    val notificationId: String,
    val reason: String,
    val title: String,
    val repositoryName: String,
    val subjectId: String
)
