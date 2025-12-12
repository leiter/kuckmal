package com.mediathekview.android.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for MediathekView
 * Instance managed by Koin dependency injection
 */
@Database(
    entities = [MediaEntry::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
}
