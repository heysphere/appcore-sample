package me.sphere.models

import com.squareup.sqldelight.ColumnAdapter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

internal abstract class JsonObjectAdapter<T: Any>(
    private val serializer: KSerializer<T>
): ColumnAdapter<T, String> {
    override fun decode(databaseValue: String): T
        = Json.decodeFromString(serializer, databaseValue)

    override fun encode(value: T): String
        = Json.encodeToString(serializer, value)
}
