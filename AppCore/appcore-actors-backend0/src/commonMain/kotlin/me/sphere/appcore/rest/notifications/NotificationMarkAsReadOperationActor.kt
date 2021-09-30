package me.sphere.appcore.rest.notifications

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import me.sphere.appcore.rest.optimisticAction
import me.sphere.logging.Logger
import me.sphere.network.API
import me.sphere.network.HTTPClient
import me.sphere.network.HTTPRequest
import me.sphere.network.request
import me.sphere.sqldelight.SqlDatabaseGateway
import me.sphere.sqldelight.StoreScope
import me.sphere.sqldelight.operations.OperationStoreActorBase
import me.sphere.sqldelight.operations.notifications.NotificationMarkAsReadOperation

class NotificationMarkAsReadOperationActor(
    private val httpClient: HTTPClient,
    private val storeScope: StoreScope,
    database: SqlDatabaseGateway,
    logger: Logger
) : OperationStoreActorBase<NotificationMarkAsReadOperation.Input, Unit>(
    database,
    logger,
    storeScope
) {
    override val definition = NotificationMarkAsReadOperation

    override suspend fun perform(input: NotificationMarkAsReadOperation.Input) {
        database.optimisticAction {
            optimisticallyApply {
                database.notificationQueries.markAsReadOptimistically(
                    isRead = true,
                    input.notificationId
                )
            }
            runAsync {
                val request = HTTPRequest<Unit>(
                    method = HTTPRequest.Method.PATCH,
                    resource = API("notifications/threads/${input.notificationId}"),
                    urlQuery = null,
                    headers = mapOf(
                        "Authorization" to "Bearer ${storeScope.gitHubAccessToken}"
                    ),
                    body = null
                )
                httpClient.request(request)
            }

            onSuccess {
                database.notificationQueries.markAsRead(isUnRead = false, input.notificationId)
            }

            onFailure {
                database.notificationQueries.markAsReadOptimistically(
                    isRead = false,
                    input.notificationId
                )
            }
        }
    }
}