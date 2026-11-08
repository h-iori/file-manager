package com.ioristudios.filemanager.sharing

import com.ioristudios.filemanager.sharing.protocol.Hkdf
import com.ioristudios.filemanager.sharing.protocol.NearbyFrames
import com.ioristudios.filemanager.sharing.protocol.PinCalculator
import com.ioristudios.filemanager.sharing.protocol.ProtobufLite
import com.ioristudios.filemanager.sharing.protocol.SecureMessageCodec
import com.ioristudios.filemanager.sharing.protocol.Ukey2Handshake
import com.ioristudios.filemanager.sharing.resilience.TransferLifecycleStatus
import com.ioristudios.filemanager.sharing.resilience.TransferSessionManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.GeneralSecurityException

class ProtocolAndResilienceTest {

    @Test
    fun testUkey2HandshakeRoundtripAndPinMatch() {
        // Initialize client and server instances
        val client = Ukey2Handshake(isClient = true)
        val server = Ukey2Handshake(isClient = false)

        // 1. ClientInit -> Server
        val clientInitFrame = client.createClientInit()
        assertNotNull(clientInitFrame)
        assertTrue(clientInitFrame.isNotEmpty())

        // 2. Server processes ClientInit and generates ServerInit
        val serverInitFrame = server.processClientInitAndGenerateServerInit(clientInitFrame)
        assertNotNull(serverInitFrame)
        assertTrue(serverInitFrame.isNotEmpty())

        // 3. Client processes ServerInit and generates ClientFinished
        val clientFinishedFrame = client.processServerInitAndGenerateClientFinish(serverInitFrame)
        assertNotNull(clientFinishedFrame)
        assertTrue(clientFinishedFrame.isNotEmpty())

        // 4. Server processes ClientFinished
        server.processClientFinished(clientFinishedFrame)

        // Verify both completed
        assertEquals(Ukey2Handshake.State.COMPLETED, client.state)
        assertEquals(Ukey2Handshake.State.COMPLETED, server.state)

        // Verify identical Auth Tokens derived
        assertNotNull(client.authToken)
        assertNotNull(server.authToken)
        assertArrayEquals(client.authToken, server.authToken)

        // Verify matching 4-digit PIN generated on both sides
        val clientPin = PinCalculator.calculatePin(client.authToken!!)
        val serverPin = PinCalculator.calculatePin(server.authToken!!)
        assertEquals(clientPin, serverPin)
        assertEquals(4, clientPin.length)
        assertTrue(clientPin.toInt() in 1000..9999)

        // Verify symmetric session keys align across client and server
        assertArrayEquals(client.getEncryptionKey(), server.getDecryptionKey())
        assertArrayEquals(client.getDecryptionKey(), server.getEncryptionKey())
        assertArrayEquals(client.getSigningHmacKey(), server.getVerifyingHmacKey())
        assertArrayEquals(client.getVerifyingHmacKey(), server.getSigningHmacKey())
    }

    @Test
    fun testSecureMessageEncryptionAndHmacVerification() {
        val clientKey = ByteArray(32) { 1 }
        val serverKey = ByteArray(32) { 2 }
        val clientHmac = ByteArray(32) { 3 }
        val serverHmac = ByteArray(32) { 4 }

        val clientCodec = SecureMessageCodec(
            encryptionKey = clientKey,
            decryptionKey = serverKey,
            signingHmacKey = clientHmac,
            verifyingHmacKey = serverHmac
        )

        val serverCodec = SecureMessageCodec(
            encryptionKey = serverKey,
            decryptionKey = clientKey,
            signingHmacKey = serverHmac,
            verifyingHmacKey = clientHmac
        )

        val originalMessage = "Hello Google Files P2P Quick Share!".toByteArray(Charsets.UTF_8)

        // Encrypt with client
        val wirePacket = clientCodec.encrypt(originalMessage)
        assertTrue(wirePacket.isNotEmpty())

        // Decrypt directly with server using standard SecureMessage protobuf parser
        val decrypted = serverCodec.decrypt(wirePacket)
        assertArrayEquals(originalMessage, decrypted)
    }

    @Test(expected = GeneralSecurityException::class)
    fun testSecureMessageTamperRejection() {
        val clientKey = ByteArray(32) { 1 }
        val serverKey = ByteArray(32) { 2 }
        val clientHmac = ByteArray(32) { 3 }
        val serverHmac = ByteArray(32) { 4 }

        val clientCodec = SecureMessageCodec(clientKey, serverKey, clientHmac, serverHmac)
        val serverCodec = SecureMessageCodec(serverKey, clientKey, serverHmac, clientHmac)

        val wirePacket = clientCodec.encrypt("Secret data".toByteArray(Charsets.UTF_8))

        // Tamper with wire bytes
        val tampered = wirePacket.copyOf()
        tampered[tampered.size - 1] = (tampered[tampered.size - 1] + 1).toByte()

        serverCodec.decrypt(tampered)
    }

    @Test
    fun testNearbyFramesEncodeDecode() {
        // 1. ConnectionRequest
        val req = NearbyFrames.ConnectionRequest("EP01", "Pixel 8 Pro")
        val reqBytes = NearbyFrames.encodeConnectionRequest(req)
        val parsedReq = NearbyFrames.decodeFrame(reqBytes)
        assertTrue(parsedReq is NearbyFrames.ParsedFrame.ConnRequest)
        assertEquals("EP01", (parsedReq as NearbyFrames.ParsedFrame.ConnRequest).request.endpointId)
        assertEquals("Pixel 8 Pro", parsedReq.request.endpointName)

        // 2. Introduction
        val files = listOf(
            NearbyFrames.FilePayloadMeta(1001L, "photo.jpg", 1024L * 500, "image/jpeg"),
            NearbyFrames.FilePayloadMeta(1002L, "video.mp4", 1024L * 1024 * 50, "video/mp4")
        )
        val introBytes = NearbyFrames.encodeIntroduction(NearbyFrames.Introduction(files))
        val parsedIntro = NearbyFrames.decodeFrame(introBytes)
        assertTrue(parsedIntro is NearbyFrames.ParsedFrame.Intro)
        val decodedFiles = (parsedIntro as NearbyFrames.ParsedFrame.Intro).introduction.files
        assertEquals(2, decodedFiles.size)
        assertEquals("photo.jpg", decodedFiles[0].fileName)
        assertEquals(1024L * 1024 * 50, decodedFiles[1].fileSize)

        // 3. PayloadTransfer
        val chunk = byteArrayOf(1, 2, 3, 4, 5)
        val payload = NearbyFrames.PayloadTransfer(1001L, 0L, chunk, true)
        val payloadBytes = NearbyFrames.encodePayloadTransfer(payload)
        val parsedPayload = NearbyFrames.decodeFrame(payloadBytes)
        assertTrue(parsedPayload is NearbyFrames.ParsedFrame.Payload)
        val p = (parsedPayload as NearbyFrames.ParsedFrame.Payload).transfer
        assertEquals(1001L, p.payloadId)
        assertTrue(p.isLastChunk)
        assertArrayEquals(chunk, p.chunkData)

        // 4. TransferControl
        val ctrl = NearbyFrames.TransferControl(1001L, "PAUSE")
        val ctrlBytes = NearbyFrames.encodeTransferControl(ctrl)
        val parsedCtrl = NearbyFrames.decodeFrame(ctrlBytes)
        assertTrue(parsedCtrl is NearbyFrames.ParsedFrame.Control)
        assertEquals("PAUSE", (parsedCtrl as NearbyFrames.ParsedFrame.Control).control.action)

        // 5. ChannelInit for Multi-Connection Parallel Mode
        val chInit = NearbyFrames.ChannelInit(channelId = 3, sessionToken = "token_abc123")
        val chInitBytes = NearbyFrames.encodeChannelInit(chInit)
        val parsedChInit = NearbyFrames.decodeFrame(chInitBytes)
        assertTrue(parsedChInit is NearbyFrames.ParsedFrame.ChannelInitFrame)
        val decodedInit = (parsedChInit as NearbyFrames.ParsedFrame.ChannelInitFrame).init
        assertEquals(3, decodedInit.channelId)
        assertEquals("token_abc123", decodedInit.sessionToken)

        // 6. BandwidthUpgrade
        val upgrade = NearbyFrames.BandwidthUpgrade(
            medium = "PARALLEL_CHANNELS",
            ssid = "token123",
            password = "",
            ipAddress = "192.168.1.10",
            port = 4
        )
        val upgradeBytes = NearbyFrames.encodeBandwidthUpgrade(upgrade)
        val parsedUpgrade = NearbyFrames.decodeFrame(upgradeBytes)
        assertTrue(parsedUpgrade is NearbyFrames.ParsedFrame.Upgrade)
        assertEquals("PARALLEL_CHANNELS", (parsedUpgrade as NearbyFrames.ParsedFrame.Upgrade).upgrade.medium)
        assertEquals(4, parsedUpgrade.upgrade.port)
    }

    @Test
    fun testSecureMessageCodecDuplicateAndMultipleRounds() {
        val clientKey = ByteArray(32) { 1 }
        val serverKey = ByteArray(32) { 2 }
        val clientHmac = ByteArray(32) { 3 }
        val serverHmac = ByteArray(32) { 4 }

        val clientCodec = SecureMessageCodec(clientKey, serverKey, clientHmac, serverHmac)
        val serverCodec = SecureMessageCodec(serverKey, clientKey, serverHmac, clientHmac)

        // Verify duplicate works independently
        val dupClient = clientCodec.duplicate()
        val dupServer = serverCodec.duplicate()

        for (i in 1..20) {
            val message = "Message round $i - high throughput payload test".toByteArray(Charsets.UTF_8)
            val encrypted = dupClient.encrypt(message)
            val decrypted = dupServer.decrypt(encrypted)
            assertArrayEquals(message, decrypted)
        }
    }

    @Test
    fun testTransferSessionManagerResilience() {
        val manager = TransferSessionManager()
        assertEquals(TransferLifecycleStatus.IDLE, manager.progress.value.status)

        manager.updateStatus(TransferLifecycleStatus.TRANSFERRING)
        assertEquals(TransferLifecycleStatus.TRANSFERRING, manager.progress.value.status)

        // Pause
        manager.pauseTransfer()
        assertTrue(manager.isPaused)
        assertEquals(TransferLifecycleStatus.PAUSED, manager.progress.value.status)

        // Resume
        manager.resumeTransfer()
        assertFalse(manager.isPaused)
        assertEquals(TransferLifecycleStatus.TRANSFERRING, manager.progress.value.status)

        // Cancel
        manager.cancelTransfer()
        assertTrue(manager.isCancelled)
        assertEquals(TransferLifecycleStatus.CANCELLED, manager.progress.value.status)
    }
}

