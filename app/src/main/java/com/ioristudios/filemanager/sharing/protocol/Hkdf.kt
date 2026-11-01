package com.ioristudios.filemanager.sharing.protocol

import java.io.ByteArrayOutputStream
import java.security.GeneralSecurityException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ceil

/**
 * RFC 5869 HMAC-based Extract-and-Expand Key Derivation Function (HKDF)
 * Implemented using standard Java Cryptography Architecture (JCA) Mac("HmacSHA256").
 */
object Hkdf {

    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val HASH_LEN = 32 // SHA-256 output length in bytes

    /**
     * HKDF-Extract(salt, IKM) -> PRK
     */
    fun extract(salt: ByteArray?, ikm: ByteArray): ByteArray {
        val effectiveSalt = if (salt == null || salt.isEmpty()) {
            ByteArray(HASH_LEN)
        } else {
            salt
        }
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(effectiveSalt, HMAC_ALGORITHM))
        return mac.doFinal(ikm)
    }

    /**
     * HKDF-Expand(PRK, info, length) -> OKM
     */
    fun expand(prk: ByteArray, info: ByteArray?, length: Int): ByteArray {
        if (length <= 0) return ByteArray(0)
        val n = ceil(length.toDouble() / HASH_LEN.toDouble()).toInt()
        if (n > 255) {
            throw GeneralSecurityException("HKDF cannot expand to more than 255 * HashLen bytes ($length requested)")
        }

        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(prk, HMAC_ALGORITHM))

        val result = ByteArray(length)
        var t = ByteArray(0)
        var offset = 0

        for (i in 1..n) {
            mac.reset()
            if (t.isNotEmpty()) {
                mac.update(t)
            }
            if (info != null && info.isNotEmpty()) {
                mac.update(info)
            }
            mac.update(i.toByte())
            t = mac.doFinal()

            val bytesToCopy = minOf(t.size, length - offset)
            System.arraycopy(t, 0, result, offset, bytesToCopy)
            offset += bytesToCopy
        }

        return result
    }

    /**
     * Complete HKDF: Extract then Expand
     */
    fun deriveKey(salt: ByteArray?, ikm: ByteArray, info: ByteArray?, length: Int): ByteArray {
        val prk = extract(salt, ikm)
        return expand(prk, info, length)
    }
}

