package com.movie.tmdb.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.google.android.gms.ads.MobileAds
import com.movie.tmdb.app.R
import com.movie.tmdb.app.model.CastMember
import com.movie.tmdb.app.model.Movie
import com.movie.tmdb.app.ui.theme.JavaTmdbTheme

class ViewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)

        val movieId = intent?.extras?.getInt(EXTRA_MOVIE_ID, 0) ?: 0

        setContent {
            JavaTmdbTheme {
                MovieDetailScreen(
                    movieId = movieId,
                    onBack = { finish() },
                    onMovieClick = { movie ->
                        startActivity(
                            Intent(this, ViewActivity::class.java)
                                .putExtra(EXTRA_MOVIE_ID, movie.id)
                        )
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_MOVIE_ID = "movie_id"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovieDetailScreen(
    movieId: Int,
    onBack: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    viewModel: MovieDetailViewModel = viewModel(factory = MovieDetailViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(movieId) { viewModel.load(movieId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.movie?.title ?: stringResource(R.string.movie_details),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (state.movie != null) {
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                painter = painterResource(
                                    if (state.isFavorite) R.drawable.ic_favorite
                                    else R.drawable.ic_favorite_border
                                ),
                                contentDescription = stringResource(
                                    if (state.isFavorite) R.string.remove_from_favorites
                                    else R.string.add_to_favorites
                                )
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = { AdMobBanner() }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val movie = state.movie
            val errorMessage = state.errorMessage
            when {
                state.isLoading && movie == null ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                movie == null ->
                    ErrorState(
                        message = stringResource(errorMessage ?: R.string.error_generic),
                        onRetry = viewModel::retry,
                        modifier = Modifier.align(Alignment.Center)
                    )

                else -> MovieDetailContent(
                    movie = movie,
                    onPlayTrailer = { url ->
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.no_trailer_app)
                                )
                            }
                        }
                    },
                    onMovieClick = onMovieClick
                )
            }
        }
    }
}

@Composable
private fun MovieDetailContent(
    movie: Movie,
    onPlayTrailer: (String) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MoviePoster(
            posterPath = movie.posterPath,
            title = movie.title,
            modifier = Modifier.fillMaxWidth(0.6f)
        )

        Text(
            text = movie.title ?: movie.originalTitle.orEmpty(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        movie.tagline?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, style = MaterialTheme.typography.titleSmall, fontStyle = FontStyle.Italic)
        }

        MetadataRow(movie)

        movie.genres?.takeIf { it.isNotEmpty() }?.let { genres ->
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(genres, key = { it.id }) { genre ->
                    AssistChip(onClick = {}, label = { Text(genre.name.orEmpty()) })
                }
            }
        }

        movie.trailerUrl?.let { url ->
            Button(onClick = { onPlayTrailer(url) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_play),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.watch_trailer),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Text(text = movie.overview.orEmpty(), style = MaterialTheme.typography.bodyMedium)

        movie.credits?.cast?.takeIf { it.isNotEmpty() }?.let { cast ->
            SectionHeader(stringResource(R.string.section_cast))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(cast.take(MAX_CAST), key = { it.id }) { CastCard(it) }
            }
        }

        movie.similar?.results?.takeIf { it.isNotEmpty() }?.let { similar ->
            SectionHeader(stringResource(R.string.section_similar))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(similar.take(MAX_SIMILAR), key = { it.id }) { related ->
                    Column(
                        modifier = Modifier
                            .width(110.dp)
                            .clickable { onMovieClick(related) }
                    ) {
                        MoviePoster(
                            posterPath = related.posterPath,
                            title = related.title,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = related.title ?: related.originalTitle.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(movie: Movie) {
    val parts = buildList {
        movie.releaseDate?.takeIf { it.isNotBlank() }?.let { add(stringResource(R.string.released_on, it)) }
        movie.runtime?.takeIf { it > 0 }?.let { add(stringResource(R.string.runtime_minutes, it)) }
        movie.voteAverage?.takeIf { it > 0 }?.let {
            add(stringResource(R.string.rating_format, it, movie.voteCount))
        }
    }
    if (parts.isEmpty()) return

    Text(
        text = parts.joinToString("  ·  "),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun CastCard(member: CastMember) {
    Column(modifier = Modifier.width(96.dp)) {
        MoviePoster(
            posterPath = member.profilePath,
            title = member.name,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = member.name.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
        member.character?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private const val MAX_CAST = 15
private const val MAX_SIMILAR = 15
