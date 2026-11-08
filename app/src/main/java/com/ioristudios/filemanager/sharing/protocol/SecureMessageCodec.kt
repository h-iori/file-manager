package com.ioristudios.filemanager.sharing.protocol

import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encapsulates and decapsulates Google Nearby Connections SecureMessage frames.
 * Employs AES-256-CBC encryption with fresh random IVs and HMAC-SHA256 message signing.
 * Wire-compatible with Google Files / Android Quick Share protocol buffers.
 */
class SecureMessageCodec(
    private val encryptionKey: ByteArray,
    private val decryptionKey: ByteArray,
    private val signingHmacKey: ByteArray,
    private val verifyingHmacKey: ByteArray
) {

    private val secureRandom = SecureRandom()
    private var sendSequenceNumber: Long = 1
    private var receiveSequenceNumber: Long = 1

    private val encKeySpec = SecretKeySpec(encryptionKey, "AES")
    private val decKeySpec = SecretKeySpec(decryptionKey, "AES")
    private val signHmacKeySpec = SecretKeySpec(signingHmacKey, "HmacSHA256")
    private val verifyHmacKeySpec = SecretKeySpec(verifyingHmacKey, "HmacSHA256")

    private val encryptCipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    private val decryptCipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    private val signingMac: Mac = Mac.getInstance("HmacSHA256").apply {
        init(signHmacKeySpec)
    }
    private val verifyingMac: Mac = Mac.getInstance("HmacSHA256").apply {
        init(verifyHmacKeySpec)
    }

    /**
     * Creates an independent copy of this codec with its own cipher and MAC instances
     * for parallel worker channels sharing the same key material.
     */
    fun duplicate(): SecureMessageCodec {
        return SecureMessageCodec(
            encryptionKey = encryptionKey.copyOf(),
            decryptionKey = decryptionKey.copyOf(),
            signingHmacKey = signingHmacKey.copyOf(),
            verifyingHmacKey = verifyingHmacKey.copyOf()
        )
    }

    /**
     * Encrypts plaintext and returns the wire-ready serialized SecureMessage Protobuf bytes.
     * message SecureMessage {
     *   required bytes header_and_body = 1;
     *   required bytes signature = 2;
     * }
     */
    @Synchronized
    fun encrypt(plaintext: ByteArray): ByteArray {
        val iv = ByteArray(16)
        secureRandom.nextBytes(iv)

        encryptCipher.init(Cipher.ENCRYPT_MODE, encKeySpec, IvParameterSpec(iv))
        val ciphertext = encryptCipher.doFinal(plaintext)

        // Header: signature_scheme=1 (HMAC_SHA256), encryption_scheme=1 (AES_256_CBC), iv=16B
        val headerWriter = ProtobufLite.Writer(64)
            .writeInt32(1, 1) // signature_scheme = HMAC_SHA256
            .writeInt32(2, 1) // encryption_scheme = AES_256_CBC
            .writeBytes(3, iv) // iv
            .writeInt64(4, sendSequenceNumber++) // sequence_number for anti-replay

        // HeaderAndBody: field 1 = header, field 2 = body
        val headerBytes = headerWriter.toByteArray()
        val headerAndBodyWriter = ProtobufLite.Writer(headerBytes.size + ciphertext.size + 16)
            .writeBytes(1, headerBytes)
            .writeBytes(2, ciphertext)

        val headerAndBodyBytes = headerAndBodyWriter.toByteArray()

        // Compute HMAC-SHA256 signature over HeaderAndBody bytes
        signingMac.reset()
        val signature = signingMac.doFinal(headerAndBodyBytes)

        // SecureMessage Protobuf: field 1 = header_and_body, field 2 = signature
        val secureMessageWriter = ProtobufLite.Writer(headerAndBodyBytes.size + signature.size + 16)
            .writeBytes(1, headerAndBodyBytes)
            .writeBytes(2, signature)

        return secureMessageWriter.toByteArray()
    }

    /**
     * Auto-detects whether the incoming frame is a standard SecureMessage Protobuf
     * (starting with tag 0x0A) or a legacy byte-envelope, verifies HMAC, and decrypts.
     */
    @Synchronized
    fun decrypt(rawBytes: ByteArray): ByteArray {
        if (rawBytes.isEmpty()) {
            throw GeneralSecurityException("Empty encrypted frame received")
        }

        // Standard Protobuf SecureMessage begins with tag 1 (field 1, wire type 2 -> 0x0A)
        if (rawBytes[0] == 0x0A.toByte()) {
            val reader = ProtobufLite.Reader(rawBytes)
            var headerAndBodyBytes: ByteArray? = null
            var signature: ByteArray? = null

            while (!reader.isAtEnd()) {
                val tag = reader.readTag() ?: break
                when (ProtobufLite.getTagFieldNumber(tag)) {
                    1 -> headerAndBodyBytes = reader.readBytes()
                    2 -> signature = reader.readBytes()
                    else -> reader.skipField(ProtobufLite.getTagWireType(tag))
                }
            }

            if (headerAndBodyBytes != null && signature != null) {
                return decrypt(signature, headerAndBodyBytes)
            }
        }

        // Fallback for raw envelope with 4-byte length prefix + 32-byte signature
        if (rawBytes.size > 36) {
            val signature = rawBytes.copyOfRange(4, 36)
            val headerAndBody = rawBytes.copyOfRange(36, rawBytes.size)
            return decrypt(signature, headerAndBody)
        }

        throw GeneralSecurityException("Unrecognized SecureMessage envelope format")
    }

    /**
     * Verifies HMAC signature, extracts IV & ciphertext, and decrypts to plaintext.
     */
    @Synchronized
    fun decrypt(signature: ByteArray, headerAndBodyBytes: ByteArray): ByteArray {
        // 1. Verify HMAC-SHA256
        verifyingMac.reset()
        val computedSignature = verifyingMac.doFinal(headerAndBodyBytes)

        if (!MessageDigestEquals.isEqual(signature, computedSignature)) {
            throw GeneralSecurityException("HMAC-SHA256 signature verification failed on SecureMessage")
        }

        // 2. Parse HeaderAndBody
        val reader = ProtobufLite.Reader(headerAndBodyBytes)
        var iv = ByteArray(16)
        var ciphertext = ByteArray(0)
        var msgSeq = 0L

        while (!reader.isAtEnd()) {
            val tag = reader.readTag() ?: break
            when (ProtobufLite.getTagFieldNumber(tag)) {
                1 -> {
                    // Header
                    val headerBytes = reader.readBytes()
                    val headerReader = ProtobufLite.Reader(headerBytes)
                    while (!headerReader.isAtEnd()) {
                        val hTag = headerReader.readTag() ?: break
                        when (ProtobufLite.getTagFieldNumber(hTag)) {
                            3 -> iv = headerReader.readBytes()
                            4 -> msgSeq = headerReader.readVarint64()
                            else -> headerReader.skipField(ProtobufLite.getTagWireType(hTag))
                        }
                    }
                }
                2 -> ciphertext = reader.readBytes()
                else -> reader.skipField(ProtobufLite.getTagWireType(tag))
            }
        }

        receiveSequenceNumber++

        // 3. Decrypt AES-256-CBC
        decryptCipher.init(Cipher.DECRYPT_MODE, decKeySpec, IvParameterSpec(iv))
        return decryptCipher.doFinal(ciphertext)
    }

    private object MessageDigestEquals {
        fun isEqual(a: ByteArray, b: ByteArray): Boolean {
            if (a.size != b.size) return false
            var result = 0
            for (i in a.indices) {
                result = result or (a[i].toInt() xor b[i].toInt())
            }
            return result == 0
        }
    }
}
