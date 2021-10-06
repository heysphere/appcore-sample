package me.sphere.appcore.usecases

import me.sphere.appcore.dataSource.PagingDataSource

interface NotificationListUseCase {
    fun notifications(shouldShowAll: Boolean): PagingDataSource<Notification>
}

data class Notification(
    val notificationId: String,
    val unread: Boolean,
    val title: String,
    val repositoryName: String,
    val subjectId: String
)