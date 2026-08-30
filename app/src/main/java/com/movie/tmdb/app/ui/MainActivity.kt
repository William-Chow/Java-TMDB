package com.movie.tmdb.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.ads.MobileAds
import com.movie.tmdb.app.R
import com.movie.tmdb.app.model.Movie
import com.movie.tmdb.app.model.MovieCategory
import com.movie.tmdb.app.ui.theme.JavaTmdbTheme

class MainActivity : ComponentActivity() {

    private var backPressedTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)

        setContent {
            JavaTmdbTheme {
                MovieListScreen(
                    onMovieClick = { movie ->
                        startActivity(
                            Intent(this, ViewActivity::class.java)
                                .putExtra(ViewActivity.EXTRA_MOVIE_ID, movie.id)
                        )
                    }
                )
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedTime + 3000 > System.currentTimeMillis()) {
                    finish()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        R.string.press_back_again,
                        Toast.LENGTH_LONG
                    ).show()
                }
                backPressedTime = System.currentTimeMillis()
            }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovieListScreen(
    onMovieClick: (Movie) -> Unit,
    viewModel: MovieListViewModel = viewModel(factory = MovieListViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val messages = viewModel.messages
    val context = LocalContext.current

    // A channel of events, so two identical failures in a row both surface.
    LaunchedEffect(messages, context) {
        messages.collect { snackbarHostState.showSnackbar(context.getString(it)) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = { AdMobBanner() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CategoryTabs(selected = state.tab, onSelected = viewModel::onTabSelected)

            if (!state.isFavoritesTab) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    label = { Text(stringResource(R.string.search_movies)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    singleLine = true
                )
            }

            val fatalError = state.fatalError
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.isLoading && state.movies.isEmpty() ->
                        Box(Modifier.fillMaxSize()) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }

                    fatalError != null ->
                        Box(Modifier.fillMaxSize()) {
                            ErrorState(
                                message = stringResource(fatalError),
                                onRetry = viewModel::retry,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                    state.movies.isEmpty() ->
                        Box(Modifier.fillMaxSize()) {
                            Text(
                                text = when {
                                    state.isFavoritesTab -> stringResource(R.string.no_favorites)
                                    state.isSearchResult ->
                                        stringResource(R.string.no_results_for, state.query.trim())

                                    else -> stringResource(R.string.no_movies)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp)
                            )
                        }

                    else -> MovieGrid(
                        movies = state.movies,
                        favoriteIds = state.favoriteIds,
                        isLoadingMore = state.isLoadingMore,
                        onLoadMore = viewModel::loadNextPage,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onMovieClick = onMovieClick
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTabs(selected: ListTab, onSelected: (ListTab) -> Unit) {
    val tabs = remember {
        MovieCategory.entries.map { ListTab.Category(it) } + ListTab.Favorites
    }
    val selectedIndex = tabs.indexOf(selected).coerceAtLeast(0)

    ScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = 8.dp) {
        tabs.forEach { tab ->
            val label = when (tab) {
                is ListTab.Category -> stringResource(tab.category.labelRes)
                ListTab.Favorites -> stringResource(R.string.tab_favorites)
            }
            Tab(
                selected = tab == selected,
                onClick = { onSelected(tab) },
                text = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
}

@Composable
private fun MovieGrid(
    movies: List<Movie>,
    favoriteIds: Set<Int>,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    onToggleFavorite: (Movie) -> Unit,
    onMovieClick: (Movie) -> Unit
) {
    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember(movies.size) {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= movies.size - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(movies, key = { it.id }) { movie ->
            MovieCard(
                movie = movie,
                isFavorite = movie.id in favoriteIds,
                onToggleFavorite = { onToggleFavorite(movie) },
                onClick = { onMovieClick(movie) }
            )
        }
        if (isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun MovieCard(
    movie: Movie,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Box {
            MoviePoster(
                posterPath = movie.posterPath,
                title = movie.title ?: movie.originalTitle,
                modifier = Modifier.fillMaxWidth()
            )
            FavoriteButton(
                isFavorite = isFavorite,
                onToggle = onToggleFavorite,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
        Text(
            text = movie.title ?: movie.originalTitle ?: "",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}

@Composable
private fun FavoriteButton(isFavorite: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            painter = painterResource(
                if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            ),
            contentDescription = stringResource(
                if (isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites
            ),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

/** Start fetching the next page while this many items are still below the fold. */
private const val LOAD_MORE_THRESHOLD = 4
