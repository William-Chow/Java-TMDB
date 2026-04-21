package com.movie.tmdb.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.movie.tmdb.app.R
import com.movie.tmdb.app.model.Movie
import com.movie.tmdb.app.model.Result
import com.movie.tmdb.app.network.APIClient
import com.movie.tmdb.app.network.APIClient.API_KEY
import com.movie.tmdb.app.network.APIInterface
import com.movie.tmdb.app.util.Utils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : ComponentActivity() {

    private var backPressedTime = 0L
    private val apiInterface: APIInterface by lazy { APIClient.getClient().create(APIInterface::class.java) }
    private val popularList = mutableStateListOf<Movie>()
    private val originalList = mutableStateListOf<Movie>()
    private val searchList = mutableStateListOf<Movie>()
    private val visibleList = mutableStateListOf<Movie>()
    private var errorMessage by mutableStateOf<String?>(null)
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        if (Utils.getConnectionType(this)) {
            getPopularMovieList()
        } else {
            errorMessage = "No Internet Connection. Please Try Again"
        }

        setContent {
            MovieListScreen(
                movies = visibleList,
                errorMessage = errorMessage,
                onSearch = ::onSearchQuery,
                onMovieClick = { movie ->
                    startActivity(Intent(this, ViewActivity::class.java).putExtra("movie_id", movie.id))
                }
            )
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedTime + 3000 > System.currentTimeMillis()) {
                    finish()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Press back again to leave the app.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                backPressedTime = System.currentTimeMillis()
            }
        })
    }

    private fun getPopularMovieList() {
        apiInterface.getPopularMovies(API_KEY).enqueue(object : Callback<Result> {
            override fun onResponse(call: Call<Result>, response: Response<Result>) {
                val results = response.body()?.results.orEmpty()
                popularList.clear()
                originalList.clear()
                visibleList.clear()
                popularList.addAll(results)
                originalList.addAll(results)
                visibleList.addAll(results)
                if (results.isEmpty()) {
                    errorMessage = "No result found. Please Try Again later."
                }
            }

            override fun onFailure(call: Call<Result>, t: Throwable) {
                call.cancel()
                errorMessage = "Error. Please Try Again later."
            }
        })
    }

    private fun onSearchQuery(query: String) {
        searchJob?.cancel()
        if (query.length < 2) {
            visibleList.clear()
            visibleList.addAll(originalList)
            return
        }

        searchJob = lifecycleScope.launch {
            delay(300)
            getSearchResult(query)
        }
    }

    private fun getSearchResult(query: String) {
        apiInterface.getSearch(API_KEY, query, 1).enqueue(object : Callback<Result> {
            override fun onResponse(call: Call<Result>, response: Response<Result>) {
                searchList.clear()
                searchList.addAll(response.body()?.results.orEmpty())
                visibleList.clear()
                visibleList.addAll(searchList)
            }

            override fun onFailure(call: Call<Result>, t: Throwable) {
                call.cancel()
                errorMessage = "Error. Please Try Again later."
            }
        })
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovieListScreen(
    movies: List<Movie>,
    errorMessage: String?,
    onSearch: (String) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            scope.launch { snackbarHostState.showSnackbar(errorMessage) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JavaTMDB") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            AdMobBanner()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AdMobBanner()
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it)
                },
                label = { Text("Search movies") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                singleLine = true
            )
            MovieGrid(movies = movies, onMovieClick = onMovieClick)
        }
    }
}

@Composable
private fun MovieGrid(
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit
) {
    if (movies.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "No data available", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(movies, key = { it.id }) { movie ->
            MovieCard(movie = movie, onClick = { onMovieClick(movie) })
        }
    }
}

@Composable
private fun MovieCard(movie: Movie, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = "${Utils.image_url}${movie.poster_path}",
                contentDescription = movie.title ?: movie.original_title,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = movie.original_title ?: movie.title ?: "",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun AdMobBanner() {
    val context = LocalContext.current
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
