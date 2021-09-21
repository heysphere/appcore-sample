package me.sphere.appcore.rest.notifications

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import me.sphere.logging.Logger
import me.sphere.network.*
import me.sphere.sqldelight.SqlDatabaseGateway
import me.sphere.sqldelight.StoreScope
import me.sphere.sqldelight.operations.PagingReconciliationActor
import me.sphere.sqldelight.operations.notificaitons.NotificationReconciliation

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
            resource = Absolute("https://api.github.com/notifications"),
            urlQuery = mapOf(
                "page" to context.start.toString(),
                "per_page" to context.pageSize.toString(),
            ),
            headers = mapOf(
                "Authorization" to "Bearer ${storeScope.gitHubAccessToken}"
            ),
            body = ""
        )

        val result = httpClient.request(
            request,
            requestSerializationStrategy = String.serializer(),
            responseSerializationStrategy = ListSerializer(APINotification.serializer())
        )

        val ids = result.map { ID(it.id) }
        // TODO Save to db

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