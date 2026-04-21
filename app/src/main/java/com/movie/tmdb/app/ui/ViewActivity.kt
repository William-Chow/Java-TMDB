package com.movie.tmdb.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.movie.tmdb.app.R
import com.movie.tmdb.app.model.Movie
import com.movie.tmdb.app.network.APIClient
import com.movie.tmdb.app.network.APIClient.API_KEY
import com.movie.tmdb.app.network.APIInterface
import com.movie.tmdb.app.util.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ViewActivity : ComponentActivity() {
    private val apiInterface: APIInterface by lazy { APIClient.getClient().create(APIInterface::class.java) }
    private var movie by mutableStateOf<Movie?>(null)
    private var errorMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)

        val movieId = intent?.extras?.getInt("movie_id", 0) ?: 0
        if (movieId != 0) {
            getMovie(movieId)
        } else {
            errorMessage = "Something not right. Please Try Again later."
        }

        setContent {
            MovieDetailScreen(movie = movie, errorMessage = errorMessage)
        }
    }

    private fun getMovie(movieId: Int) {
        if (!Utils.getConnectionType(this)) {
            errorMessage = "No Internet Connection. Please Try Again"
            return
        }

        apiInterface.getMovie(movieId, API_KEY).enqueue(object : Callback<Movie> {
            override fun onResponse(call: Call<Movie>, response: Response<Movie>) {
                movie = response.body()
            }

            override fun onFailure(call: Call<Movie>, t: Throwable) {
                call.cancel()
                errorMessage = "Error. Please Try Again later."
            }
        })
    }
}

@Composable
private fun MovieDetailScreen(movie: Movie?, errorMessage: String?) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(errorMessage)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = { DetailAdMobBanner() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = movie?.title ?: "Title",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = movie?.overview ?: "Overview",
                style = MaterialTheme.typography.bodyMedium
            )
            AsyncImage(
                model = "${Utils.image_url}${movie?.poster_path ?: ""}",
                contentDescription = movie?.title ?: "Movie image",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DetailAdMobBanner() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val adUnitId = stringResource(R.string.admob_banner_ad_unit_id)
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = {
            AdView(context).apply {
                this.adUnitId = adUnitId
                setAdSize(AdSize.BANNER)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
