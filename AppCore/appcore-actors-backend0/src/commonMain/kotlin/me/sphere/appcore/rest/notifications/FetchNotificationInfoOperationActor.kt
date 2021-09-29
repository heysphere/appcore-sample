package me.sphere.appcore.rest.notifications

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import me.sphere.logging.Logger
import me.sphere.network.API
import me.sphere.network.HTTPClient
import me.sphere.network.HTTPRequest
import me.sphere.network.request
import me.sphere.sqldelight.SqlDatabaseGateway
import me.sphere.sqldelight.StoreScope
import me.sphere.sqldelight.operations.OperationStoreActorBase
import me.sphere.sqldelight.operations.notifications.FetchNotificationInfoOperation

internal class FetchNotificationInfoOperationActor(
    private val httpClient: HTTPClient,
    private val storeScope: StoreScope,
    database: SqlDatabaseGateway,
    logger: Logger
) : OperationStoreActorBase<FetchNotificationInfoOperation.Input, FetchNotificationInfoOperation.Output>(database, logger, storeScope) {
    override val definition = FetchNotificationInfoOperation

    override suspend fun perform(input: FetchNotificationInfoOperation.Input): FetchNotificationInfoOperation.Output {
        val request = HTTPRequest(
            method = HTTPRequest.Method.GET,
            resource = API("threads/${input.notificationId}"),
            urlQuery = null,
            headers = mapOf(
                "Authorization" to "Bearer ${storeScope.gitHubAccessToken}"
            ),
            body = null
        )
        val result = httpClient.request(
            request,
            requestSerializationStrategy = String.serializer(),
            responseSerializationStrategy = APINotification.serializer(),
            json = Json { ignoreUnknownKeys = true }
        )

        database.transaction {
            val subjectId = result.subject.url.split('/').last()

            database.notificationQueries.upsert(
                id = result.id,
                unread = result.unread,
                reason = result.reason,
                title = result.subject.title,
                url = result.subject.url,
                repositoryFullName = result.repository.full_name,
                subjectId = subjectId
            )
        }

        return FetchNotificationInfoOperation.Output(result.id)
    }
}
