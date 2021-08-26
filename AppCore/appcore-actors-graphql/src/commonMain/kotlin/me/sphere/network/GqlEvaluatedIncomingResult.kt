package me.sphere.network

import kotlinx.serialization.json.JsonObject

internal sealed class GqlEvaluatedIncomingResult {
    class Data(val data: JsonObject): GqlEvaluatedIncomingResult()
    class Error(val error: Throwable): GqlEvaluatedIncomingResult()
    object Completed: GqlEvaluatedIncomingResult()
}
