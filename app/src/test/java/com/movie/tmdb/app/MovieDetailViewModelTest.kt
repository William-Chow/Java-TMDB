package com.movie.tmdb.app

import com.movie.tmdb.app.data.ConnectivityChecker
import com.movie.tmdb.app.data.MovieRepository
import com.movie.tmdb.app.ui.MovieDetailViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class MovieDetailViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val api = FakeApi()
    private val movieDao = FakeMovieDao()
    private val favoriteDao = FakeFavoriteDao()
    private var online = true

    private fun viewModel(): MovieDetailViewModel {
        val repository = MovieRepository(api, movieDao, favoriteDao, apiKey = "test-key", now = { 1L })
        return MovieDetailViewModel(repository, ConnectivityChecker { online })
    }

    @Test
    fun `loads the requested movie`() = runTest(dispatcherRule.dispatcher.scheduler) {
        api.detailResponse = { movie(42, "Alien") }

        val vm = viewModel()
        vm.load(42)
        advanceUntilIdle()

        assertEquals("Alien", vm.uiState.value.movie?.title)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `a missing movie id reports an error instead of calling the api`() =
        runTest(dispatcherRule.dispatcher.scheduler) {
            val vm = viewModel()
            vm.load(0)
            advanceUntilIdle()

            assertTrue(api.detailCalls.isEmpty())
            assertEquals(R.string.error_generic, vm.uiState.value.errorMessage)
        }

    @Test
    fun `offline reports no connection without calling the api`() =
        runTest(dispatcherRule.dispatcher.scheduler) {
            online = false

            val vm = viewModel()
            vm.load(42)
            advanceUntilIdle()

            assertTrue(api.detailCalls.isEmpty())
            assertEquals(R.string.error_no_internet, vm.uiState.value.errorMessage)
        }

    @Test
    fun `retry recovers after a network failure`() = runTest(dispatcherRule.dispatcher.scheduler) {
        var fail = true
        api.detailResponse = { if (fail) throw IOException("boom") else movie(42, "Alien") }

        val vm = viewModel()
        vm.load(42)
        advanceUntilIdle()
        assertEquals(R.string.error_network, vm.uiState.value.errorMessage)

        fail = false
        vm.retry()
        advanceUntilIdle()

        assertEquals("Alien", vm.uiState.value.movie?.title)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `favorite state reflects the store both ways`() = runTest(dispatcherRule.dispatcher.scheduler) {
        api.detailResponse = { movie(42, "Alien") }

        val vm = viewModel()
        vm.load(42)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isFavorite)

        vm.toggleFavorite()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isFavorite)

        vm.toggleFavorite()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isFavorite)
    }
}
