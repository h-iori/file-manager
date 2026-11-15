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

    companion object {
        /** Upper bound on [encryptPayloadChunk] output for a chunk of [dataLen] bytes (incl. cipher slack). */
        fun maxEncryptedPayloadSize(dataLen: Int): Int = dataLen + 256
    }

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

    /** Once a peer has sent sequence numbers, every later message on this codec must carry one. */
    private var sequenceEnforced = false

    /**
     * Anti-replay check: each message must carry exactly the next sequence number for this
     * codec (one codec per socket and direction). Replayed, reordered, dropped or
     * cross-socket frames are therefore rejected. Called only after the HMAC verified.
     */
    private fun verifySequence(seq: Long?) {
        if (seq == null) {
            if (sequenceEnforced) throw GeneralSecurityException("SecureMessage is missing its sequence number")
            return
        }
        if (seq != receiveSequenceNumber) {
            throw GeneralSecurityException(
                "Replayed or out-of-order SecureMessage (expected #$receiveSequenceNumber, got #$seq)"
            )
        }
        sequenceEnforced = true
        receiveSequenceNumber++
    }

    /**
     * Derives an independent codec for a parallel data channel (e.g. "channel-2"). Each channel gets
     * its own keys, so a frame captured on one socket can never be accepted on another.
     * Both peers derive the same keys (sender's encryption key == receiver's decryption key).
     */
    fun deriveChannelCodec(label: String): SecureMessageCodec {
        val salt = "FastShare channel keys v1".toByteArray(Charsets.UTF_8)
        val info = label.toByteArray(Charsets.UTF_8)
        return SecureMessageCodec(
            encryptionKey = Hkdf.deriveKey(salt, encryptionKey, info, 32),
            decryptionKey = Hkdf.deriveKey(salt, decryptionKey, info, 32),
            signingHmacKey = Hkdf.deriveKey(salt, signingHmacKey, info, 32),
            verifyingHmacKey = Hkdf.deriveKey(salt, verifyingHmacKey, info, 32)
        )
    }

    // Scratch space reused by the zero-copy hot path (guarded by @Synchronized).
    private val prefixScratch = ByteArray(64)
    private val ivScratch = ByteArray(16)
    private val macScratch = ByteArray(32)

    /**
     * Zero-copy equivalent of `encrypt(NearbyFrames.encodePayloadTransfer(...))`.
     *
     * Produces byte-identical wire output (same SecureMessage layout, AES-256-CBC + HMAC-SHA256),
     * but encrypts the chunk straight from [data] into [out] with no intermediate buffers.
     * The encoded message is written to [out] starting at [outOffset]; returns its length.
     * [out] must be at least `outOffset + maxEncryptedPayloadSize(dataLen)` bytes.
     */
    @Synchronized
    fun encryptPayloadChunk(
        payloadId: Long,
        offset: Long,
        data: ByteArray,
        dataOffset: Int,
        dataLen: Int,
        isLast: Boolean,
        out: ByteArray,
        outOffset: Int
    ): Int {
        val prefixLen = NearbyFrames.writePayloadTransferPrefix(prefixScratch, payloadId, offset, dataLen)
        val suffix = NearbyFrames.payloadTransferSuffix(isLast)
        val plaintextLen = prefixLen + dataLen + suffix.size
        val ciphertextLen = (plaintextLen / 16 + 1) * 16 // PKCS5 always pads 1..16 bytes

        secureRandom.nextBytes(ivScratch)
        val seq = sendSequenceNumber++

        // Header { 1: sig scheme, 2: enc scheme, 3: iv, 4: sequence }
        val headerLen = 2 + 2 + 2 + 16 + 1 + ProtobufLite.varintSize64(seq)
        val headerAndBodyLen = 1 + ProtobufLite.varintSize32(headerLen) + headerLen +
            1 + ProtobufLite.varintSize32(ciphertextLen) + ciphertextLen

        var p = outOffset
        out[p++] = 0x0A // SecureMessage.header_and_body
        p = ProtobufLite.putVarint32(out, p, headerAndBodyLen)
        val habStart = p
        out[p++] = 0x0A // HeaderAndBody.header
        p = ProtobufLite.putVarint32(out, p, headerLen)
        out[p++] = 0x08; out[p++] = 1 // signature_scheme = HMAC_SHA256
        out[p++] = 0x10; out[p++] = 1 // encryption_scheme = AES_256_CBC
        out[p++] = 0x1A; out[p++] = 16 // iv
        System.arraycopy(ivScratch, 0, out, p, 16); p += 16
        out[p++] = 0x20 // sequence_number
        p = ProtobufLite.putVarint64(out, p, seq)
        out[p++] = 0x12 // HeaderAndBody.body
        p = ProtobufLite.putVarint32(out, p, ciphertextLen)

        encryptCipher.init(Cipher.ENCRYPT_MODE, encKeySpec, IvParameterSpec(ivScratch))
        p += encryptCipher.update(prefixScratch, 0, prefixLen, out, p)
        if (dataLen > 0) p += encryptCipher.update(data, dataOffset, dataLen, out, p)
        p += encryptCipher.doFinal(suffix, 0, suffix.size, out, p)

        if (p - habStart != headerAndBodyLen) {
            throw GeneralSecurityException("Unexpected ciphertext length while encoding payload")
        }

        signingMac.reset()
        signingMac.update(out, habStart, headerAndBodyLen)
        out[p++] = 0x12 // SecureMessage.signature
        out[p++] = 32
        signingMac.doFinal(out, p)
        p += 32
        return p - outOffset
    }

    /**
     * Zero-copy decrypt: verifies and decrypts the SecureMessage in raw[0, rawLen) and writes
     * the plaintext into [out] (which must be at least rawLen bytes). Returns plaintext length.
     * Falls back to [decrypt] for non-standard envelopes.
     */
    @Synchronized
    fun decryptInto(raw: ByteArray, rawLen: Int, out: ByteArray): Int {
        if (rawLen > 0 && raw[0] == 0x0A.toByte()) {
            var habStart = -1
            var habLen = 0
            var sigStart = -1
            var sigLen = 0
            val top = ProtobufLite.ArrayReader(raw, 0, rawLen)
            while (!top.isAtEnd()) {
                val tag = top.readVarint32()
                when (ProtobufLite.getTagFieldNumber(tag)) {
                    1 -> { habLen = top.readLength(); habStart = top.pos; top.pos += habLen }
                    2 -> { sigLen = top.readLength(); sigStart = top.pos; top.pos += sigLen }
                    else -> top.skipField(ProtobufLite.getTagWireType(tag))
                }
            }

            if (habStart >= 0 && sigStart >= 0) {
                // 1. Verify HMAC-SHA256 (constant time)
                verifyingMac.reset()
                verifyingMac.update(raw, habStart, habLen)
                verifyingMac.doFinal(macScratch, 0)
                if (sigLen != macScratch.size) {
                    throw GeneralSecurityException("HMAC-SHA256 signature verification failed on SecureMessage")
                }
                var diff = 0
                for (i in 0 until sigLen) diff = diff or (raw[sigStart + i].toInt() xor macScratch[i].toInt())
                if (diff != 0) {
                    throw GeneralSecurityException("HMAC-SHA256 signature verification failed on SecureMessage")
                }

                // 2. Locate IV and ciphertext without copying
                var ivStart = -1
                var msgSeq: Long? = null
                var ctStart = -1
                var ctLen = 0
                val hab = ProtobufLite.ArrayReader(raw, habStart, habStart + habLen)
                while (!hab.isAtEnd()) {
                    val tag = hab.readVarint32()
                    when (ProtobufLite.getTagFieldNumber(tag)) {
                        1 -> {
                            val hLen = hab.readLength()
                            val header = ProtobufLite.ArrayReader(raw, hab.pos, hab.pos + hLen)
                            while (!header.isAtEnd()) {
                                val hTag = header.readVarint32()
                                when (ProtobufLite.getTagFieldNumber(hTag)) {
                                    3 -> {
                                        val ivLen = header.readLength()
                                        if (ivLen != 16) throw GeneralSecurityException("Invalid IV length $ivLen")
                                        ivStart = header.pos
                                        header.pos += ivLen
                                    }
                                    4 -> msgSeq = header.readVarint64()
                                    else -> header.skipField(ProtobufLite.getTagWireType(hTag))
                                }
                            }
                            hab.pos += hLen
                        }
                        2 -> { ctLen = hab.readLength(); ctStart = hab.pos; hab.pos += ctLen }
                        else -> hab.skipField(ProtobufLite.getTagWireType(tag))
                    }
                }

                if (ivStart < 0) throw GeneralSecurityException("SecureMessage has no IV")
                if (ctStart < 0) throw GeneralSecurityException("SecureMessage has no body")
                verifySequence(msgSeq)
                val ivSpec = IvParameterSpec(raw, ivStart, 16)

                // 3. Decrypt AES-256-CBC directly into the caller's buffer
                decryptCipher.init(Cipher.DECRYPT_MODE, decKeySpec, ivSpec)
                return decryptCipher.doFinal(raw, ctStart, ctLen, out, 0)
            }
        }

        val plain = decrypt(raw.copyOf(rawLen))
        System.arraycopy(plain, 0, out, 0, plain.size)
        return plain.size
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
        var iv: ByteArray? = null
        var ciphertext = ByteArray(0)
        var msgSeq: Long? = null

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

        if (iv == null || iv.size != 16) throw GeneralSecurityException("SecureMessage has no valid IV")
        verifySequence(msgSeq)

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
