package com.movie.tmdb.app.network

import com.movie.tmdb.app.model.Movie
import com.movie.tmdb.app.model.Result
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface APIInterface {

    /** [list] is a [com.movie.tmdb.app.model.MovieCategory] path: popular, top_rated, ... */
    @GET("movie/{list}?language=en-US")
    suspend fun getMovieList(
        @Path("list") list: String,
        @Query("api_key") apiKey: String,
        @Query("page") page: Int
    ): Result

    // Cast, trailers and recommendations ride along on the one detail request.
    @GET("movie/{movie_id}?language=en-US&append_to_response=credits,videos,similar")
    suspend fun getMovie(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): Movie

    @GET("search/movie?language=en-US")
    suspend fun getSearch(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int
    ): Result
}
