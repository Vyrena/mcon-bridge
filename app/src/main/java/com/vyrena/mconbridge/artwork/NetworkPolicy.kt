package com.vyrena.mconbridge.artwork

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class NetworkPolicy(context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    fun canDownload(wifiOnly: Boolean): Boolean {
        if (!wifiOnly) return true
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
