package com.movie.tmdb.app.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.movie.tmdb.app.R
import com.movie.tmdb.app.TmdbApplication
import com.movie.tmdb.app.data.ConnectivityChecker
import com.movie.tmdb.app.data.MovieRepository
import com.movie.tmdb.app.model.Movie
import com.movie.tmdb.app.model.MovieCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ListTab {
    data class Category(val category: MovieCategory) : ListTab
    data object Favorites : ListTab
}

data class MovieListUiState(
    val tab: ListTab = ListTab.Category(MovieCategory.POPULAR),
    val query: String = "",
    val movies: List<Movie> = emptyList(),
    val favoriteIds: Set<Int> = emptySet(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** Set only when the list is empty and reloading is the way out. */
    @param:StringRes val fatalError: Int? = null,
    val canLoadMore: Boolean = false,
    val isSearchResult: Boolean = false,
    val isFavoritesTab: Boolean = false
)

/** What the list is currently showing; derived from the selected tab and the search box. */
private sealed interface Mode {
    data class Category(val category: MovieCategory) : Mode
    data class Search(val query: String) : Mode
    data object Favorites : Mode
}

@OptIn(FlowPreview::class)
class MovieListViewModel(
    private val repository: MovieRepository,
    private val connectivity: ConnectivityChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieListUiState())
    val uiState = _uiState.asStateFlow()

    /** Failures that happen while results are already on screen; surfaced as a snackbar. */
    private val _messages = Channel<Int>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private val tabs = MutableStateFlow<ListTab>(ListTab.Category(MovieCategory.POPULAR))
    private val queries = MutableStateFlow("")

    private var mode: Mode = Mode.Category(MovieCategory.POPULAR)
    private var page = 1
    private var totalPages = 1
    private var observeJob: Job? = null
    private var firstPageJob: Job? = null
    private var loadMoreJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeFavoriteIds().collect { ids ->
                _uiState.update { it.copy(favoriteIds = ids) }
            }
        }

        viewModelScope.launch {
            val searchTerms = queries
                .map { it.trim() }
                .map { if (it.length < MIN_QUERY_LENGTH) "" else it }
                // Falling back to the browsing list is instant; only real searches wait.
                .debounce { if (it.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }
                .distinctUntilChanged()

            combine(tabs, searchTerms) { tab, term ->
                when {
                    tab is ListTab.Favorites -> Mode.Favorites
                    term.isEmpty() -> Mode.Category((tab as ListTab.Category).category)
                    else -> Mode.Search(term)
                }
            }
                .distinctUntilChanged()
                .collect(::switchTo)
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        queries.value = query
    }

    fun onTabSelected(tab: ListTab) {
        _uiState.update { it.copy(tab = tab) }
        tabs.value = tab
    }

    fun retry() = switchTo(mode)

    fun refresh() {
        when (val current = mode) {
            is Mode.Favorites -> Unit // Room-backed; nothing to pull.
            is Mode.Category -> startFirstPage(current, refreshing = true)
            is Mode.Search -> startFirstPage(current, refreshing = true)
        }
    }

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch { repository.toggleFavorite(movie) }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore) return
        if (loadMoreJob?.isActive == true) return

        val current = mode
        val next = page + 1
        loadMoreJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val more = when (current) {
                    is Mode.Category -> {
                        // Written through to Room; the cache Flow delivers the new rows.
                        totalPages = repository.loadCategoryPage(current.category, next)
                        emptyList()
                    }

                    is Mode.Search -> {
                        val result = repository.search(current.query, next)
                        totalPages = result.totalPages
                        result.results.orEmpty()
                    }

                    Mode.Favorites -> emptyList()
                }
                if (current != mode) return@launch
                page = next
                _uiState.update {
                    it.copy(
                        movies = if (more.isEmpty()) it.movies else (it.movies + more).distinctBy(Movie::id),
                        isLoadingMore = false,
                        canLoadMore = next < totalPages
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (current != mode) return@launch
                _uiState.update { it.copy(isLoadingMore = false) }
                _messages.trySend(e.toMessageRes())
            }
        }
    }

    private fun switchTo(next: Mode) {
        mode = next
        observeJob?.cancel()
        firstPageJob?.cancel()
        loadMoreJob?.cancel()
        page = 1
        totalPages = 1

        _uiState.update {
            it.copy(
                fatalError = null,
                isLoadingMore = false,
                canLoadMore = false,
                isSearchResult = next is Mode.Search,
                isFavoritesTab = next is Mode.Favorites
            )
        }

        when (next) {
            Mode.Favorites -> {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                observeJob = viewModelScope.launch {
                    repository.observeFavorites().collect { favorites ->
                        _uiState.update { it.copy(movies = favorites) }
                    }
                }
            }

            is Mode.Category -> {
                // Room is the source of truth here, so cached rows paint before the network answers.
                observeJob = viewModelScope.launch {
                    repository.observeCategory(next.category).collect { cached ->
                        _uiState.update { it.copy(movies = cached) }
                    }
                }
                startFirstPage(next, refreshing = false)
            }

            is Mode.Search -> {
                _uiState.update { it.copy(movies = emptyList()) }
                startFirstPage(next, refreshing = false)
            }
        }
    }

    private fun startFirstPage(target: Mode, refreshing: Boolean) {
        firstPageJob?.cancel()
        loadMoreJob?.cancel()
        firstPageJob = viewModelScope.launch { loadFirstPage(target, refreshing) }
    }

    /**
     * Cancelling [firstPageJob] cancels the in-flight HTTP call, so a slow response for
     * "ab" can no longer overwrite the results already rendered for "abc".
     */
    private suspend fun loadFirstPage(target: Mode, refreshing: Boolean) {
        val hasContentAlready = when (target) {
            is Mode.Category -> repository.cachedCount(target.category) > 0
            else -> false
        }
        _uiState.update {
            it.copy(
                isLoading = !refreshing && !hasContentAlready,
                isRefreshing = refreshing,
                fatalError = null
            )
        }

        if (!connectivity.isOnline()) {
            finishFirstPage(target, hasContentAlready, R.string.error_no_internet)
            return
        }

        try {
            when (target) {
                is Mode.Category -> {
                    totalPages = repository.loadCategoryPage(target.category, 1)
                    if (target != mode) return
                    _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false, fatalError = null, canLoadMore = 1 < totalPages)
                    }
                }

                is Mode.Search -> {
                    val result = repository.search(target.query, 1)
                    if (target != mode) return
                    totalPages = result.totalPages
                    _uiState.update {
                        it.copy(
                            movies = result.results.orEmpty().distinctBy(Movie::id),
                            isLoading = false,
                            isRefreshing = false,
                            fatalError = null,
                            canLoadMore = 1 < totalPages
                        )
                    }
                }

                Mode.Favorites -> Unit
            }
            page = 1
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (target != mode) return
            finishFirstPage(target, hasContentAlready, e.toMessageRes())
        }
    }

    /** With cached rows on screen a failure is a snackbar; with nothing to show it takes over the screen. */
    private fun finishFirstPage(target: Mode, hasContentAlready: Boolean, @StringRes message: Int) {
        if (target != mode) return
        if (hasContentAlready) {
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            _messages.trySend(message)
        } else {
            _uiState.update {
                it.copy(
                    movies = if (target is Mode.Search) emptyList() else it.movies,
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    fatalError = message,
                    canLoadMore = false
                )
            }
        }
    }

    @StringRes
    private fun Throwable.toMessageRes(): Int =
        if (this is java.io.IOException) R.string.error_network else R.string.error_generic

    companion object {
        const val MIN_QUERY_LENGTH = 2
        const val SEARCH_DEBOUNCE_MS = 300L

        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TmdbApplication
                MovieListViewModel(app.container.repository, app.container.connectivity)
            }
        }
    }
}
