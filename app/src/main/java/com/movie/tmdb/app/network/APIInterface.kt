package com.movie.tmdb.app.network

import com.movie.tmdb.app.model.Movie
import com.movie.tmdb.app.model.Result
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface APIInterface {

    @GET("movie/popular?")
    fun getPopularMovies(@Query("api_key") apiKey: String): Call<Result>

    @GET("movie/{movie_id}?language=en-US")
    fun getMovie(@Path("movie_id") movieId: Int, @Query("api_key") apiKey: String): Call<Movie>

    // search/movie?api_key={api_key}&language=en-US
    @GET("search/movie?language=en-US")
    fun getSearch(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int
    ): Call<Result>
}
