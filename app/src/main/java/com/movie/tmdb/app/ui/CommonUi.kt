package com.movie.tmdb.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.movie.tmdb.app.R
import com.movie.tmdb.app.util.Utils

/** TMDB posters are 2:3. */
private const val POSTER_ASPECT_RATIO = 2f / 3f

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

/** Pinning the poster ratio stops the grid from reflowing as images arrive. */
@Composable
fun MoviePoster(posterPath: String?, title: String?, modifier: Modifier = Modifier) {
    val fallback = painterResource(R.drawable.ic_no_exist)
    AsyncImage(
        model = posterPath?.let { "${Utils.IMAGE_URL}$it" },
        contentDescription = title?.let { stringResource(R.string.poster_of, it) },
        contentScale = ContentScale.Crop,
        placeholder = painterResource(R.drawable.ic_logo),
        error = fallback,
        fallback = fallback,
        modifier = modifier.aspectRatio(POSTER_ASPECT_RATIO)
    )
}
