package com.movie.tmdb.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.movie.tmdb.app.R
import com.movie.tmdb.app.TmdbApplication

/**
 * An anchored adaptive banner. [visible] is false while the screen has no content of its own
 * (loading, an error, an empty list): AdMob does not allow ads on screens like that. The ad then
 * collapses and stops refreshing, but the AdView survives, so coming back does not reload it.
 */
@Composable
fun AdMobBanner(visible: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adsConsent = remember { (context.applicationContext as TmdbApplication).container.adsConsent }
    val canRequestAds by adsConsent.canRequestAds.collectAsStateWithLifecycle()
    if (!canRequestAds) return

    val adUnitId = stringResource(R.string.admob_banner_ad_unit_id)
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    // Held here rather than built in the factory, so the lifecycle effect below can reach it.
    val adView = remember(screenWidthDp) {
        AdView(context).apply {
            this.adUnitId = adUnitId
            // Full width, with the height Google picks for that width.
            setAdSize(AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, screenWidthDp))
        }
    }
    var requested by remember(adView) { mutableStateOf(false) }

    AndroidView(
        modifier = modifier
            .then(
                if (visible) Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                else Modifier.height(0.dp)
            )
            .fillMaxWidth(),
        factory = { adView },
        update = { it.isVisible = visible },
        onRelease = { it.destroy() }
    )

    // Refreshes only while the screen is resumed and has content. The first request waits for
    // that too, so an ad is never fetched for a screen that cannot show it.
    LifecycleResumeEffect(adView, visible) {
        if (visible) {
            if (!requested) {
                adView.loadAd(AdRequest.Builder().build())
                requested = true
            }
            adView.resume()
        } else {
            adView.pause()
        }
        onPauseOrDispose { adView.pause() }
    }
}
