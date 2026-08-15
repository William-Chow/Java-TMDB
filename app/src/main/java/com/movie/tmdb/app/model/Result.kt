package com.movie.tmdb.app.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.google.gson.annotations.SerializedName

@JsonIgnoreProperties(ignoreUnknown = true)
data class Result(
    @SerializedName("results")
    val results: List<Movie>? = null
)
