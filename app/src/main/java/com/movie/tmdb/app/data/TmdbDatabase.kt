package com.movie.tmdb.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * There is deliberately no destructive fallback: favorites are user data, so every version bump
 * needs a Migration (or an AutoMigration here) built against the schemas exported to app/schemas.
 * cached_movies is only a cache, so a migration that changes it may simply drop and recreate it.
 */
@Database(
    entities = [CachedMovieEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = true
)
abstract class TmdbDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao

    abstract fun favoriteDao(): FavoriteDao

    companion object {
        fun build(context: Context): TmdbDatabase =
            Room.databaseBuilder(context, TmdbDatabase::class.java, "tmdb.db").build()
    }
}
