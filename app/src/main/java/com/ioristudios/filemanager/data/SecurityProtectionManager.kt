package com.ioristudios.filemanager.data

import java.io.File
import java.util.Locale

enum class ProtectionLevel {
    NONE,
    SCOPED_STORAGE_RESTRICTED, // Android/data, Android/obb
    SYSTEM_CORE_LOCKED         // /system, /vendor, /data, /apex, /root, /boot, /etc, etc.
}

data class DirectoryProtectionInfo(
    val isProtected: Boolean,
    val level: ProtectionLevel,
    val title: String,
    val description: String,
    val allowBrowsing: Boolean = true,
    val allowModification: Boolean = true
)

object SecurityProtectionManager {

    private val SYSTEM_ROOT_NAMES = setOf(
        "system", "vendor", "apex", "data", "boot", "etc", "sbin",
        "proc", "sys", "dev", "product", "odm", "system_ext", "lost+found"
    )

    fun checkProtection(path: String): DirectoryProtectionInfo {
        val normalized = path.replace('\\', '/').trimEnd('/')
        val lower = normalized.lowercase(Locale.ROOT)
        val file = File(path)
        val fileName = file.name.lowercase(Locale.ROOT)

        // 1. Android Scoped Storage sensitive directories: Android/data & Android/obb
        val isDataOrObb = lower.endsWith("/android/data") || lower.endsWith("/android/obb") ||
                (fileName == "data" && file.parentFile?.name?.equals("Android", ignoreCase = true) == true) ||
                (fileName == "obb" && file.parentFile?.name?.equals("Android", ignoreCase = true) == true)

        if (isDataOrObb) {
            return DirectoryProtectionInfo(
                isProtected = true,
                level = ProtectionLevel.SCOPED_STORAGE_RESTRICTED,
                title = "Scoped Storage Protected",
                description = "Google's Scoped Storage policy (Android 11+) protects this directory to safeguard application caches, user logins, and private data.",
                allowBrowsing = true,
                allowModification = true
            )
        }

        // Subdirectories inside Android/data or Android/obb (app private containers)
        if (lower.contains("/android/data/") || lower.contains("/android/obb/")) {
            return DirectoryProtectionInfo(
                isProtected = true,
                level = ProtectionLevel.SCOPED_STORAGE_RESTRICTED,
                title = "App Sandbox Container",
                description = "This folder contains private app state. Modifying or deleting it directly can affect app data or cause crashes.",
                allowBrowsing = true,
                allowModification = true
            )
        }

        // Top-level Android directory in storage
        if (fileName == "android" && (lower.endsWith("/android") || file.parentFile?.name?.equals("0") == true)) {
            return DirectoryProtectionInfo(
                isProtected = true,
                level = ProtectionLevel.SCOPED_STORAGE_RESTRICTED,
                title = "Android System Storage",
                description = "Core application data root. Sensitive container for installed applications.",
                allowBrowsing = true,
                allowModification = true
            )
        }

        // 2. Sensitive core root and system partitions
        val isRootSystemPartition = normalized.startsWith("/system") ||
                normalized.startsWith("/vendor") ||
                normalized.startsWith("/apex") ||
                (normalized.startsWith("/data") && !normalized.startsWith("/data/media")) ||
                normalized.startsWith("/proc") ||
                normalized.startsWith("/sys") ||
                normalized.startsWith("/dev") ||
                normalized.startsWith("/etc") ||
                normalized.startsWith("/sbin")

        if (isRootSystemPartition) {
            return DirectoryProtectionInfo(
                isProtected = true,
                level = ProtectionLevel.SYSTEM_CORE_LOCKED,
                title = "System Core Locked",
                description = "Core operating system partition protected by Android SELinux security policies to guarantee system stability.",
                allowBrowsing = true,
                allowModification = true
            )
        }

        // Encrypted containers
        if (fileName == ".android_secure") {
            return DirectoryProtectionInfo(
                isProtected = true,
                level = ProtectionLevel.SYSTEM_CORE_LOCKED,
                title = "Encrypted System Container",
                description = "Secure system storage area for encrypted application packages.",
                allowBrowsing = true,
                allowModification = true
            )
        }

        return DirectoryProtectionInfo(
            isProtected = false,
            level = ProtectionLevel.NONE,
            title = "",
            description = "",
            allowBrowsing = true,
            allowModification = true
        )
    }

    /**
     * Checks if a path targets a sensitive system/scoped directory requiring a secondary disclaimer.
     */
    fun isSensitiveItem(path: String): Boolean {
        return checkProtection(path).isProtected
    }
}
