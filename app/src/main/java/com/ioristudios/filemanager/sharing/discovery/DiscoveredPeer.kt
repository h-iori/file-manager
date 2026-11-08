package com.ioristudios.filemanager.sharing.discovery

/**
 * Represents a nearby peer device discovered via mDNS, BLE, or Wi-Fi Direct.
 */
data class DiscoveredPeer(
    val endpointId: String,
    val deviceName: String,
    val deviceType: DeviceType = DeviceType.PHONE,
    val hostAddress: String? = null,
    val port: Int = 0,
    val rssi: Int = 0,
    val isFastApp: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

enum class DeviceType {
    PHONE,
    TABLET,
    COMPUTER,
    UNKNOWN
}

