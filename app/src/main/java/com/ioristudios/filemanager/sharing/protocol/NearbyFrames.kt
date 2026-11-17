package com.ioristudios.filemanager.sharing.protocol

import java.io.ByteArrayOutputStream

/**
 * Data structures and codecs for Google Nearby Connections wire frames.
 */
object NearbyFrames {

    // Frame types
    const val FRAME_CONNECTION_REQUEST = 1
    const val FRAME_CONNECTION_RESPONSE = 2
    const val FRAME_PAIRED_KEY_ENCRYPTION = 3
    const val FRAME_INTRODUCTION = 4
    const val FRAME_TRANSFER_ACCEPT = 5
    const val FRAME_TRANSFER_REJECT = 6
    const val FRAME_PAYLOAD_TRANSFER = 7
    const val FRAME_PAYLOAD_ACK = 8
    const val FRAME_BANDWIDTH_UPGRADE = 9
    const val FRAME_TRANSFER_CONTROL = 10
    const val FRAME_CHANNEL_INIT = 11

    data class ChannelInit(
        val channelId: Int,
        val sessionToken: String
    )

    data class FilePayloadMeta(
        val payloadId: Long,
        val fileName: String,
        val fileSize: Long,
        val mimeType: String
    )

    data class ConnectionRequest(
        val endpointId: String,
        val endpointName: String
    )

    data class ConnectionResponse(
        val accepted: Boolean
    )

    data class Introduction(
        val files: List<FilePayloadMeta>
    )

    data class PayloadTransfer(
        val payloadId: Long,
        val offset: Long,
        val chunkData: ByteArray,
        val isLastChunk: Boolean
    )

    data class PayloadAck(
        val payloadId: Long,
        val success: Boolean,
        val nextOffset: Long = 0L
    )

    data class BandwidthUpgrade(
        val medium: String, // "WIFI_DIRECT", "WIFI_HOTSPOT", "LAN"
        val ssid: String,
        val password: String,
        val ipAddress: String,
        val port: Int
    )

    data class TransferControl(
        val payloadId: Long,
        val action: String // "PAUSE", "RESUME", "CANCEL", "RETRY"
    )

    // --- Encoders ---

    fun encodeConnectionRequest(req: ConnectionRequest): ByteArray {
        val inner = ProtobufLite.Writer()
            .writeString(1, req.endpointId)
            .writeString(2, req.endpointName)
        return ProtobufLite.Writer()
            .writeInt32(1, FRAME_CONNECTION_REQUEST)
            .writeMessage(2, inner)
            .toByteArray()
    }

    fun encodeConnectionResponse(resp: ConnectionResponse): ByteArray {
        val inner = ProtobufLite.Writer()
            .writeBool(1, resp.accepted)
        return ProtobufLite.Writer()
            .writeInt32(1, FRAME_CONNECTION_RESPONSE)
            .writeMessage(2, inner)
            .toByteArray()
    }

    fun encodeIntroduction(intro: Introduction): ByteArray {
        val inner = ProtobufLite.Writer()
        for (file in intro.files) {
            val fileWriter = ProtobufLite.Writer()
                .writeInt64(1, file.payloadId)
                .writeString(2, file.fileName)
                .writeInt64(3, file.fileSize)
                .writeString(4, file.mimeType)
            inner.writeMessage(1, fileWriter)
        }
        return ProtobufLite.Writer()
            .writeInt32(1, FRAME_INTRODUCTION)
            .writeMessage(2, inner)
            .toByteArray()
    }

    fun encodeTransferAccept(): ByteArray {
        return ProtobufLite.Writer()
            .writeInt32(1, FRAME_TRANSFER_ACCEPT)
            .toByteArray()
    }

    fun encodeTransferReject(): ByteArray {
        return ProtobufLite.Writer()
            .writeInt32(1, FRAME_TRANSFER_REJECT)
            .toByteArray()
    }

    fun encodePayloadTransfer(transfer: PayloadTransfer): ByteArray {
        val inner = ProtobufLite.Writer(transfer.chunkData.size + 64)
            .writeInt64(1, transfer.payloadId)
            .writeInt64(2, transfer.offset)
            .writeBytes(3, transfer.chunkData)
            .writeBool(4, transfer.isLastChunk)
        return ProtobufLite.Writer(transfer.chunkData.size + 96)
            .writeInt32(1, FRAME_PAYLOAD_TRANSFER)
            .writeMessage(2, inner)
            .toByteArray()
    }

    /**
     * Writes the bytes that precede the chunk data in an encoded PayloadTransfer frame
     * (byte-identical to [encodePayloadTransfer]) into [dst]; returns the prefix length.
     * The full plaintext is: prefix + chunk data + [payloadTransferSuffix].
     * [dst] must hold at least 48 bytes.
     */
    fun writePayloadTransferPrefix(dst: ByteArray, payloadId: Long, offset: Long, dataLen: Int): Int {
        val innerLen = 1 + ProtobufLite.varintSize64(payloadId) +
            1 + ProtobufLite.varintSize64(offset) +
            1 + ProtobufLite.varintSize32(dataLen) + dataLen +
            2 // isLastChunk bool: tag + value
        var p = 0
        dst[p++] = 0x08 // field 1 (frame type), varint
        p = ProtobufLite.putVarint32(dst, p, FRAME_PAYLOAD_TRANSFER)
        dst[p++] = 0x12 // field 2 (payload message), length-delimited
        p = ProtobufLite.putVarint32(dst, p, innerLen)
        dst[p++] = 0x08 // inner field 1: payloadId
        p = ProtobufLite.putVarint64(dst, p, payloadId)
        dst[p++] = 0x10 // inner field 2: offset
        p = ProtobufLite.putVarint64(dst, p, offset)
        dst[p++] = 0x1A // inner field 3: chunk bytes
        p = ProtobufLite.putVarint32(dst, p, dataLen)
        return p
    }

    /** Trailing isLastChunk field of an encoded PayloadTransfer frame. */
    fun payloadTransferSuffix(isLast: Boolean): ByteArray =
        if (isLast) SUFFIX_LAST else SUFFIX_NOT_LAST

    private val SUFFIX_LAST = byteArrayOf(0x20, 1)
    private val SUFFIX_NOT_LAST = byteArrayOf(0x20, 0)

    /** Mutable, reusable view of a PayloadTransfer frame parsed in place (no copies). */
    class PayloadView {
        var payloadId = 0L
        var offset = 0L
        var dataOffset = 0
        var dataLength = 0
        var isLastChunk = false
    }

    /**
     * Parses [buf] (length [len]) in place. Returns true and fills [view] if it is a
     * PayloadTransfer frame; the chunk data stays inside [buf] at view.dataOffset.
     * Returns false for any other frame type (use [decodeFrame] for those).
     */
    fun parsePayloadInPlace(buf: ByteArray, len: Int, view: PayloadView): Boolean {
        val outer = ProtobufLite.ArrayReader(buf, 0, len)
        var frameType = 0
        var innerStart = -1
        var innerLen = 0
        while (!outer.isAtEnd()) {
            val tag = outer.readVarint32()
            when (ProtobufLite.getTagFieldNumber(tag)) {
                1 -> frameType = outer.readVarint32()
                2 -> {
                    innerLen = outer.readLength()
                    innerStart = outer.pos
                    outer.pos += innerLen
                }
                else -> outer.skipField(ProtobufLite.getTagWireType(tag))
            }
        }
        if (frameType != FRAME_PAYLOAD_TRANSFER || innerStart < 0) return false

        view.payloadId = 0L
        view.offset = 0L
        view.dataOffset = innerStart
        view.dataLength = 0
        view.isLastChunk = false
        val r = ProtobufLite.ArrayReader(buf, innerStart, innerStart + innerLen)
        while (!r.isAtEnd()) {
            val tag = r.readVarint32()
            when (ProtobufLite.getTagFieldNumber(tag)) {
                1 -> view.payloadId = r.readVarint64()
                2 -> view.offset = r.readVarint64()
                3 -> {
                    view.dataLength = r.readLength()
                    view.dataOffset = r.pos
                    r.pos += view.dataLength
                }
                4 -> view.isLastChunk = r.readVarint64() == 1L
                else -> r.skipField(ProtobufLite.getTagWireType(tag))
            }
        }
        return true
    }

    fun encodePayloadAck(ack: PayloadAck): ByteArray {
        val inner = ProtobufLite.Writer()
            .writeInt64(1, ack.payloadId)
            .writeBool(2, ack.success)
            .writeInt64(3, ack.nextOffset)
        return ProtobufLite.Writer()
            .writeInt32(1, FRAME_PAYLOAD_ACK)
            .writeMessage(2, inner)
            .toByteArray()
    }

    fun encodeBandwidthUpgrade(bu: BandwidthUpgrade): ByteArray {
        val inner = ProtobufLite.Writer()
            .writeString(1, bu.medium)
            .writeString(2, bu.ssid)
            .writeString(3, bu.password)
            .writeString(4, bu.ipAddress)
            .writeInt32(5, bu.port)
        return ProtobufLite.Writer()
            .writeInt32(1, FRAME_BANDWIDTH_UPGRADE)
            .writeMessage(2, inner)
            .toByteArray()
    }

    fun encodeTransferControl(ctrl: TransferControl): ByteArray {
        val inner = ProtobufLite.Writer()
            .writeInt64(1, ctrl.payloadId)
            .writeString(2, ctrl.action)
        return ProtobufLite.Writer()
            .writeInt32(1, FRAME_TRANSFER_CONTROL)
            .writeMessage(2, inner)
            .toByteArray()
    }

    fun encodeChannelInit(init: ChannelInit): ByteArray {
        val inner = ProtobufLite.Writer()
            .writeInt32(1, init.channelId)
            .writeString(2, init.sessionToken)
        return ProtobufLite.Writer()
            .writeInt32(1, FRAME_CHANNEL_INIT)
            .writeMessage(2, inner)
            .toByteArray()
    }

    // --- Decoders ---

    sealed class ParsedFrame {
        data class ConnRequest(val request: ConnectionRequest) : ParsedFrame()
        data class ConnResponse(val response: ConnectionResponse) : ParsedFrame()
        data class Intro(val introduction: Introduction) : ParsedFrame()
        object TransferAccepted : ParsedFrame()
        object TransferRejected : ParsedFrame()
        data class Payload(val transfer: PayloadTransfer) : ParsedFrame()
        data class Ack(val ack: PayloadAck) : ParsedFrame()
        data class Upgrade(val upgrade: BandwidthUpgrade) : ParsedFrame()
        data class Control(val control: TransferControl) : ParsedFrame()
        data class ChannelInitFrame(val init: ChannelInit) : ParsedFrame()
        object Unknown : ParsedFrame()
    }

    fun decodeFrame(bytes: ByteArray): ParsedFrame {
        val reader = ProtobufLite.Reader(bytes)
        var frameType = 0
        var payloadData = ByteArray(0)

        while (!reader.isAtEnd()) {
            val tag = reader.readTag() ?: break
            when (ProtobufLite.getTagFieldNumber(tag)) {
                1 -> frameType = reader.readVarint32()
                2 -> payloadData = reader.readBytes()
                else -> reader.skipField(ProtobufLite.getTagWireType(tag))
            }
        }

        return when (frameType) {
            FRAME_CONNECTION_REQUEST -> {
                val r = ProtobufLite.Reader(payloadData)
                var epId = ""
                var epName = ""
                while (!r.isAtEnd()) {
                    val tag = r.readTag() ?: break
                    when (ProtobufLite.getTagFieldNumber(tag)) {
                        1 -> epId = r.readString()
                        2 -> epName = r.readString()
                        else -> r.skipField(ProtobufLite.getTagWireType(tag))
                    }
                }
                ParsedFrame.ConnRequest(ConnectionRequest(epId, epName))
            }
            FRAME_CONNECTION_RESPONSE -> {
                val r = ProtobufLite.Reader(payloadData)
                var accepted = false
                while (!r.isAtEnd()) {
                    val tag = r.readTag() ?: break
                    when (ProtobufLite.getTagFieldNumber(tag)) {
                        1 -> accepted = r.readVarint32() == 1
                        else -> r.skipField(ProtobufLite.getTagWireType(tag))
                    }
                }
                ParsedFrame.ConnResponse(ConnectionResponse(accepted))
            }
            FRAME_INTRODUCTION -> {
                val r = ProtobufLite.Reader(payloadData)
                val files = mutableListOf<FilePayloadMeta>()
                while (!r.isAtEnd()) {
                    val tag = r.readTag() ?: break
                    when (ProtobufLite.getTagFieldNumber(tag)) {
                        1 -> {
                            val fBytes = r.readBytes()
                            val fr = ProtobufLite.Reader(fBytes)
                            var pId = 0L
                            var fName = ""
                            var fSize = 0L
                            var mMime = "*/*"
                            while (!fr.isAtEnd()) {
                                val fTag = fr.readTag() ?: break
                                when (ProtobufLite.getTagFieldNumber(fTag)) {
                                    1 -> pId = fr.readVarint64()
                                    2 -> fName = fr.readString()
                                    3 -> fSize = fr.readVarint64()
                                    4 -> mMime = fr.readString()
                                    else -> fr.skipField(ProtobufLite.getTagWireType(fTag))
                                }
                            }
                            files.add(FilePayloadMeta(pId, fName, fSize, mMime))
                        }
                        else -> r.skipField(ProtobufLite.getTagWireType(tag))
                    }
                }
                ParsedFrame.Intro(Introduction(files))
            }
            FRAME_TRANSFER_ACCEPT -> ParsedFrame.TransferAccepted
            FRAME_TRANSFER_REJECT -> ParsedFrame.TransferRejected
            FRAME_PAYLOAD_TRANSFER -> {
                val r = ProtobufLite.Reader(payloadData)
                var pId = 0L
                var offset = 0L
                var chunk = ByteArray(0)
                var last = false
                while (!r.isAtEnd()) {
                    val tag = r.readTag() ?: break
                    when (ProtobufLite.getTagFieldNumber(tag)) {
                        1 -> pId = r.readVarint64()
                        2 -> offset = r.readVarint64()
                        3 -> chunk = r.readBytes()
                        4 -> last = r.readVarint32() == 1
                        else -> r.skipField(ProtobufLite.getTagWireType(tag))
                    }
                }
                ParsedFrame.Payload(PayloadTransfer(pId, offset, chunk, last))
            }
            FRAME_PAYLOAD_ACK -> {
                val r = ProtobufLite.Reader(payloadData)
                var pId = 0L
                var success = false
                var nextOffset = 0L
                while (!r.isAtEnd()) {
                    val tag = r.readTag() ?: break
                    when (ProtobufLite.getTagFieldNumber(tag)) {
                        1 -> pId = r.readVarint64()
                        2 -> success = r.readVarint32() == 1
                        3 -> nextOffset = r.readVarint64()
                        else -> r.skipField(ProtobufLite.getTagWireType(tag))
                    }
                }
                ParsedFrame.Ack(PayloadAck(pId, success, nextOffset))
            }
            FRAME_BANDWIDTH_UPGRADE -> {
                val r = ProtobufLite.Reader(payloadData)
                var medium = ""
                var ssid = ""
                var pass = ""
                var ip = ""
                var port = 0
                while (!r.isAtEnd()) {
                    val tag = r.readTag() ?: break
                    when (ProtobufLite.getTagFieldNumber(tag)) {
                        1 -> medium = r.readString()
                        2 -> ssid = r.readString()
                        3 -> pass = r.readString()
                        4 -> ip = r.readString()
                        5 -> port = r.readVarint32()
                        else -> r.skipField(ProtobufLite.getTagWireType(tag))
                    }
                }
                ParsedFrame.Upgrade(BandwidthUpgrade(medium, ssid, pass, ip, port))
            }
            FRAME_TRANSFER_CONTROL -> {
                val r = ProtobufLite.Reader(payloadData)
                var pId = 0L
                var action = ""
                while (!r.isAtEnd()) {
                    val tag = r.readTag() ?: break
                    when (ProtobufLite.getTagFieldNumber(tag)) {
                        1 -> pId = r.readVarint64()
                        2 -> action = r.readString()
                        else -> r.skipField(ProtobufLite.getTagWireType(tag))
                    }
                }
                ParsedFrame.Control(TransferControl(pId, action))
            }
            FRAME_CHANNEL_INIT -> {
                val r = ProtobufLite.Reader(payloadData)
                var chId = 0
                var token = ""
                while (!r.isAtEnd()) {
                    val tag = r.readTag() ?: break
                    when (ProtobufLite.getTagFieldNumber(tag)) {
                        1 -> chId = r.readVarint32()
                        2 -> token = r.readString()
                        else -> r.skipField(ProtobufLite.getTagWireType(tag))
                    }
                }
                ParsedFrame.ChannelInitFrame(ChannelInit(chId, token))
            }
            else -> ParsedFrame.Unknown
        }
    }
}

