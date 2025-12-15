package com.mediathekview.android.repository

import com.mediathekview.shared.repository.MediaRepository as SharedMediaRepository

/**
 * Android-specific MediaRepository interface
 * Extends the shared KMP interface and adds Android-specific methods
 */
interface MediaRepository : SharedMediaRepository {

    /**
     * Get cache statistics for monitoring performance
     * @return Cache statistics including hit rate, size, etc.
     */
    fun getCacheStats(): SearchCache.CacheStats
}
