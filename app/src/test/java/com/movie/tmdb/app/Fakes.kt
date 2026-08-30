package com.movie.tmdb.app

import com.movie.tmdb.app.data.CachedMovieEntity
import com.movie.tmdb.app.data.FavoriteDao
import com.movie.tmdb.app.data.FavoriteEntity
import com.movie.tmdb.app.data.MovieDao
import com.movie.tmdb.app.model.Movie
import com.movie.tmdb.app.model.Result
import com.movie.tmdb.app.network.APIInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

fun movie(id: Int, title: String = "Movie $id") = Movie(id = id, title = title)

fun page(movies: List<Movie>, page: Int = 1, totalPages: Int = 1) =
    Result(page = page, totalPages = totalPages, results = movies)

class FakeApi : APIInterface {

    val listCalls = mutableListOf<Pair<String, Int>>()
    val searchCalls = mutableListOf<Pair<String, Int>>()
    val detailCalls = mutableListOf<Int>()

    var listResponse: suspend (String, Int) -> Result = { _, p -> page(emptyList(), p) }
    var searchResponse: suspend (String, Int) -> Result = { _, p -> page(emptyList(), p) }
    var detailResponse: suspend (Int) -> Movie = { movie(it) }

    override suspend fun getMovieList(list: String, apiKey: String, page: Int): Result {
        listCalls += list to page
        return listResponse(list, page)
    }

    override suspend fun getSearch(apiKey: String, query: String, page: Int): Result {
        searchCalls += query to page
        return searchResponse(query, page)
    }

    override suspend fun getMovie(movieId: Int, apiKey: String): Movie {
        detailCalls += movieId
        return detailResponse(movieId)
    }
}

class FakeMovieDao : MovieDao {

    private val rows = MutableStateFlow<List<CachedMovieEntity>>(emptyList())

    override fun observeCategory(category: String): Flow<List<CachedMovieEntity>> =
        rows.map { all -> all.filter { it.category == category }.sortedBy { it.position } }

    override suspend fun countFor(category: String): Int = rows.value.count { it.category == category }

    override suspend fun insertAll(movies: List<CachedMovieEntity>) {
        rows.update { current ->
            val byKey = current.associateBy { it.category to it.id }.toMutableMap()
            movies.forEach { byKey[it.category to it.id] = it }
            byKey.values.toList()
        }
    }

    override suspend fun clearCategory(category: String) {
        rows.update { current -> current.filterNot { it.category == category } }
    }
}

class FakeFavoriteDao : FavoriteDao {

    private val rows = MutableStateFlow<List<FavoriteEntity>>(emptyList())

    override fun observeAll(): Flow<List<FavoriteEntity>> =
        rows.map { all -> all.sortedByDescending { it.addedAt } }

    override fun observeIds(): Flow<List<Int>> = rows.map { all -> all.map { it.id } }

    override suspend fun add(favorite: FavoriteEntity) {
        rows.update { current -> current.filterNot { it.id == favorite.id } + favorite }
    }

    override suspend fun remove(id: Int) {
        rows.update { current -> current.filterNot { it.id == id } }
    }

    override suspend fun isFavorite(id: Int): Boolean = rows.value.any { it.id == id }
}
