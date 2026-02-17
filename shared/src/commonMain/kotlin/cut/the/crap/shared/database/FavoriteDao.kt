package cut.the.crap.shared.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for favorite entries.
 * Handles favorites and watch later lists.
 */
@Dao
interface FavoriteDao {

    /**
     * Insert or replace a favorite entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntry): Long

    /**
     * Delete a favorite entry by its media identification
     */
    @Query("DELETE FROM favorite_entries WHERE channel = :channel AND theme = :theme AND title = :title")
    suspend fun delete(channel: String, theme: String, title: String)

    /**
     * Delete a favorite entry by its media identification and list type
     */
    @Query("DELETE FROM favorite_entries WHERE channel = :channel AND theme = :theme AND title = :title AND listType = :listType")
    suspend fun deleteByType(channel: String, theme: String, title: String, listType: String)

    /**
     * Get all favorites of a specific type as Flow (reactive)
     */
    @Query("SELECT * FROM favorite_entries WHERE listType = :listType ORDER BY addedAt DESC")
    fun getAllByType(listType: String): Flow<List<FavoriteEntry>>

    /**
     * Get all favorites (both types) as Flow (reactive)
     */
    @Query("SELECT * FROM favorite_entries ORDER BY addedAt DESC")
    fun getAll(): Flow<List<FavoriteEntry>>

    /**
     * Check if an entry is in favorites (any type) as Flow (reactive)
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_entries WHERE channel = :channel AND theme = :theme AND title = :title)")
    fun isFavorite(channel: String, theme: String, title: String): Flow<Boolean>

    /**
     * Check if an entry is in a specific list type as Flow (reactive)
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_entries WHERE channel = :channel AND theme = :theme AND title = :title AND listType = :listType)")
    fun isFavoriteByType(channel: String, theme: String, title: String, listType: String): Flow<Boolean>

    /**
     * Get the list type for an entry (null if not in any list)
     */
    @Query("SELECT listType FROM favorite_entries WHERE channel = :channel AND theme = :theme AND title = :title LIMIT 1")
    suspend fun getListType(channel: String, theme: String, title: String): String?

    /**
     * Get count of favorites by type
     */
    @Query("SELECT COUNT(*) FROM favorite_entries WHERE listType = :listType")
    fun getCountByType(listType: String): Flow<Int>

    /**
     * Delete all favorites
     */
    @Query("DELETE FROM favorite_entries")
    suspend fun deleteAll()

    /**
     * Delete all favorites of a specific type
     */
    @Query("DELETE FROM favorite_entries WHERE listType = :listType")
    suspend fun deleteAllByType(listType: String)
}
