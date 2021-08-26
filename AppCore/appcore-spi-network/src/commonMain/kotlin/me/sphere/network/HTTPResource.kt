package me.sphere.network

import kotlin.js.JsExport

@JsExport
sealed class HTTPResource {
    override fun toString(): String = when (this) {
        is Absolute -> this.url
        is API -> "\${API_ROOT}{$path}"
    }
}

@JsExport
data class Absolute(val url: String): HTTPResource()

@JsExport
data class API(val path: String): HTTPResource()
