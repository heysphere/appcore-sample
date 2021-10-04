package me.sphere.appcore.rest.notifications

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import me.sphere.logging.Logger
import me.sphere.network.*
import me.sphere.sqldelight.SqlDatabaseGateway
import me.sphere.sqldelight.StoreScope
import me.sphere.sqldelight.operations.PagingReconciliationActor
import me.sphere.sqldelight.operations.notifications.NotificationReconciliation

internal class NotificationReconciliationActor(
    private val httpClient: HTTPClient,
    private val storeScope: StoreScope,
    database: SqlDatabaseGateway,
    logger: Logger,
) : PagingReconciliationActor(database, logger, storeScope) {
    override val definition = NotificationReconciliation
    override suspend fun fetch(context: FetchContext): FetchResult {
        val request = HTTPRequest(
            method = HTTPRequest.Method.GET,
            resource = API("notifications"),
            urlQuery = mapOf(
                "page" to (context.start / context.pageSize).toString(),
                "per_page" to context.pageSize.toString(),
            ),
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

@Serializable
internal data class APINotification(
    val id: String,
    val unread: Boolean,
    val reason: String,
    val subject: NotificationSubject,
    val repository: Repository
)

@Serializable
data class Repository(
    val full_name: String
)

@Serializable
internal data class NotificationSubject(
    val title: String,
    val url: String
)
