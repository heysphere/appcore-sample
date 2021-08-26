package me.sphere.models

import kotlinx.serialization.Serializable

@Serializable
sealed class ValidatedUrl

@Serializable
data class HttpsUrl @Throws(IllegalArgumentException::class) constructor(val value: String): ValidatedUrl() {
    init { require(value.maybeHttpsUrl) { "HttpsUrl accepts only file or HTTPS URL." } }
}

@Serializable
data class FileUrl @Throws(IllegalArgumentException::class) constructor(val value: String): ValidatedUrl() {
    init { require(value.maybeFileUrl) { "FileUrl accepts only file URL." } }
}

fun ValidatedUrl(urlString: String): ValidatedUrl? = when {
    urlString.maybeFileUrl -> runCatching { FileUrl(urlString) }.getOrNull()
    else -> null
}

expect val PLATFORM_FILE_PREFIX: String

private val String.maybeFileUrl: Boolean get() = startsWith(PLATFORM_FILE_PREFIX)
private val String.maybeHttpsUrl: Boolean get() = startsWith("https://")
