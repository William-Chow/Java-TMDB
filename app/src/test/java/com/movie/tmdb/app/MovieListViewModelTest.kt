package com.movie.tmdb.app

import com.movie.tmdb.app.data.MovieRepository
import com.movie.tmdb.app.data.ConnectivityChecker
import com.movie.tmdb.app.model.MovieCategory
import com.movie.tmdb.app.ui.ListTab
import com.movie.tmdb.app.ui.MovieListViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class MovieListViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val api = FakeApi()
    private val movieDao = FakeMovieDao()
    private val favoriteDao = FakeFavoriteDao()
    private var online = true

    private fun viewModel(): MovieListViewModel {
        val repository = MovieRepository(api, movieDao, favoriteDao, apiKey = "test-key", now = { 1L })
        return MovieListViewModel(repository, ConnectivityChecker { online })
    }

    private val popular = (1..20).map(::movie)

    @Test
    fun `popular list loads on start and is cached`() = runTest(dispatcherRule.dispatcher.scheduler) {
        api.listResponse = { _, p -> page(popular, p, totalPages = 3) }

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(popular.map { it.id }, vm.uiState.value.movies.map { it.id })
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.fatalError)
        assertTrue(vm.uiState.value.canLoadMore)
        assertEquals(20, movieDao.countFor(MovieCategory.POPULAR.path))
    }

    @Test
    fun `rapid typing collapses into a single search`() = runTest(dispatcherRule.dispatcher.scheduler) {
        api.listResponse = { _, p -> page(popular, p) }
        api.searchResponse = { q, p -> page(listOf(movie(99, q)), p) }

        val vm = viewModel()
        advanceUntilIdle()

        vm.onQueryChange("ba")
        advanceTimeBy(100)
        vm.onQueryChange("bat")
        advanceTimeBy(100)
        vm.onQueryChange("batman")
        advanceUntilIdle()

        assertEquals(listOf("batman" to 1), api.searchCalls)
    }

    /** The regression this whole rework was about: a slow earlier query must not win. */
    @Test
    fun `slow response for an earlier query cannot overwrite a newer one`() =
        runTest(dispatcherRule.dispatcher.scheduler) {
            api.listResponse = { _, p -> page(popular, p) }
            api.searchResponse = { query, p ->
                if (query == "ab") {
                    delay(500)
                    page(listOf(movie(999, "stale")), p)
                } else {
                    page(listOf(movie(111, "fresh")), p)
                }
            }

            val vm = viewModel()
            advanceUntilIdle()

            vm.onQueryChange("ab")
            advanceTimeBy(301) // debounce elapses, the slow "ab" request is now in flight
            vm.onQueryChange("abc")
            advanceUntilIdle()

            assertEquals(listOf("ab" to 1, "abc" to 1), api.searchCalls)
            assertEquals(listOf(111), vm.uiState.value.movies.map { it.id })
        }

    @Test
    fun `queries shorter than the minimum fall back to the browsing list`() =
        runTest(dispatcherRule.dispatcher.scheduler) {
            api.listResponse = { _, p -> page(popular, p) }

            val vm = viewModel()
            advanceUntilIdle()

            vm.onQueryChange("b")
            advanceUntilIdle()

            assertTrue(api.searchCalls.isEmpty())
            assertEquals(popular.map { it.id }, vm.uiState.value.movies.map { it.id })
        }

    @Test
    fun `next page appends and stops at the last page`() = runTest(dispatcherRule.dispatcher.scheduler) {
        val second = (21..40).map(::movie)
        api.listResponse = { _, p -> page(if (p == 1) popular else second, p, totalPages = 2) }

        val vm = viewModel()
        advanceUntilIdle()

        vm.loadNextPage()
        advanceUntilIdle()

        assertEquals((1..40).toList(), vm.uiState.value.movies.map { it.id })
        assertFalse(vm.uiState.value.canLoadMore)

        vm.loadNextPage()
        advanceUntilIdle()
        assertEquals(listOf("popular" to 1, "popular" to 2), api.listCalls)
    }

    @Test
    fun `offline with nothing cached takes over the screen`() = runTest(dispatcherRule.dispatcher.scheduler) {
        online = false

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(R.string.error_no_internet, vm.uiState.value.fatalError)
        assertTrue(vm.uiState.value.movies.isEmpty())
    }

    @Test
    fun `offline with a cache shows the cache and only warns`() = runTest(dispatcherRule.dispatcher.scheduler) {
        api.listResponse = { _, p -> page(popular, p) }

        // Populate the cache while online, then drop the connection and reload.
        val warm = viewModel()
        advanceUntilIdle()
        assertEquals(20, warm.uiState.value.movies.size)

        online = false
        val messages = mutableListOf<Int>()
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.messages.collect { messages += it }
            }
        advanceUntilIdle()

        assertNull(vm.uiState.value.fatalError)
        assertEquals(popular.map { it.id }, vm.uiState.value.movies.map { it.id })
        assertEquals(listOf(R.string.error_no_internet), messages)
    }

    @Test
    fun `a failed next page keeps the results and reports a message`() =
        runTest(dispatcherRule.dispatcher.scheduler) {
            api.listResponse = { _, p ->
                if (p == 1) page(popular, p, totalPages = 3) else throw IOException("boom")
            }

            val vm = viewModel()
            advanceUntilIdle()
            val messages = mutableListOf<Int>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.messages.collect { messages += it }
            }

            vm.loadNextPage()
            advanceUntilIdle()

            assertEquals(20, vm.uiState.value.movies.size)
            assertFalse(vm.uiState.value.isLoadingMore)
            assertEquals(listOf(R.string.error_network), messages)
        }

    @Test
    fun `switching category fetches that endpoint`() = runTest(dispatcherRule.dispatcher.scheduler) {
        api.listResponse = { list, p -> page(listOf(movie(1, list)), p) }

        val vm = viewModel()
        advanceUntilIdle()

        vm.onTabSelected(ListTab.Category(MovieCategory.TOP_RATED))
        advanceUntilIdle()

        assertEquals(listOf("popular" to 1, "top_rated" to 1), api.listCalls)
        assertEquals("top_rated", vm.uiState.value.movies.single().title)
    }

    /** The state's tab drives the tab-row indicator; it used to never leave POPULAR. */
    @Test
    fun `selecting a tab is reflected in the state`() = runTest(dispatcherRule.dispatcher.scheduler) {
        api.listResponse = { _, p -> page(popular, p) }

        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(ListTab.Category(MovieCategory.POPULAR), vm.uiState.value.tab)

        vm.onTabSelected(ListTab.Category(MovieCategory.UPCOMING))
        advanceUntilIdle()
        assertEquals(ListTab.Category(MovieCategory.UPCOMING), vm.uiState.value.tab)

        vm.onTabSelected(ListTab.Favorites)
        advanceUntilIdle()
        assertEquals(ListTab.Favorites, vm.uiState.value.tab)
        assertTrue(vm.uiState.value.isFavoritesTab)
    }

    @Test
    fun `toggling a favorite adds then removes it`() = runTest(dispatcherRule.dispatcher.scheduler) {
        api.listResponse = { _, p -> page(popular, p) }

        val vm = viewModel()
        advanceUntilIdle()

        vm.toggleFavorite(popular.first())
        advanceUntilIdle()
        assertEquals(setOf(1), vm.uiState.value.favoriteIds)

        vm.toggleFavorite(popular.first())
        advanceUntilIdle()
        assertTrue(vm.uiState.value.favoriteIds.isEmpty())
    }

    @Test
    fun `favorites tab lists saved movies without hitting the network`() =
        runTest(dispatcherRule.dispatcher.scheduler) {
            api.listResponse = { _, p -> page(popular, p) }

            val vm = viewModel()
            advanceUntilIdle()
            vm.toggleFavorite(movie(7, "Saved"))
            advanceUntilIdle()

            val callsBefore = api.listCalls.size
            vm.onTabSelected(ListTab.Favorites)
            advanceUntilIdle()

            assertTrue(vm.uiState.value.isFavoritesTab)
            assertEquals(listOf(7), vm.uiState.value.movies.map { it.id })
            assertEquals(callsBefore, api.listCalls.size)
        }
}
