package me.sphere.sqldelight.operations

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class OperationErrorCode(val rawValue: String) {
    companion object {
        val DEFAULT get() = OperationErrorCode("ERROR")
    }
}

class OperationUnexpectedStateException(val type: String, val reason: String) :
    RuntimeException("Operation (type=$type) is in an unexpected state: $reason")

class OperationFailedException(val type: String, val id: Long, val code: OperationErrorCode, override val messageForHuman: String?) :
    RuntimeException("Operation (type=$type, id=$id) has failed: $messageForHuman"), HumanReadableException

class OperationSuspendedException(val type: String, val id: Long) :
    RuntimeException("Operation (type=$type, id=$id) has been suspended and awaiting the next resumption opportunity.")

interface HumanReadableException {
    val messageForHuman: String?
}
