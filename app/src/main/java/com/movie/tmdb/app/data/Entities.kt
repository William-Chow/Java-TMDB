package com.movie.tmdb.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.movie.tmdb.app.model.Movie

/**
 * One page-ordered slice of a TMDB list, kept so the app opens with content while offline.
 * [position] preserves the server's ordering, which neither id nor rating reproduces.
 */
@Entity(tableName = "cached_movies", primaryKeys = ["category", "id"])
data class CachedMovieEntity(
    val category: String,
    val id: Int,
    val position: Int,
    val page: Int,
    val title: String?,
    val originalTitle: String?,
    val posterPath: String?,
    val overview: String?,
    val releaseDate: String?,
    val voteAverage: Double?,
    val voteCount: Int
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: Int,
    val title: String?,
    val originalTitle: String?,
    val posterPath: String?,
    val overview: String?,
    val releaseDate: String?,
    val voteAverage: Double?,
    val voteCount: Int,
    val addedAt: Long
)

fun CachedMovieEntity.toMovie() = Movie(
    id = id,
    title = title,
    originalTitle = originalTitle,
    posterPath = posterPath,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount
)

fun Movie.toCacheEntity(category: String, position: Int, page: Int) = CachedMovieEntity(
    category = category,
    id = id,
    position = position,
    page = page,
    title = title,
    originalTitle = originalTitle,
    posterPath = posterPath,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount
)

fun FavoriteEntity.toMovie() = Movie(
    id = id,
    title = title,
    originalTitle = originalTitle,
    posterPath = posterPath,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount
)

fun Movie.toFavoriteEntity(addedAt: Long) = FavoriteEntity(
    id = id,
    title = title,
    originalTitle = originalTitle,
    posterPath = posterPath,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount,
    addedAt = addedAt
)
