package com.richardittou.qrcodenow.domain.action

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiNetworkSpecifier
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri
import com.richardittou.qrcodenow.domain.model.QrContent
import java.util.concurrent.ConcurrentHashMap

interface ExternalActionLauncher {
    fun open(context: Context, content: QrContent): Result<Unit>
    fun openUrl(context: Context, url: String): Result<Unit>
    fun shareText(context: Context, text: String): Result<Unit>
    fun connectWifi(context: Context, wifi: QrContent.Wifi): Result<Unit>
}

class AndroidExternalActionLauncher : ExternalActionLauncher {
    private val activeWifiCallbacks = ConcurrentHashMap<ConnectivityManager, ConnectivityManager.NetworkCallback>()

    override fun open(context: Context, content: QrContent): Result<Unit> = runCatching {
        val intent = when (content) {
            is QrContent.Url -> Intent(Intent.ACTION_VIEW, content.raw.toUri())
            is QrContent.Phone -> Intent(Intent.ACTION_DIAL, "tel:${Uri.encode(content.number)}".toUri())
            is QrContent.Sms -> Intent(Intent.ACTION_SENDTO, "smsto:${Uri.encode(content.number)}".toUri()).apply {
                content.message?.let { putExtra("sms_body", it) }
            }
            is QrContent.Email -> Intent(Intent.ACTION_SENDTO, "mailto:${Uri.encode(content.address)}".toUri()).apply {
                content.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                content.body?.let { putExtra(Intent.EXTRA_TEXT, it) }
            }
            is QrContent.Location -> Intent(Intent.ACTION_VIEW, "geo:${content.latitude},${content.longitude}?q=${content.latitude},${content.longitude}".toUri())
            is QrContent.AppLink -> Intent(Intent.ACTION_VIEW, content.raw.toUri())
            else -> throw IllegalArgumentException("Não há ação externa para esse conteúdo")
        }
        launch(context, intent)
    }

    override fun openUrl(context: Context, url: String): Result<Unit> = runCatching {
        val uri = url.toUri()
        require(uri.scheme?.lowercase() in setOf("http", "https"))
        launch(context, Intent(Intent.ACTION_VIEW, uri))
    }

    override fun shareText(context: Context, text: String): Result<Unit> = runCatching {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        launch(context, Intent.createChooser(send, "Compartilhar QR Code"))
    }

    override fun connectWifi(context: Context, wifi: QrContent.Wifi): Result<Unit> = runCatching {
        require(wifi.ssid.isNotBlank()) { "Nome da rede Wi-Fi ausente" }
        val specifierBuilder = WifiNetworkSpecifier.Builder().setSsid(wifi.ssid)
        val password = wifi.password.orEmpty()
        when (wifi.security.uppercase()) {
            "", "NOPASS", "OPEN" -> Unit
            "WPA3", "SAE" -> specifierBuilder.setWpa3Passphrase(password.also { require(it.length >= 8) })
            "WPA", "WPA2", "WPA/WPA2" ->
                specifierBuilder.setWpa2Passphrase(password.also { require(it.length >= 8) })
            else -> throw IllegalArgumentException("Tipo de segurança Wi-Fi não compatível")
        }
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifierBuilder.build())
            .build()
        activeWifiCallbacks.remove(connectivityManager)?.let { old ->
            runCatching { connectivityManager.unregisterNetworkCallback(old) }
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = Unit
            override fun onLost(network: Network) = releaseWifiCallback(connectivityManager, this)
            override fun onUnavailable() = releaseWifiCallback(connectivityManager, this)
        }
        activeWifiCallbacks[connectivityManager] = callback
        connectivityManager.requestNetwork(request, callback)
        Handler(Looper.getMainLooper()).postDelayed(
            { releaseWifiCallback(connectivityManager, callback) },
            WIFI_REQUEST_TIMEOUT_MS
        )
    }

    private fun launch(context: Context, intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun releaseWifiCallback(
        connectivityManager: ConnectivityManager,
        callback: ConnectivityManager.NetworkCallback
    ) {
        if (activeWifiCallbacks.remove(connectivityManager, callback)) {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
    }

    private companion object { const val WIFI_REQUEST_TIMEOUT_MS = 10 * 60_000L }
}
