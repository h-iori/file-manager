package com.ioristudios.filemanager.sharing.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.security.SecureRandom
import java.util.ArrayDeque

/**
 * Manages mDNS / DNS-SD advertising and discovery for Google Nearby Connections / Quick Share.
 * Service Type: `_FC9F5ED42C8A._tcp.`
 * Fully interoperable with Google Files and Android Quick Share receivers and senders.
 */
class NearbyMdnsManager(private val context: Context) {

    companion object {
        private const val TAG = "NearbyMdnsManager"
        const val SERVICE_TYPE = "_FC9F5ED42C8A._tcp."
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val _discoveredPeers = MutableStateFlow<Map<String, DiscoveredPeer>>(emptyMap())
    val discoveredPeers: StateFlow<Map<String, DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    val localEndpointId = generateEndpointId()

    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var isResolving = false

    private fun generateEndpointId(): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val rnd = SecureRandom()
        return (1..4)
            .map { allowedChars[rnd.nextInt(allowedChars.length)] }
            .joinToString("")
    }

    /**
     * Constructs the Quick Share compliant service instance name containing endpoint info
     * and local device name so Google Files / Android Quick Share recognizes our device:
     * [0x22, epId (4 bytes), ServiceIdHash (3 bytes), 0x0B, 0x6A, DeviceType, Salt (6 bytes), NameLen, NameBytes]
     */
    fun createServiceInstanceName(endpointId: String, localDeviceName: String): String {
        val epBytes = endpointId.toByteArray(Charsets.US_ASCII)
        val nameBytes = localDeviceName.toByteArray(Charsets.UTF_8).let {
            if (it.size > 32) it.copyOfRange(0, 32) else it
        }
        val salt = ByteArray(6)
        SecureRandom().nextBytes(salt)

        val totalSize = 10 + 1 + 6 + 1 + nameBytes.size
        val bytes = ByteArray(totalSize)
        bytes[0] = 0x22 // PCP version byte (P2P_CLUSTER)
        System.arraycopy(epBytes, 0, bytes, 1, minOf(4, epBytes.size))
        bytes[5] = 0x6E.toByte() // Service ID hash
        bytes[6] = 0x8C.toByte()
        bytes[7] = 0x31.toByte()
        bytes[8] = 0x0B.toByte()
        bytes[9] = 0x6A.toByte()
        // EndpointInfo
        bytes[10] = 0x47.toByte() // Phone device type
        System.arraycopy(salt, 0, bytes, 11, 6)
        bytes[17] = nameBytes.size.toByte()
        System.arraycopy(nameBytes, 0, bytes, 18, nameBytes.size)

        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /**
     * Starts advertising the local device on mDNS (Receiver Mode)
     */
    fun startAdvertising(port: Int, localDeviceName: String) {
        stopAdvertising()
        val nsd = nsdManager ?: return

        val serviceInfo = NsdServiceInfo().apply {
            serviceType = SERVICE_TYPE
            serviceName = createServiceInstanceName(localEndpointId, localDeviceName)
            setPort(port)
            // Add EndpointInfo TXT record for full backward compatibility
            setAttribute("n", localDeviceName)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo?) {
                Log.d(TAG, "mDNS Service registered: ${info?.serviceName} on port $port")
            }

            override fun onRegistrationFailed(info: NsdServiceInfo?, errorCode: Int) {
                Log.e(TAG, "mDNS Service registration failed with code: $errorCode")
            }

            override fun onServiceUnregistered(info: NsdServiceInfo?) {
                Log.d(TAG, "mDNS Service unregistered")
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo?, errorCode: Int) {
                Log.e(TAG, "mDNS Service unregistration failed with code: $errorCode")
            }
        }

        try {
            nsd.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call registerService", e)
        }
    }

    /**
     * Stops mDNS advertising
     */
    fun stopAdvertising() {
        val listener = registrationListener ?: return
        try {
            nsdManager?.unregisterService(listener)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering mDNS service", e)
        } finally {
            registrationListener = null
        }
    }

    /**
     * Starts discovering nearby sharing devices (Sender Mode)
     */
    fun startDiscovery() {
        stopDiscovery()
        _discoveredPeers.value = emptyMap()
        synchronized(resolveQueue) {
            resolveQueue.clear()
            isResolving = false
        }
        val nsd = nsdManager ?: return

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery start failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e(TAG, "Discovery stop failed: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d(TAG, "mDNS Discovery started for: $serviceType")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d(TAG, "mDNS Discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo == null) return
                Log.d(TAG, "mDNS Service found: ${serviceInfo.serviceName}")

                enqueueResolve(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo == null) return
                val name = serviceInfo.serviceName ?: return
                _discoveredPeers.value = _discoveredPeers.value.filterValues { it.endpointId != name }
            }
        }

        try {
            nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call discoverServices", e)
        }
    }

    private fun enqueueResolve(serviceInfo: NsdServiceInfo) {
        synchronized(resolveQueue) {
            // Avoid queueing duplicates
            if (resolveQueue.none { it.serviceName == serviceInfo.serviceName }) {
                resolveQueue.add(serviceInfo)
            }
            processNextResolveLocked()
        }
    }

    private fun processNextResolveLocked() {
        if (isResolving || resolveQueue.isEmpty()) return
        val nextService = resolveQueue.poll() ?: return
        val nsd = nsdManager ?: return

        isResolving = true
        try {
            nsd.resolveService(nextService, object : NsdManager.ResolveListener {
                override fun onResolveFailed(info: NsdServiceInfo?, errorCode: Int) {
                    Log.w(TAG, "Resolve failed for ${info?.serviceName}: $errorCode")
                    synchronized(resolveQueue) {
                        isResolving = false
                        processNextResolveLocked()
                    }
                }

                override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                    if (resolvedInfo != null) {
                        handleResolvedService(resolvedInfo)
                    }
                    synchronized(resolveQueue) {
                        isResolving = false
                        processNextResolveLocked()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving service", e)
            isResolving = false
            processNextResolveLocked()
        }
    }

    private fun handleResolvedService(resolvedInfo: NsdServiceInfo) {
        val host: InetAddress? = resolvedInfo.host
        val port = resolvedInfo.port
        val rawName = resolvedInfo.serviceName ?: ""

        val (endpointId, deviceName) = extractDeviceMetadata(rawName, resolvedInfo.attributes)

        Log.d(TAG, "Parsed Peer: endpointId=$endpointId, deviceName='$deviceName', host=$host, port=$port")

        if (host != null && port > 0) {
            val peer = DiscoveredPeer(
                endpointId = endpointId,
                deviceName = deviceName,
                deviceType = DeviceType.PHONE,
                hostAddress = host.hostAddress,
                port = port,
                lastSeenTimestamp = System.currentTimeMillis()
            )
            _discoveredPeers.value = _discoveredPeers.value + (endpointId to peer)
        }
    }

    /**
     * Accurately parses Google Quick Share & Nearby Share base64 instance names and TXT records
     * to extract real device names (e.g. "Harsh's phone") and endpoint IDs.
     */
    private fun extractDeviceMetadata(
        rawName: String,
        attributes: Map<String, ByteArray>?
    ): Pair<String, String> {
        var endpointId = rawName
        var deviceName: String? = null

        // 1. Check TXT record attribute "n"
        val nAttr = attributes?.get("n")
        if (nAttr != null && nAttr.isNotEmpty()) {
            try {
                val nStr = String(nAttr, Charsets.UTF_8).trim()
                if (nStr.matches(Regex("^[A-Za-z0-9_-]+$"))) {
                    val decoded = Base64.decode(nStr, Base64.URL_SAFE)
                    if (decoded.size > 17 && decoded[17].toInt() in 1..(decoded.size - 18)) {
                        val nameLen = decoded[17].toInt() and 0xFF
                        deviceName = String(decoded, 18, nameLen, Charsets.UTF_8)
                    }
                }
                if (deviceName.isNullOrBlank()) {
                    deviceName = nStr
                }
            } catch (_: Exception) {}
        }

        // 2. Parse from Base64URL serviceName
        try {
            val decoded = Base64.decode(rawName, Base64.URL_SAFE)
            if (decoded.size >= 5) {
                val candidateEp = String(decoded, 1, minOf(4, decoded.size - 1), Charsets.US_ASCII)
                if (candidateEp.all { it.isLetterOrDigit() }) {
                    endpointId = candidateEp
                } else {
                    endpointId = decoded.slice(1..4).joinToString("") { "%02X".format(it) }
                }
            }

            if (deviceName.isNullOrBlank() && decoded.size > 17) {
                val nameLen = decoded[17].toInt() and 0xFF
                if (nameLen in 1..(decoded.size - 18)) {
                    val candidate = String(decoded, 18, nameLen, Charsets.UTF_8).trim()
                    if (candidate.isNotBlank() && candidate.all { it.code in 32..126 || it.code > 127 }) {
                        deviceName = candidate
                    }
                }

                // Fallback tail scan for valid UTF-8 string
                if (deviceName.isNullOrBlank()) {
                    for (start in (decoded.size - 1) downTo 10) {
                        val candidate = String(decoded, start, decoded.size - start, Charsets.UTF_8).trim()
                        if (candidate.length >= 3 && candidate.all { it.code in 32..126 }) {
                            deviceName = candidate
                            break
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        val finalName = if (!deviceName.isNullOrBlank()) {
            deviceName
        } else {
            "Quick Share Device (${endpointId.take(6)})"
        }

        return Pair(endpointId, finalName)
    }

    /**
     * Stops mDNS discovery
     */
    fun stopDiscovery() {
        val listener = discoveryListener ?: return
        try {
            nsdManager?.stopServiceDiscovery(listener)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping discovery", e)
        } finally {
            discoveryListener = null
            synchronized(resolveQueue) {
                resolveQueue.clear()
                isResolving = false
            }
        }
    }

    fun clear() {
        stopAdvertising()
        stopDiscovery()
        _discoveredPeers.value = emptyMap()
    }
}
