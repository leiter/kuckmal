package cut.the.crap.shared.data

import io.ktor.client.HttpClient

/**
 * Platform-specific HTTP client factory.
 */
expect object HttpClientFactory {
    fun create(): HttpClient
}
