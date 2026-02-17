package cut.the.crap.shared.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a favorite or watch later entry.
 * Used for saving content for later viewing.
 */
@Entity(
    tableName = "favorite_entries",
    indices = [
        Index(value = ["channel", "theme", "title"], unique = true),
        Index(value = ["listType"]),
        Index(value = ["addedAt"])
    ]
)
data class FavoriteEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Media identification (matches MediaEntry)
    val channel: String,
    val theme: String,
    val title: String,

    // Timestamp when added to favorites
    val addedAt: Long,

    // List type: "favorite" or "watchLater"
    val listType: String = "favorite"
)
