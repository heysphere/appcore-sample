package me.sphere.models.operations

import kotlinx.serialization.Serializable

@Serializable
enum class OperationStatus {
    Idle, Started, Success, Failure, Suspended
}
