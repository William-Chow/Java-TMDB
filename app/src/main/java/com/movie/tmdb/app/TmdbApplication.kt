package com.movie.tmdb.app

import android.app.Application
import android.content.Context
import com.movie.tmdb.app.data.ConnectivityChecker
import com.movie.tmdb.app.data.MovieRepository
import com.movie.tmdb.app.data.TmdbDatabase
import com.movie.tmdb.app.network.APIClient
import com.movie.tmdb.app.util.Utils

/** Hand-rolled DI: small enough that a container beats pulling in a framework. */
class AppContainer(context: Context) {

    private val database = TmdbDatabase.build(context)

    val repository = MovieRepository(
        api = APIClient.api,
        movieDao = database.movieDao(),
        favoriteDao = database.favoriteDao()
    )

    val connectivity = ConnectivityChecker { Utils.getConnectionType(context) }
}

class TmdbApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}
