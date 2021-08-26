package me.sphere.network

/**
 * Represent an HTTP client that performs HTTP requests on behalf of a valid Sphere agent.
 *
 * Implementations should:
 * 1. upon 401 Unauthorized, transparently attempt to retrieve a new token, and retry the HTTP request.
 * 2. throw `HTTPError.Unauthorized` if the retry is futile.
 */
interface AgentHTTPClient: HTTPClient {
    /**
     * Get a fresh auth token of the current agent.
     *
     * @throws [NoCredentialError] if there is no valid agent.
     */
    suspend fun agentAuthToken(): String
}
