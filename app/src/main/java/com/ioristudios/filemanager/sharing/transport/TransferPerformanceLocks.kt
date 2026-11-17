package com.ioristudios.filemanager.sharing.transport

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.util.Log

/**
 * Keeps the Wi-Fi radio and CPU at full performance for the duration of a transfer.
 *
 * Without these, Android drops Wi-Fi into power-save (802.11 PSM) as soon as the screen dims
 * or the app is backgrounded, which can cut throughput by 5-10x, and may doze the CPU
 * mid-transfer. Reference counted so nested acquire/release pairs are safe.
 */
class TransferPerformanceLocks(context: Context) {

    companion object {
        private const val TAG = "TransferPerfLocks"
        private const val LOCK_TAG = "FileManager:ShareTransfer"
        private const val WAKE_LOCK_TIMEOUT_MS = 3L * 60L * 60L * 1000L // 3h safety cap
    }

    private val appContext = context.applicationContext
    private var refCount = 0
    private var highPerfLock: WifiManager.WifiLock? = null
    private var lowLatencyLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    @Synchronized
    @SuppressLint("WakelockTimeout")
    fun acquire() {
        refCount++
        if (refCount > 1) return
        try {
            val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifi != null) {
                @Suppress("DEPRECATION")
                highPerfLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, LOCK_TAG).apply {
                    setReferenceCounted(false)
                    acquire()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Disables power save while the app is foreground with screen on (API 29+).
                    lowLatencyLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "$LOCK_TAG:LL").apply {
                        setReferenceCounted(false)
                        acquire()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Wi-Fi lock unavailable: ${e.message}")
        }
        try {
            val power = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = power?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_TAG)?.apply {
                setReferenceCounted(false)
                acquire(WAKE_LOCK_TIMEOUT_MS)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Wake lock unavailable: ${e.message}")
        }
    }

    @Synchronized
    fun release() {
        if (refCount == 0) return
        refCount--
        if (refCount > 0) return
        try { highPerfLock?.takeIf { it.isHeld }?.release() } catch (_: Exception) {}
        try { lowLatencyLock?.takeIf { it.isHeld }?.release() } catch (_: Exception) {}
        try { wakeLock?.takeIf { it.isHeld }?.release() } catch (_: Exception) {}
        highPerfLock = null
        lowLatencyLock = null
        wakeLock = null
    }
}
