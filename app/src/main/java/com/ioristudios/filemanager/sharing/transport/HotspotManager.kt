package com.ioristudios.filemanager.sharing.transport

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Manages Android Local-Only Hotspot for peer-to-peer file sharing
 * when devices are NOT connected to the same Wi-Fi network (or router blocks LAN traffic).
 */
class HotspotManager(private val context: Context) {

    companion object {
        private const val TAG = "HotspotManager"
    }

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null

    var isHotspotActive: Boolean = false
        private set

    var activeSsid: String? = null
        private set
    var activePassword: String? = null
        private set

    /**
     * Starts a localized direct Wi-Fi hotspot on the device.
     */
    fun startHotspot(
        onStarted: (ssid: String, password: String) -> Unit,
        onFailed: (reason: String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onFailed("Local hotspot requires Android 8.0 (API 26) or higher")
            return
        }

        val wm = wifiManager
        if (wm == null) {
            onFailed("WifiManager service unavailable")
            return
        }

        try {
            wm.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(res: WifiManager.LocalOnlyHotspotReservation?) {
                    super.onStarted(res)
                    reservation = res
                    isHotspotActive = true

                    val ssid: String?
                    val pass: String?

                    @Suppress("DEPRECATION")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val config = res?.softApConfiguration
                        ssid = config?.ssid
                        pass = config?.passphrase
                    } else {
                        val config = res?.wifiConfiguration
                        ssid = config?.SSID
                        pass = config?.preSharedKey
                    }

                    activeSsid = ssid ?: "Files-Direct-Share"
                    activePassword = pass ?: ""

                    Log.d(TAG, "Hotspot started: SSID=$activeSsid, Pass=$activePassword")
                    onStarted(activeSsid!!, activePassword!!)
                }

                override fun onStopped() {
                    super.onStopped()
                    isHotspotActive = false
                    reservation = null
                    activeSsid = null
                    activePassword = null
                    Log.d(TAG, "Hotspot stopped")
                }

                override fun onFailed(reason: Int) {
                    super.onFailed(reason)
                    isHotspotActive = false
                    val errorMsg = when (reason) {
                        ERROR_NO_CHANNEL -> "Hotspot failed: no Wi-Fi channel available"
                        ERROR_GENERIC -> "Hotspot failed: generic Wi-Fi system error"
                        ERROR_INCOMPATIBLE_MODE -> "Hotspot failed: Wi-Fi mode incompatible"
                        ERROR_TETHERING_DISALLOWED -> "Hotspot failed: tethering disallowed by system"
                        else -> "Hotspot failed with error code $reason"
                    }
                    Log.e(TAG, errorMsg)
                    onFailed(errorMsg)
                }
            }, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting local hotspot", e)
            onFailed(e.message ?: "Could not start local hotspot")
        }
    }

    fun stopHotspot() {
        try {
            reservation?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing hotspot reservation", e)
        } finally {
            reservation = null
            isHotspotActive = false
            activeSsid = null
            activePassword = null
        }
    }

    /**
     * Inspects active network interfaces and extracts the local IPv4 address
     * (e.g., 192.168.0.x on Wi-Fi LAN, 192.168.43.1 on Hotspot).
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue

                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress
                        if (host != null && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }
}

