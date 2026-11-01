package com.ioristudios.filemanager.sharing.protocol

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement

/**
 * Universal Key Exchange 2 (UKEY2) implementation for Google Nearby Connections.
 * Handles ephemeral ECDH key agreement over NIST P-256 (secp256r1) and derives
 * symmetric session keys (AES-256-CBC) and authentication tags (HMAC-SHA256).
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
     * Client Step 1: Generates ClientInit wire message
     */
    fun createClientInit(): ByteArray {
        check(isClient) { "Only client can create ClientInit" }
        check(state == State.INITIAL)

        val innerWriter = ProtobufLite.Writer()
            .writeInt32(1, 1) // version = 1
            .writeBytes(2, localRandom) // random = 32 bytes
            .writeString(4, "AES_256_CBC-HMAC_SHA256") // next_protocol

        val clientInitData = innerWriter.toByteArray()

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

        val serverPublicKeyBytes = keyPair.public.encoded

        val innerWriter = ProtobufLite.Writer()
            .writeInt32(1, 1) // version = 1
            .writeBytes(2, localRandom) // server random = 32 bytes
            .writeInt32(3, 1) // selected_cipher = P256_SHA512 (1)
            .writeBytes(4, serverPublicKeyBytes) // public_key

        val outerWriter = ProtobufLite.Writer()
            .writeInt32(1, 2) // message_type = SERVER_INIT (2)
            .writeBytes(2, innerWriter.toByteArray())

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

    private fun deriveAllKeys(remoteKey: ECPublicKey) {
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(keyPair.private)
        ka.doPhase(remoteKey, true)
        val sharedSecret = ka.generateSecret()

        // HKDF-Extract using "UKEY2" info context
        val salt = "UKEY2".toByteArray(Charsets.UTF_8)
        val prk = Hkdf.extract(salt, sharedSecret)

        // Derive 32-byte Auth Token for PIN confirmation
        authToken = Hkdf.expand(prk, "auth-token".toByteArray(Charsets.UTF_8), 32)

        // Derive Symmetric AES-256 session keys & HMAC signing keys
        clientKey = Hkdf.expand(prk, "client-key".toByteArray(Charsets.UTF_8), 32)
        serverKey = Hkdf.expand(prk, "server-key".toByteArray(Charsets.UTF_8), 32)
        clientHmacKey = Hkdf.expand(prk, "client-hmac-key".toByteArray(Charsets.UTF_8), 32)
        serverHmacKey = Hkdf.expand(prk, "server-hmac-key".toByteArray(Charsets.UTF_8), 32)
    }

    fun getEncryptionKey(): ByteArray = if (isClient) clientKey!! else serverKey!!
    fun getDecryptionKey(): ByteArray = if (isClient) serverKey!! else clientKey!!
    fun getSigningHmacKey(): ByteArray = if (isClient) clientHmacKey!! else serverHmacKey!!
    fun getVerifyingHmacKey(): ByteArray = if (isClient) serverHmacKey!! else clientHmacKey!!
}

