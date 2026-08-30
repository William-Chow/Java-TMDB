package com.movie.tmdb.app.model

import com.google.gson.annotations.SerializedName

data class Result(
    @SerializedName("page")
    val page: Int = 1,

    @SerializedName("total_pages")
    val totalPages: Int = 1,

    @SerializedName("results")
    val results: List<Movie>? = null
)
