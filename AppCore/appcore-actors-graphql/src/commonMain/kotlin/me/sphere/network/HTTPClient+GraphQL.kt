package me.sphere.network

import com.apollographql.apollo.api.*
import me.sphere.appcore.utils.*
import okio.Buffer
import kotlin.native.concurrent.SharedImmutable

@SharedImmutable
private val graphQLMappers = ScalarTypeAdapters(
    emptyMap()
).freeze()

internal suspend fun <D : Operation.Data, T, V : Operation.Variables> HTTPClient.requestGraphQL(
    operation: Operation<D, T, V>
): T {

    val body = operation.composeRequestBody(graphQLMappers).utf8()
    val httpRequest = HTTPRequest(HTTPRequest.Method.POST, API("/graphql"), null, null, body)
    return request(
        request = httpRequest,
        requestMapper = { it },
        responseMapper = {
            val buffer = Buffer()
            buffer.writeUtf8(it)

            val response = operation.parse(buffer, graphQLMappers)

            if (response.errors != null) {
                throw GraphQLHTTPError(response.errors)
            } else {
                response.data ?: throw GraphQLHTTPError(null)
            }
        }
    )
}
