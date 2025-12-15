package cut.the.crap.android.repository

import android.util.LruCache
import cut.the.crap.shared.database.MediaEntry

/**
 * LRU cache for search results with expiration time
 * Reduces database queries for repeated searches
 */
class SearchCache {

    companion object {
        private const val MAX_CACHE_SIZE = 50 // Number of search results to cache
        private const val CACHE_EXPIRY_MS = 5 * 60 * 1000L // 5 minutes in milliseconds
    }

    /**
     * Cache entry with timestamp for expiration
     */
    data class CacheEntry(
        val results: List<MediaEntry>,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS
        }
    }

    // LRU cache for search results
    private val cache = LruCache<String, CacheEntry>(MAX_CACHE_SIZE)

    /**
     * Generate cache key from search parameters
     */
    fun generateKey(
        query: String,
        channel: String? = null,
        theme: String? = null,
        limit: Int,
        offset: Int = 0
    ): String {
        return buildString {
            append(query.lowercase())
            append("|channel=${channel ?: "all"}")
            append("|theme=${theme ?: "all"}")
            append("|limit=$limit")
            append("|offset=$offset")
        }
    }

    /**
     * Get cached search results if available and not expired
     */
    fun get(key: String): List<MediaEntry>? {
        val entry = cache.get(key)
        return if (entry != null && !entry.isExpired()) {
            entry.results
        } else {
            // Remove expired entry
            if (entry != null) {
                cache.remove(key)
            }
            null
        }
    }

    /**
     * Store search results in cache
     */
    fun put(key: String, results: List<MediaEntry>) {
        cache.put(key, CacheEntry(results))
    }

    /**
     * Clear all cached entries
     */
    fun clear() {
        cache.evictAll()
    }

    /**
     * Get cache statistics for monitoring
     */
    fun getStats(): CacheStats {
        return CacheStats(
            hitCount = cache.hitCount(),
            missCount = cache.missCount(),
            size = cache.size(),
            maxSize = cache.maxSize()
        )
    }

    data class CacheStats(
        val hitCount: Int,
        val missCount: Int,
        val size: Int,
        val maxSize: Int
    ) {
        val hitRate: Float = if (hitCount + missCount > 0) {
            hitCount.toFloat() / (hitCount + missCount)
        } else 0f
    }
}