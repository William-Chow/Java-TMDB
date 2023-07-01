package com.movie.app.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface APIInterface {

    @GET("movie/popular?")
    Call<Result> getPopularMovies(@Query("api_key") String apiKey);

    @GET("movie/{movie_id}?language=en-US")
    Call<Movie> getMovie(@Path("movie_id") int movie_id, @Query("api_key") String apiKey);

    // search/movie?api_key={api_key}&language=en-US
    @GET("search/movie?language=en-US")
    Call<Result> getSearch(@Query("api_key") String apiKey, @Query("query") String query, @Query("page") int page);
}
