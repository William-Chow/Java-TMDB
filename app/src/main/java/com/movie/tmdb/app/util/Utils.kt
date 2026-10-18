package com.movie.tmdb.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object Utils {

    const val IMAGE_URL = "https://image.tmdb.org/t/p/w500"

    // Check Internet Connection. Asks about the capability rather than the transport, so
    // Ethernet, Bluetooth tethering and anything else that reaches the internet counts too.
    fun getConnectionType(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
