package com.ioristudios.filemanager.sharing.protocol

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * High-performance, lightweight pure-Kotlin Protocol Buffer encoder & decoder.
 * Provides wire-level binary serialization for Google Nearby Connections frames
 * without requiring external protobuf runtime jars or plugins.
 */
class ProtobufLite {

    companion object {
        const val WIRE_TYPE_VARINT = 0
        const val WIRE_TYPE_FIXED64 = 1
        const val WIRE_TYPE_LENGTH_DELIMITED = 2
        const val WIRE_TYPE_FIXED32 = 5

        fun makeTag(fieldNumber: Int, wireType: Int): Int {
            return (fieldNumber shl 3) or wireType
        }

        fun getTagFieldNumber(tag: Int): Int {
            return tag ushr 3
        }

        fun getTagWireType(tag: Int): Int {
            return tag and 0x07
        }
    }

    /**
     * Binary Protobuf Writer
     */
    class Writer(private val out: ByteArrayOutputStream = ByteArrayOutputStream()) {

        fun writeTag(fieldNumber: Int, wireType: Int): Writer {
            writeVarint32(makeTag(fieldNumber, wireType))
            return this
        }

        fun writeVarint32(value: Int): Writer {
            var v = value
            while ((v and 0x7F.inv()) != 0) {
                out.write((v and 0x7F) or 0x80)
                v = v ushr 7
            }
            out.write(v and 0x7F)
            return this
        }

        fun writeVarint64(value: Long): Writer {
            var v = value
            while ((v and 0x7FL.inv()) != 0L) {
                out.write(((v and 0x7F) or 0x80).toInt())
                v = v ushr 7
            }
            out.write((v and 0x7F).toInt())
            return this
        }

        fun writeInt32(fieldNumber: Int, value: Int): Writer {
            writeTag(fieldNumber, WIRE_TYPE_VARINT)
            writeVarint32(value)
            return this
        }

        fun writeInt64(fieldNumber: Int, value: Long): Writer {
            writeTag(fieldNumber, WIRE_TYPE_VARINT)
            writeVarint64(value)
            return this
        }

        fun writeBool(fieldNumber: Int, value: Boolean): Writer {
            writeTag(fieldNumber, WIRE_TYPE_VARINT)
            out.write(if (value) 1 else 0)
            return this
        }

        fun writeBytes(fieldNumber: Int, value: ByteArray): Writer {
            writeTag(fieldNumber, WIRE_TYPE_LENGTH_DELIMITED)
            writeVarint32(value.size)
            out.write(value)
            return this
        }

        fun writeString(fieldNumber: Int, value: String): Writer {
            writeBytes(fieldNumber, value.toByteArray(Charsets.UTF_8))
            return this
        }

        fun writeMessage(fieldNumber: Int, writer: Writer): Writer {
            val bytes = writer.toByteArray()
            writeBytes(fieldNumber, bytes)
            return this
        }

        fun toByteArray(): ByteArray = out.toByteArray()
    }

    /**
     * Binary Protobuf Reader
     */
    class Reader(private val input: InputStream) {

        constructor(bytes: ByteArray) : this(ByteArrayInputStream(bytes))

        fun isAtEnd(): Boolean = input.available() <= 0

        fun readTag(): Int? {
            if (input.available() <= 0) return null
            return readVarint32()
        }

        fun readVarint32(): Int {
            var result = 0
            var shift = 0
            while (shift < 32) {
                val b = input.read()
                if (b == -1) throw IllegalStateException("Unexpected EOF reading varint32")
                result = result or ((b and 0x7F) shl shift)
                if ((b and 0x80) == 0) return result
                shift += 7
            }
            throw IllegalStateException("Malformed varint32")
        }

        fun readVarint64(): Long {
            var result = 0L
            var shift = 0
            while (shift < 64) {
                val b = input.read()
                if (b == -1) throw IllegalStateException("Unexpected EOF reading varint64")
                result = result or ((b.toLong() and 0x7FL) shl shift)
                if ((b and 0x80) == 0) return result
                shift += 7
            }
            throw IllegalStateException("Malformed varint64")
        }

        fun readBytes(): ByteArray {
            val length = readVarint32()
            val bytes = ByteArray(length)
            var read = 0
            while (read < length) {
                val count = input.read(bytes, read, length - read)
                if (count == -1) throw IllegalStateException("Unexpected EOF reading byte array")
                read += count
            }
            return bytes
        }

        fun readString(): String {
            return String(readBytes(), Charsets.UTF_8)
        }

        fun skipField(wireType: Int) {
            when (wireType) {
                WIRE_TYPE_VARINT -> readVarint64()
                WIRE_TYPE_FIXED64 -> {
                    for (i in 0 until 8) input.read()
                }
                WIRE_TYPE_LENGTH_DELIMITED -> {
                    val len = readVarint32()
                    input.skip(len.toLong())
                }
                WIRE_TYPE_FIXED32 -> {
                    for (i in 0 until 4) input.read()
                }
                else -> throw IllegalStateException("Unknown wire type: $wireType")
            }
        }
    }
}

