package com.movie.tmdb.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.movie.tmdb.app.model.Movie

/**
 * The [Movie] fields a list card needs, shared by both tables. Embedded without a prefix, so the
 * columns keep their v1 names; `id` stays on each entity because it is part of that table's key.
 */
data class MovieSummary(
    val title: String?,
    val originalTitle: String?,
    val posterPath: String?,
    val overview: String?,
    val releaseDate: String?,
    val voteAverage: Double?,
    val voteCount: Int
)

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
    @Embedded val summary: MovieSummary
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: Int,
    @Embedded val summary: MovieSummary,
    val addedAt: Long
)

fun CachedMovieEntity.toMovie() = summary.toMovie(id)

fun FavoriteEntity.toMovie() = summary.toMovie(id)

fun Movie.toCacheEntity(category: String, position: Int, page: Int) = CachedMovieEntity(
    category = category,
    id = id,
    position = position,
    page = page,
    summary = toSummary()
)

fun Movie.toFavoriteEntity(addedAt: Long) = FavoriteEntity(id = id, summary = toSummary(), addedAt = addedAt)

private fun Movie.toSummary() = MovieSummary(
    title = title,
    originalTitle = originalTitle,
    posterPath = posterPath,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount
)

private fun MovieSummary.toMovie(id: Int) = Movie(
    id = id,
    title = title,
    originalTitle = originalTitle,
    posterPath = posterPath,
    overview = overview,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    voteCount = voteCount
)
