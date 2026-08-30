package com.movie.tmdb.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CachedMovieEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TmdbDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao

    abstract fun favoriteDao(): FavoriteDao

    companion object {
        fun build(context: Context): TmdbDatabase =
            Room.databaseBuilder(context, TmdbDatabase::class.java, "tmdb.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
