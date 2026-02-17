package cut.the.crap.shared.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * Entry in the cache with value and timestamp
 */
data class CacheEntry<T>(
    val value: T,
    val timestamp: Long,
    val expiresAt: Long
)

/**
 * tvOS-optimized cache for API responses.
 * Provides in-memory caching with TTL support for offline functionality.
 *
 * @param defaultTtlMs Default time-to-live for cache entries in milliseconds (default: 5 minutes)
 * @param maxEntries Maximum number of entries to keep in cache (default: 100)
 */
class TvosCache<T>(
    private val defaultTtlMs: Long = 5 * 60 * 1000, // 5 minutes
    private val maxEntries: Int = 100
) {
    private val cache = mutableMapOf<String, CacheEntry<T>>()
    private val mutex = Mutex()

    /**
     * Get current time in milliseconds
     */
    private fun currentTimeMs(): Long =
        (NSDate().timeIntervalSince1970 * 1000).toLong()

    /**
     * Get a value from cache if it exists and is not expired
     * @param key Cache key
     * @return Cached value or null if not found/expired
     */
    suspend fun get(key: String): T? = mutex.withLock {
        val entry = cache[key] ?: return@withLock null

        if (isExpired(entry)) {
            cache.remove(key)
            return@withLock null
        }

        entry.value
    }

    /**
     * Get a value from cache even if expired (for offline fallback)
     * @param key Cache key
     * @return Cached value or null if not found
     */
    suspend fun getStale(key: String): T? = mutex.withLock {
        cache[key]?.value
    }

    /**
     * Put a value in the cache
     * @param key Cache key
     * @param value Value to cache
     * @param ttlMs Time-to-live in milliseconds (uses default if not specified)
     */
    suspend fun put(key: String, value: T, ttlMs: Long = defaultTtlMs) = mutex.withLock {
        val now = currentTimeMs()

        // Evict oldest entries if at capacity
        if (cache.size >= maxEntries && !cache.containsKey(key)) {
            evictOldest()
        }

        cache[key] = CacheEntry(
            value = value,
            timestamp = now,
            expiresAt = now + ttlMs
        )
    }

    /**
     * Check if an entry is expired
     */
    fun isExpired(entry: CacheEntry<T>): Boolean {
        return currentTimeMs() > entry.expiresAt
    }

    /**
     * Check if cache contains a valid (non-expired) entry for key
     */
    suspend fun contains(key: String): Boolean = mutex.withLock {
        val entry = cache[key] ?: return@withLock false
        !isExpired(entry)
    }

    /**
     * Remove a specific entry from cache
     */
    suspend fun remove(key: String) = mutex.withLock {
        cache.remove(key)
    }

    /**
     * Clear all entries from cache
     */
    suspend fun clear() = mutex.withLock {
        cache.clear()
    }

    /**
     * Get the number of entries in cache
     */
    suspend fun size(): Int = mutex.withLock {
        cache.size
    }

    /**
     * Remove expired entries
     */
    suspend fun evictExpired() = mutex.withLock {
        val now = currentTimeMs()
        cache.entries.removeAll { now > it.value.expiresAt }
    }

    /**
     * Evict the oldest entry to make room for new ones
     */
    private fun evictOldest() {
        val oldest = cache.entries.minByOrNull { it.value.timestamp }
        oldest?.let { cache.remove(it.key) }
    }

    /**
     * Get cache statistics
     */
    suspend fun getStats(): CacheStats = mutex.withLock {
        val now = currentTimeMs()
        val validEntries = cache.values.count { now <= it.expiresAt }
        val expiredEntries = cache.size - validEntries

        CacheStats(
            totalEntries = cache.size,
            validEntries = validEntries,
            expiredEntries = expiredEntries,
            oldestTimestamp = cache.values.minOfOrNull { it.timestamp },
            newestTimestamp = cache.values.maxOfOrNull { it.timestamp }
        )
    }

    /**
     * Cache statistics
     */
    data class CacheStats(
        val totalEntries: Int,
        val validEntries: Int,
        val expiredEntries: Int,
        val oldestTimestamp: Long?,
        val newestTimestamp: Long?
    )
}

/**
 * Specialized string cache for API responses
 */
typealias TvosApiCache = TvosCache<String>

/**
 * Create a cache key from endpoint and parameters
 */
fun createCacheKey(endpoint: String, params: Map<String, Any?> = emptyMap()): String {
    if (params.isEmpty()) return endpoint

    val sortedParams = params.entries
        .filter { it.value != null }
        .sortedBy { it.key }
        .joinToString("&") { "${it.key}=${it.value}" }

    return "$endpoint?$sortedParams"
}
