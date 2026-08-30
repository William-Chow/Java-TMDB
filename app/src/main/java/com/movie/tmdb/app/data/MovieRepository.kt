package com.movie.tmdb.app.data

import com.movie.tmdb.app.BuildConfig
import com.movie.tmdb.app.model.Movie
import com.movie.tmdb.app.model.MovieCategory
import com.movie.tmdb.app.model.Result
import com.movie.tmdb.app.network.APIInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Categories are served from Room and refreshed from the network, so the list survives
 * going offline. Search results are transient and never cached.
 */
class MovieRepository(
    private val api: APIInterface,
    private val movieDao: MovieDao,
    private val favoriteDao: FavoriteDao,
    /** Supplied by `tmdb.apiKey` in the git-ignored local.properties. */
    private val apiKey: String = BuildConfig.TMDB_API_KEY,
    private val now: () -> Long = System::currentTimeMillis
) {

    fun observeCategory(category: MovieCategory): Flow<List<Movie>> =
        movieDao.observeCategory(category.path).map { entities -> entities.map(CachedMovieEntity::toMovie) }

    suspend fun cachedCount(category: MovieCategory): Int = movieDao.countFor(category.path)

    /** Fetches one page, writes it through to the cache, and returns the total page count. */
    suspend fun loadCategoryPage(category: MovieCategory, page: Int): Int {
        val result = api.getMovieList(category.path, apiKey, page)
        val entities = result.results.orEmpty().mapIndexed { index, movie ->
            movie.toCacheEntity(category.path, (page - 1) * PAGE_SIZE + index, page)
        }
        if (page == 1) {
            movieDao.replaceCategory(category.path, entities)
        } else {
            movieDao.insertAll(entities)
        }
        return result.totalPages
    }

    suspend fun search(query: String, page: Int): Result = api.getSearch(apiKey, query, page)

    suspend fun movieDetail(movieId: Int): Movie = api.getMovie(movieId, apiKey)

    fun observeFavorites(): Flow<List<Movie>> =
        favoriteDao.observeAll().map { entities -> entities.map(FavoriteEntity::toMovie) }

    fun observeFavoriteIds(): Flow<Set<Int>> = favoriteDao.observeIds().map { it.toSet() }

    /** Returns the new state: true if the movie is now a favorite. */
    suspend fun toggleFavorite(movie: Movie): Boolean =
        if (favoriteDao.isFavorite(movie.id)) {
            favoriteDao.remove(movie.id)
            false
        } else {
            favoriteDao.add(movie.toFavoriteEntity(now()))
            true
        }

    private companion object {
        /** TMDB list endpoints return 20 items per page. */
        const val PAGE_SIZE = 20
    }
}
