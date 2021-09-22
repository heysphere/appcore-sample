package me.sphere.appcore.share

import me.sphere.appcore.ClientBase
import me.sphere.appcore.SqlDatabaseProvider
import me.sphere.appcore.utils.freeze
import me.sphere.logging.Logger
import me.sphere.models.AgentId
import me.sphere.models.BackendEnvironmentType
import me.sphere.sqldelight.StoreActorBuilder
import me.sphere.sqldelight.StoreClientType

data class ShareClientBuilder(
    val environmentType: BackendEnvironmentType,
    val storeActorBuilders: List<StoreActorBuilder>,
    val sqlDatabaseProvider: SqlDatabaseProvider,
    val logger: Logger
)

fun ShareClientBuilder.makeClient(gitHubAccessToken: String): ShareClient {
    return object : ClientBase(
        StoreClientType.Share,
        environmentType,
        gitHubAccessToken,
        storeActorBuilders,
        sqlDatabaseProvider,
        logger
    ), ShareClient {
        init { freeze() }
    }
}
