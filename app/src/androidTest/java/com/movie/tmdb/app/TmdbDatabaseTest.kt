package com.movie.tmdb.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.movie.tmdb.app.data.CachedMovieEntity
import com.movie.tmdb.app.data.FavoriteEntity
import com.movie.tmdb.app.data.TmdbDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TmdbDatabaseTest {

    private lateinit var database: TmdbDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, TmdbDatabase::class.java).build()
    }

    @After
    fun tearDown() = database.close()

    private fun cached(id: Int, position: Int, category: String = "popular") = CachedMovieEntity(
        category = category,
        id = id,
        position = position,
        page = 1,
        title = "Movie $id",
        originalTitle = null,
        posterPath = null,
        overview = null,
        releaseDate = null,
        voteAverage = null,
        voteCount = 0
    )

    @Test
    fun cachedMoviesComeBackInServerOrder() = runBlocking {
        val dao = database.movieDao()
        dao.insertAll(listOf(cached(id = 7, position = 1), cached(id = 3, position = 0)))

        assertEquals(listOf(3, 7), dao.observeCategory("popular").first().map { it.id })
    }

    @Test
    fun replacingAPageDropsRowsTheServerNoLongerReturns() = runBlocking {
        val dao = database.movieDao()
        dao.insertAll(listOf(cached(id = 1, position = 0), cached(id = 2, position = 1)))

        dao.replaceCategory("popular", listOf(cached(id = 2, position = 0)))

        assertEquals(listOf(2), dao.observeCategory("popular").first().map { it.id })
        assertEquals(1, dao.countFor("popular"))
    }

    @Test
    fun categoriesAreIsolatedFromEachOther() = runBlocking {
        val dao = database.movieDao()
        dao.insertAll(listOf(cached(id = 1, position = 0), cached(id = 1, position = 0, category = "top_rated")))

        dao.clearCategory("popular")

        assertEquals(0, dao.countFor("popular"))
        assertEquals(1, dao.countFor("top_rated"))
    }

    @Test
    fun favoritesRoundTripNewestFirst() = runBlocking {
        val dao = database.favoriteDao()
        dao.add(favorite(id = 1, addedAt = 100))
        dao.add(favorite(id = 2, addedAt = 200))

        assertEquals(listOf(2, 1), dao.observeAll().first().map { it.id })
        assertTrue(dao.isFavorite(1))

        dao.remove(1)
        assertFalse(dao.isFavorite(1))
        assertEquals(listOf(2), dao.observeIds().first())
    }

    private fun favorite(id: Int, addedAt: Long) = FavoriteEntity(
        id = id,
        title = "Movie $id",
        originalTitle = null,
        posterPath = null,
        overview = null,
        releaseDate = null,
        voteAverage = null,
        voteCount = 0,
        addedAt = addedAt
    )
}
