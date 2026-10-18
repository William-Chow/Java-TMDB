package com.movie.tmdb.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.movie.tmdb.app.R

@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adUnitId = stringResource(R.string.admob_banner_ad_unit_id)
    // Held here rather than built in the factory, so the lifecycle effect below can reach it.
    val adView = remember {
        AdView(context).apply {
            this.adUnitId = adUnitId
            setAdSize(AdSize.BANNER)
        }
    }

    AndroidView(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .fillMaxWidth(),
        factory = { adView.apply { loadAd(AdRequest.Builder().build()) } },
        onRelease = { it.destroy() }
    )

    // Stops ad refreshes while the screen is in the background.
    LifecycleResumeEffect(adView) {
        adView.resume()
        onPauseOrDispose { adView.pause() }
    }
}
