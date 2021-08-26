package me.sphere.graphql

import com.apollographql.apollo.api.*
import kotlinx.datetime.Instant

internal class InstantGraphQLAdapter: CustomTypeAdapter<Instant> {
    override fun decode(value: CustomTypeValue<*>): Instant
        = when (value) {
            is CustomTypeValue.GraphQLString -> Instant.parse(value.value)
            else -> throw RuntimeException("Expected a string in a GraphQL Date field. Found invalid data.")
        }

    override fun encode(value: Instant): CustomTypeValue<*>
        = CustomTypeValue.GraphQLString(value.toString())
}
