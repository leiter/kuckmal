package cut.the.crap.shared.data

import io.ktor.client.HttpClient

/**
 * Platform-specific HTTP client factory.
 * Each platform provides its own engine (OkHttp for JVM, Darwin for iOS).
 */
expect object HttpClientFactory {
    /**
     * Create a configured HTTP client for the current platform.
     */
    fun create(): HttpClient
}
