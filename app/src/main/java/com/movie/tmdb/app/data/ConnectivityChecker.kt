package com.movie.tmdb.app.data

/** Lets the ViewModels ask about connectivity without holding a Context. */
fun interface ConnectivityChecker {
    fun isOnline(): Boolean
}
