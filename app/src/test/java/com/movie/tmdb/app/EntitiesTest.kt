package com.movie.tmdb.app

import com.movie.tmdb.app.data.toCacheEntity
import com.movie.tmdb.app.data.toFavoriteEntity
import com.movie.tmdb.app.data.toMovie
import com.movie.tmdb.app.model.Movie
import org.junit.Assert.assertEquals
import org.junit.Test

class EntitiesTest {

    /** Every field the grid reads, populated, plus detail-only fields the tables don't keep. */
    private val movie = Movie(
        id = 42,
        title = "Title",
        originalTitle = "Original",
        posterPath = "/poster.jpg",
        overview = "Overview",
        releaseDate = "2026-01-02",
        voteAverage = 7.5,
        voteCount = 1234,
        tagline = "dropped",
        runtime = 120
    )

    private val listFields = movie.copy(tagline = null, runtime = null)

    @Test
    fun `cached rows round-trip the list fields`() {
        val entity = movie.toCacheEntity(category = "popular", position = 3, page = 1)

        assertEquals("popular", entity.category)
        assertEquals(42, entity.id)
        assertEquals(3, entity.position)
        assertEquals(1, entity.page)
        assertEquals(listFields, entity.toMovie())
    }

    @Test
    fun `favorites round-trip the list fields`() {
        val entity = movie.toFavoriteEntity(addedAt = 99L)

        assertEquals(42, entity.id)
        assertEquals(99L, entity.addedAt)
        assertEquals(listFields, entity.toMovie())
    }

    @Test
    fun `both tables store the same summary for a movie`() {
        assertEquals(
            movie.toCacheEntity("popular", 0, 1).summary,
            movie.toFavoriteEntity(0L).summary
        )
    }
}
