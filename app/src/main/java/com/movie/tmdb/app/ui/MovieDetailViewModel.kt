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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MovieDetailUiState(
    val movie: Movie? = null,
    val isLoading: Boolean = false,
    val isFavorite: Boolean = false,
    @param:StringRes val errorMessage: Int? = null
)

class MovieDetailViewModel(
    private val repository: MovieRepository,
    private val connectivity: ConnectivityChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var movieId = 0
    private var job: Job? = null
    private var favoritesJob: Job? = null

    /** Safe to call on every recomposition; only the first call for a given id fetches. */
    fun load(movieId: Int) {
        if (this.movieId == movieId && (job?.isActive == true || _uiState.value.movie != null)) return
        this.movieId = movieId

        favoritesJob?.cancel()
        favoritesJob = viewModelScope.launch {
            repository.observeFavoriteIds().collect { ids ->
                _uiState.update { it.copy(isFavorite = movieId in ids) }
            }
        }

        retry()
    }

    fun retry() {
        val id = movieId
        if (id == 0) {
            _uiState.update { it.copy(isLoading = false, errorMessage = R.string.error_generic) }
            return
        }

        job?.cancel()
        job = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            if (!connectivity.isOnline()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = R.string.error_no_internet) }
                return@launch
            }

            try {
                val movie = repository.movieDetail(id)
                _uiState.update { it.copy(movie = movie, isLoading = false, errorMessage = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message =
                    if (e is java.io.IOException) R.string.error_network else R.string.error_generic
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
            }
        }
    }

    fun toggleFavorite() {
        val movie = _uiState.value.movie ?: return
        viewModelScope.launch { repository.toggleFavorite(movie) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TmdbApplication
                MovieDetailViewModel(app.container.repository, app.container.connectivity)
            }
        }
    }
}
