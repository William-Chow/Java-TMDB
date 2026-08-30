package com.movie.tmdb.app.model

import androidx.annotation.StringRes
import com.movie.tmdb.app.R

/** The TMDB list endpoints under `movie/`, plus the label shown on its tab. */
enum class MovieCategory(val path: String, @param:StringRes val labelRes: Int) {
    POPULAR("popular", R.string.category_popular),
    NOW_PLAYING("now_playing", R.string.category_now_playing),
    TOP_RATED("top_rated", R.string.category_top_rated),
    UPCOMING("upcoming", R.string.category_upcoming)
}
