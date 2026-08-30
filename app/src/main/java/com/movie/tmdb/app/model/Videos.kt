package com.movie.tmdb.app.model

import com.google.gson.annotations.SerializedName

data class Videos(
    @SerializedName("results")
    val results: List<Video>? = null
)

data class Video(
    @SerializedName("key")
    val key: String? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("site")
    val site: String? = null,

    @SerializedName("type")
    val type: String? = null,

    @SerializedName("official")
    val official: Boolean = false
) {
    val isYouTube: Boolean get() = site.equals("YouTube", ignoreCase = true)

    val watchUrl: String? get() = key?.takeIf { isYouTube }?.let { "https://www.youtube.com/watch?v=$it" }
}
