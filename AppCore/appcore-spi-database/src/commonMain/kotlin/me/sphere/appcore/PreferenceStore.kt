package me.sphere.appcore

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

interface PreferenceStore {
    suspend fun get(key: String): String?
    suspend fun set(key: String, value: String?)
}

suspend fun <T> PreferenceStore.get(deserializer: DeserializationStrategy<T>, key: String): T? {
    return get(key = key)?.let { Json.decodeFromString(deserializer, it) }
}

suspend fun <T> PreferenceStore.set(serializer: SerializationStrategy<T>, key: String, value: T?) {
    set(key, value?.let { Json.encodeToString(serializer, it) })
}
