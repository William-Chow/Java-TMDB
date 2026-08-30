package com.movie.tmdb.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    @Query("SELECT * FROM cached_movies WHERE category = :category ORDER BY position")
    fun observeCategory(category: String): Flow<List<CachedMovieEntity>>

    @Query("SELECT COUNT(*) FROM cached_movies WHERE category = :category")
    suspend fun countFor(category: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movies: List<CachedMovieEntity>)

    @Query("DELETE FROM cached_movies WHERE category = :category")
    suspend fun clearCategory(category: String)

    /** Page 1 is a full refresh: anything the server dropped must not linger. */
    @Transaction
    suspend fun replaceCategory(category: String, movies: List<CachedMovieEntity>) {
        clearCategory(category)
        insertAll(movies)
    }
}

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<FavoriteEntity>>

    @Query("SELECT id FROM favorites")
    fun observeIds(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun remove(id: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    suspend fun isFavorite(id: Int): Boolean
}
