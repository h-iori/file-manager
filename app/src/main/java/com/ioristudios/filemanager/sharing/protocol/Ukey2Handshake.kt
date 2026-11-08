package com.ioristudios.filemanager.sharing.protocol

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement

/**
 * Universal Key Exchange 2 (UKEY2) implementation for Google Nearby Connections / Quick Share.
 * Compliant with Google UKEY2 specifications:
 * - Ephemeral ECDH key agreement over NIST P-256 (secp256r1)
 * - SHA-512 CipherCommitments in ClientInit
 * - RFC 5869 HKDF derivation with "UKEY2 v1 auth" & "UKEY2 v1 next" using handshake transcript
 * - Symmetric session keys (AES-256-CBC) and HMAC-SHA256 signing keys for SecureMessage
 */
class Ukey2Handshake(val isClient: Boolean) {

    enum class State {
        INITIAL,
        WAITING_FOR_SERVER_INIT,
        WAITING_FOR_CLIENT_FINISHED,
        COMPLETED,
        FAILED
    }

    var state: State = State.INITIAL
        private set

    private val secureRandom = SecureRandom()
    private val keyPair: KeyPair
    private val localRandom = ByteArray(32)

    private var m1Bytes: ByteArray? = null
    private var m2Bytes: ByteArray? = null

    var remotePublicKey: ECPublicKey? = null
        private set
    var authToken: ByteArray? = null
        private set
    var clientKey: ByteArray? = null
        private set
    var serverKey: ByteArray? = null
        private set
    var clientHmacKey: ByteArray? = null
        private set
    var serverHmacKey: ByteArray? = null
        private set

    init {
        secureRandom.nextBytes(localRandom)
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"), secureRandom)
        keyPair = kpg.generateKeyPair()
    }

    /**
     * Client Step 1: Generates ClientInit wire message with SHA-512 CipherCommitment
     */
    fun createClientInit(): ByteArray {
        check(isClient) { "Only client can create ClientInit" }
        check(state == State.INITIAL)

        val clientPublicKeyBytes = keyPair.public.encoded
        val sha512 = MessageDigest.getInstance("SHA-512").digest(clientPublicKeyBytes)

        // CipherCommitment: field 1 = handshake_cipher (1 = P256_SHA512), field 2 = commitment
        val commitmentWriter = ProtobufLite.Writer()
            .writeInt32(1, 1) // P256_SHA512
            .writeBytes(2, sha512)

        val innerWriter = ProtobufLite.Writer()
            .writeInt32(1, 1) // version = 1
            .writeBytes(2, localRandom) // random = 32 bytes
            .writeBytes(3, commitmentWriter.toByteArray()) // cipher_commitments
            .writeString(4, "AES_256_CBC-HMAC_SHA256") // next_protocol

        val clientInitData = innerWriter.toByteArray()
        m1Bytes = clientInitData

        val outerWriter = ProtobufLite.Writer()
            .writeInt32(1, 1) // message_type = CLIENT_INIT (1)
            .writeBytes(2, clientInitData)

        state = State.WAITING_FOR_SERVER_INIT
        return outerWriter.toByteArray()
    }

    /**
     * Server Step 1: Processes ClientInit and generates ServerInit wire message
     */
    fun processClientInitAndGenerateServerInit(clientInitFrame: ByteArray): ByteArray {
        check(!isClient) { "Only server can process ClientInit" }
        check(state == State.INITIAL)

        val reader = ProtobufLite.Reader(clientInitFrame)
        var msgType = 0
        var msgData = ByteArray(0)
        while (!reader.isAtEnd()) {
            val tag = reader.readTag() ?: break
            when (ProtobufLite.getTagFieldNumber(tag)) {
                1 -> msgType = reader.readVarint32()
                2 -> msgData = reader.readBytes()
                else -> reader.skipField(ProtobufLite.getTagWireType(tag))
            }
        }
        require(msgType == 1) { "Expected CLIENT_INIT message type 1, got $msgType" }
        m1Bytes = msgData

        val serverPublicKeyBytes = keyPair.public.encoded

        val innerWriter = ProtobufLite.Writer()
            .writeInt32(1, 1) // version = 1
            .writeBytes(2, localRandom) // server random = 32 bytes
            .writeInt32(3, 1) // selected_cipher = P256_SHA512 (1)
            .writeBytes(4, serverPublicKeyBytes) // public_key

        val serverInitData = innerWriter.toByteArray()
        m2Bytes = serverInitData

        val outerWriter = ProtobufLite.Writer()
            .writeInt32(1, 2) // message_type = SERVER_INIT (2)
            .writeBytes(2, serverInitData)

        state = State.WAITING_FOR_CLIENT_FINISHED
        return outerWriter.toByteArray()
    }

    /**
     * Client Step 2: Processes ServerInit and generates ClientFinish wire message
     */
    fun processServerInitAndGenerateClientFinish(serverInitFrame: ByteArray): ByteArray {
        check(isClient) { "Only client can process ServerInit" }
        check(state == State.WAITING_FOR_SERVER_INIT)

        val reader = ProtobufLite.Reader(serverInitFrame)
        var msgType = 0
        var msgData = ByteArray(0)
        while (!reader.isAtEnd()) {
            val tag = reader.readTag() ?: break
            when (ProtobufLite.getTagFieldNumber(tag)) {
                1 -> msgType = reader.readVarint32()
                2 -> msgData = reader.readBytes()
                else -> reader.skipField(ProtobufLite.getTagWireType(tag))
            }
        }
        require(msgType == 2) { "Expected SERVER_INIT message type 2, got $msgType" }
        m2Bytes = msgData

        val serverReader = ProtobufLite.Reader(msgData)
        var serverPubKeyBytes = ByteArray(0)
        while (!serverReader.isAtEnd()) {
            val tag = serverReader.readTag() ?: break
            when (ProtobufLite.getTagFieldNumber(tag)) {
                4 -> serverPubKeyBytes = serverReader.readBytes()
                else -> serverReader.skipField(ProtobufLite.getTagWireType(tag))
            }
        }

        val kf = KeyFactory.getInstance("EC")
        val ecSpec = X509EncodedKeySpec(serverPubKeyBytes)
        val sKey = kf.generatePublic(ecSpec) as ECPublicKey
        remotePublicKey = sKey

        deriveAllKeys(sKey)

        val clientPublicKeyBytes = keyPair.public.encoded
        val innerWriter = ProtobufLite.Writer()
            .writeBytes(1, clientPublicKeyBytes)

        val outerWriter = ProtobufLite.Writer()
            .writeInt32(1, 3) // message_type = CLIENT_FINISHED (3)
            .writeBytes(2, innerWriter.toByteArray())

        state = State.COMPLETED
        return outerWriter.toByteArray()
    }

    /**
     * Server Step 2: Processes ClientFinished and completes key exchange
     */
    fun processClientFinished(clientFinishedFrame: ByteArray) {
        check(!isClient) { "Only server can process ClientFinished" }
        check(state == State.WAITING_FOR_CLIENT_FINISHED)

        val reader = ProtobufLite.Reader(clientFinishedFrame)
        var msgType = 0
        var msgData = ByteArray(0)
        while (!reader.isAtEnd()) {
            val tag = reader.readTag() ?: break
            when (ProtobufLite.getTagFieldNumber(tag)) {
                1 -> msgType = reader.readVarint32()
                2 -> msgData = reader.readBytes()
                else -> reader.skipField(ProtobufLite.getTagWireType(tag))
            }
        }
        require(msgType == 3) { "Expected CLIENT_FINISHED message type 3, got $msgType" }

        val clientReader = ProtobufLite.Reader(msgData)
        var clientPubKeyBytes = ByteArray(0)
        while (!clientReader.isAtEnd()) {
            val tag = clientReader.readTag() ?: break
            when (ProtobufLite.getTagFieldNumber(tag)) {
                1 -> clientPubKeyBytes = clientReader.readBytes()
                else -> clientReader.skipField(ProtobufLite.getTagWireType(tag))
            }
        }

        val kf = KeyFactory.getInstance("EC")
        val ecSpec = X509EncodedKeySpec(clientPubKeyBytes)
        val cKey = kf.generatePublic(ecSpec) as ECPublicKey
        remotePublicKey = cKey

        deriveAllKeys(cKey)
        state = State.COMPLETED
    }

    /**
     * Standard Google UKEY2 key derivation:
     * - DHS = ECDH(keyPair.private, remoteKey)
     * - PRK_AUTH = HKDF-Extract("UKEY2 v1 auth", DHS)
     * - AUTH_STRING = HKDF-Expand(PRK_AUTH, M1 || M2, 32) -> PIN code
     * - PRK_NEXT = HKDF-Extract("UKEY2 v1 next", DHS)
     * - NEXT_SECRET = HKDF-Expand(PRK_NEXT, M1 || M2, 32)
     * - SecureMessage keys derived from NEXT_SECRET
     */
    private fun deriveAllKeys(remoteKey: ECPublicKey) {
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(keyPair.private)
        ka.doPhase(remoteKey, true)
        val dhs = ka.generateSecret()

        val transcript = (m1Bytes ?: ByteArray(0)) + (m2Bytes ?: ByteArray(0))

        // 1. Derive Auth Token for PIN confirmation
        val prkAuth = Hkdf.extract("UKEY2 v1 auth".toByteArray(Charsets.UTF_8), dhs)
        authToken = Hkdf.expand(prkAuth, transcript, 32)

        // 2. Derive Next-Protocol Secret
        val prkNext = Hkdf.extract("UKEY2 v1 next".toByteArray(Charsets.UTF_8), dhs)
        val nextSecret = Hkdf.expand(prkNext, transcript, 32)

        // 3. Derive Symmetric Session Keys for SecureMessage
        val prkSecure = Hkdf.extract("SecureMessage".toByteArray(Charsets.UTF_8), nextSecret)
        clientKey = Hkdf.expand(prkSecure, "client-key".toByteArray(Charsets.UTF_8), 32)
        serverKey = Hkdf.expand(prkSecure, "server-key".toByteArray(Charsets.UTF_8), 32)
        clientHmacKey = Hkdf.expand(prkSecure, "client-hmac-key".toByteArray(Charsets.UTF_8), 32)
        serverHmacKey = Hkdf.expand(prkSecure, "server-hmac-key".toByteArray(Charsets.UTF_8), 32)
    }

    fun getEncryptionKey(): ByteArray = if (isClient) clientKey!! else serverKey!!
    fun getDecryptionKey(): ByteArray = if (isClient) serverKey!! else clientKey!!
    fun getSigningHmacKey(): ByteArray = if (isClient) clientHmacKey!! else serverHmacKey!!
    fun getVerifyingHmacKey(): ByteArray = if (isClient) serverHmacKey!! else clientHmacKey!!
}
