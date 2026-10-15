package com.movie.tmdb.app.model

import com.google.gson.annotations.SerializedName

data class Credits(
    @SerializedName("cast")
    val cast: List<CastMember>? = null
)

data class CastMember(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("character")
    val character: String? = null,

    @SerializedName("profile_path")
    val profilePath: String? = null,

    // One per role: an actor playing several parts repeats [id], never this.
    @SerializedName("credit_id")
    val creditId: String? = null
)
