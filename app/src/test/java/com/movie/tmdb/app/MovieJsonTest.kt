package com.movie.tmdb.app

import com.google.gson.Gson
import com.movie.tmdb.app.model.Movie
import com.movie.tmdb.app.model.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the wire format: property names are camelCase, TMDB's are not. */
class MovieJsonTest {

    private val gson = Gson()

    @Test
    fun `list response maps snake_case fields and paging`() {
        val json = """
            {
              "page": 2,
              "total_pages": 42,
              "results": [
                {"id": 5, "title": "Dune", "original_title": "Dune",
                 "poster_path": "/dune.jpg", "vote_average": 8.4, "vote_count": 120,
                 "release_date": "2021-10-22"}
              ]
            }
        """.trimIndent()

        val result = gson.fromJson(json, Result::class.java)

        assertEquals(2, result.page)
        assertEquals(42, result.totalPages)
        val movie = result.results!!.single()
        assertEquals("Dune", movie.title)
        assertEquals("/dune.jpg", movie.posterPath)
        assertEquals("2021-10-22", movie.releaseDate)
        assertEquals(8.4, movie.voteAverage!!, 0.001)
        assertEquals(120, movie.voteCount)
    }

    @Test
    fun `detail response parses the append_to_response payload`() {
        val movie = gson.fromJson(detailJson, Movie::class.java)

        assertEquals(listOf("Science Fiction", "Drama"), movie.genres!!.map { it.name })
        assertEquals(155, movie.runtime)
        assertEquals("Rebecca Ferguson", movie.credits!!.cast!![1].name)
        assertEquals("Lady Jessica", movie.credits!!.cast!![1].character)
        assertEquals(listOf(11), movie.similar!!.results!!.map { it.id })
    }

    @Test
    fun `trailer url prefers the official youtube trailer`() {
        val movie = gson.fromJson(detailJson, Movie::class.java)

        assertEquals("https://www.youtube.com/watch?v=official1", movie.trailerUrl)
    }

    @Test
    fun `no trailer url when nothing is a youtube trailer`() {
        val json = """
            {"id": 1, "videos": {"results": [
              {"key": "abc", "site": "Vimeo", "type": "Trailer", "official": true},
              {"key": "def", "site": "YouTube", "type": "Featurette", "official": true}
            ]}}
        """.trimIndent()

        assertNull(gson.fromJson(json, Movie::class.java).trailerUrl)
    }

    @Test
    fun `unknown fields are ignored`() {
        val movie = gson.fromJson("""{"id": 3, "belongs_to_collection": {"x": 1}}""", Movie::class.java)

        assertEquals(3, movie.id)
        assertTrue(movie.title == null)
    }

    private val detailJson = """
        {
          "id": 438631,
          "title": "Dune",
          "runtime": 155,
          "genres": [{"id": 878, "name": "Science Fiction"}, {"id": 18, "name": "Drama"}],
          "credits": {"cast": [
            {"id": 1, "name": "Timothee Chalamet", "character": "Paul", "profile_path": "/p.jpg"},
            {"id": 2, "name": "Rebecca Ferguson", "character": "Lady Jessica"}
          ]},
          "videos": {"results": [
            {"key": "fan1", "site": "YouTube", "type": "Trailer", "official": false},
            {"key": "official1", "site": "YouTube", "type": "Trailer", "official": true},
            {"key": "clip1", "site": "YouTube", "type": "Clip", "official": true}
          ]},
          "similar": {"page": 1, "total_pages": 1, "results": [{"id": 11, "title": "Arrival"}]}
        }
    """.trimIndent()
}
