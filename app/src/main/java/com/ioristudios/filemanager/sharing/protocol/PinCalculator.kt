package com.ioristudios.filemanager.sharing.protocol

import java.security.MessageDigest
import kotlin.math.abs

/**
 * Derives the exact 4-digit human-readable verification PIN matching Google Files,
 * Android Quick Share, and Nearby Share.
 */
object PinCalculator {

    /**
     * Calculates the 4-digit PIN code from the UKEY2 authentication token.
     */
    fun calculatePin(authToken: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(authToken)
        val value = ((digest[0].toInt() and 0xFF) shl 24) or
                    ((digest[1].toInt() and 0xFF) shl 16) or
                    ((digest[2].toInt() and 0xFF) shl 8) or
                    (digest[3].toInt() and 0xFF)
        val pin = (abs(value) % 9000) + 1000
        return pin.toString()
    }

    /**
     * Alternative 6-digit PIN calculation if peer requests 6-digit mode.
     */
    fun calculatePin6(authToken: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(authToken)
        val value = ((digest[0].toLong() and 0xFF) shl 24) or
                    ((digest[1].toLong() and 0xFF) shl 16) or
                    ((digest[2].toLong() and 0xFF) shl 8) or
                    (digest[3].toLong() and 0xFF)
        val pin = (abs(value) % 900000L) + 100000L
        return pin.toString()
    }
}

