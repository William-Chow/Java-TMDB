package com.movie.tmdb.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Google's consent SDK (UMP) gates every ad: users in the EEA, UK and Switzerland are asked first,
 * and everyone else comes back as not required. The Ads SDK is initialized, and banners load,
 * only once [canRequestAds] is true.
 */
class AdsConsent(context: Context) {

    private val appContext = context.applicationContext
    private val consentInformation = UserMessagingPlatform.getConsentInformation(appContext)
    private val adsInitialized = AtomicBoolean(false)

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    /** True where the user was asked: they must be able to reopen their choices from the app. */
    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    init {
        // A choice made in an earlier session lets ads start before this session's update returns.
        refresh()
    }

    /** Refreshes the consent status and shows the form if this user still has to answer it. */
    fun gather(activity: Activity) {
        consentInformation.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                // The activity can be gone by now (rotated or closed); the next launch asks again.
                if (activity.isFinishing || activity.isDestroyed) {
                    refresh()
                } else {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        logIfFailed("Consent form", formError)
                        refresh()
                    }
                }
            },
            { requestError ->
                // Offline or misconfigured: fall back to whatever the user decided last time.
                logIfFailed("Consent info update", requestError)
                refresh()
            }
        )
    }

    fun showPrivacyOptionsForm(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            logIfFailed("Privacy options form", formError)
            refresh()
        }
    }

    private fun refresh() {
        _privacyOptionsRequired.value =
            consentInformation.privacyOptionsRequirementStatus == PrivacyOptionsRequirementStatus.REQUIRED
        val canRequest = consentInformation.canRequestAds()
        _canRequestAds.value = canRequest
        if (canRequest && adsInitialized.compareAndSet(false, true)) {
            // Once per process, and off the main thread: the SDK's initialization blocks on I/O.
            CoroutineScope(Dispatchers.IO).launch { MobileAds.initialize(appContext) }
        }
    }

    private fun logIfFailed(step: String, error: FormError?) {
        if (error != null) Log.w(TAG, "$step failed (${error.errorCode}): ${error.message}")
    }

    private companion object {
        const val TAG = "AdsConsent"
    }
}
