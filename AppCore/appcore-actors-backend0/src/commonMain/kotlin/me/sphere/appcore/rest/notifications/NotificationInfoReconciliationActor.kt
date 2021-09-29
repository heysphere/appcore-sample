package me.sphere.appcore.rest.notifications

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import me.sphere.logging.Logger
import me.sphere.network.API
import me.sphere.network.HTTPClient
import me.sphere.network.HTTPRequest
import me.sphere.network.request
import me.sphere.sqldelight.SqlDatabaseGateway
import me.sphere.sqldelight.StoreScope
import me.sphere.sqldelight.operations.PagingReconciliationActor
import me.sphere.sqldelight.operations.notifications.NotificationInfoReconciliation

internal class NotificationInfoReconciliationActor (
    private val httpClient: HTTPClient,
    private val storeScope: StoreScope,
    database: SqlDatabaseGateway,
    logger: Logger,
) : PagingReconciliationActor(database, logger, storeScope) {
    override val definition = NotificationInfoReconciliation

    override suspend fun fetch(context: FetchContext): FetchResult {
        val request = HTTPRequest(
            method = HTTPRequest.Method.GET,
            resource = API("threads"), // TODO use ID of notification
            urlQuery = null,
            headers = mapOf(
                "Authorization" to "Bearer ${storeScope.gitHubAccessToken}"
            ),
            body = null
        )
        val result = httpClient.request(
            request,
            requestSerializationStrategy = String.serializer(),
            responseSerializationStrategy = ListSerializer(APINotification.serializer()),
            json = Json { ignoreUnknownKeys = true }
        )

        database.transaction {
            result.forEach {
                val subjectId = it.subject.url.split('/').last()

                database.notificationQueries.upsert(
                    id = it.id,
                    unread = it.unread,
                    reason = it.reason,
                    title = it.subject.title,
                    url = it.subject.url,
                    repositoryFullName = it.repository.full_name,
                    subjectId = subjectId
                )
            }
        }

        val ids = result.map { ID(it.id) }
        return FetchResult.Success(ids)
    }
}
