package me.sphere.appcore.utils

import kotlinx.datetime.Instant

actual fun Instant.Companion.fromBlob(bytes: ByteArray): Instant = fromBlobOkio(bytes)
actual fun Instant.toBlob(): ByteArray = toBlobOkio()
