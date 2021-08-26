package me.sphere.network

import com.apollographql.apollo.api.Error
import me.sphere.sqldelight.operations.HumanReadableException

internal data class GraphQLHTTPError(
    val errors: List<Error>?
): Throwable(), HumanReadableException {
    override fun toString(): String = """
        GraphQL errors:
        ${errors?.map { it.message }?.joinToString(separator = "\n", prefix = "- ")}
    """.trimIndent()

    override val messageForHuman: String?
        get() = errors?.firstOrNull()?.message
}
