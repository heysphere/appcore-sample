package me.sphere.sqldelight.operations

import kotlinx.serialization.Serializable

@Serializable
internal data class OperationErrorDigest(
    val code: OperationErrorCode = OperationErrorCode.DEFAULT,
    val message: String?
)
