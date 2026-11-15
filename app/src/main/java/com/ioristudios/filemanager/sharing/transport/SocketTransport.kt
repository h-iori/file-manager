package com.ioristudios.filemanager.sharing.transport

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * High-performance TCP transport layer for peer-to-peer file transfer.
 * Features zero-copy chunk framing, kernel TCP buffer auto-tuning, and dual-stack IPv4 priority.
 */
class SocketTransport(val socket: Socket) {

    // Small stream buffers: large chunk frames bypass them entirely (no extra memcpy),
    // while small control frames are still coalesced into a single write.
    private val rawOutput = socket.getOutputStream()
    private val inputStream = DataInputStream(BufferedInputStream(socket.getInputStream(), 64 * 1024))
    private val outputStream = DataOutputStream(BufferedOutputStream(rawOutput, 64 * 1024))

    /** Reusable receive buffer for [receiveFrameInto]; grows as needed. */
    class FrameBuffer(initialCapacity: Int = 64 * 1024) {
        var data = ByteArray(initialCapacity)
        var length = 0
    }

    /**
     * Held while encrypting AND sending a frame, so frames that share this socket's codec
     * hit the wire in sequence-number order (the receiver rejects any reordering).
     */
    val sendLock = Any()

    fun setReadTimeout(ms: Int) {
        try { socket.soTimeout = ms } catch (_: Exception) {}
    }

    val isConnected: Boolean
        get() = socket.isConnected && !socket.isClosed

    /**
     * Sends a 4-byte big-endian length-prefixed packet without coroutine dispatch overhead.
     */
    fun sendFrameDirect(bytes: ByteArray) {
        synchronized(outputStream) {
            outputStream.writeInt(bytes.size)
            outputStream.write(bytes)
            outputStream.flush()
        }
    }

    /**
     * Sends a frame whose 4-byte big-endian length prefix the caller has already written into
     * buf[off, off+4). The whole frame goes to the socket in a single write with no copying.
     */
    fun sendPrefixedFrame(buf: ByteArray, off: Int, totalLen: Int) {
        synchronized(outputStream) {
            outputStream.flush() // drain any buffered control frame first to keep ordering
            rawOutput.write(buf, off, totalLen)
            rawOutput.flush()
        }
    }

    /**
     * Reads a length-prefixed packet into [fb], reusing its buffer (no per-frame allocation).
     */
    fun receiveFrameInto(fb: FrameBuffer) {
        synchronized(inputStream) {
            val length = inputStream.readInt()
            if (length < 0 || length > 16 * 1024 * 1024) { // 16MB frame limit
                throw IllegalStateException("Invalid packet length: $length")
            }
            if (fb.data.size < length) fb.data = ByteArray(length)
            inputStream.readFully(fb.data, 0, length)
            fb.length = length
        }
    }

    /**
     * Sends a 4-byte big-endian length-prefixed packet into the output buffer without flushing.
     * Ideal for pipelined high-throughput chunk streaming. Call flushOutput() after streaming a batch.
     */
    fun sendFrameNoFlush(bytes: ByteArray) {
        synchronized(outputStream) {
            outputStream.writeInt(bytes.size)
            outputStream.write(bytes)
        }
    }

    /**
     * Explicitly flushes the underlying buffered output stream to the network socket.
     */
    fun flushOutput() {
        synchronized(outputStream) {
            outputStream.flush()
        }
    }

    /**
     * Reads a 4-byte big-endian length-prefixed packet without coroutine dispatch overhead.
     */
    fun receiveFrameDirect(): ByteArray {
        return synchronized(inputStream) {
            val length = inputStream.readInt()
            if (length < 0 || length > 16 * 1024 * 1024) { // 16MB frame limit
                throw IllegalStateException("Invalid packet length: $length")
            }
            val buffer = ByteArray(length)
            inputStream.readFully(buffer)
            buffer
        }
    }

    suspend fun sendFrame(bytes: ByteArray) = withContext(Dispatchers.IO) {
        sendFrameDirect(bytes)
    }

    suspend fun receiveFrame(): ByteArray = withContext(Dispatchers.IO) {
        receiveFrameDirect()
    }

    fun close() {
        try {
            socket.close()
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "SocketTransport"

        /**
         * Connects to a remote host and port with automatic retry, IPv4 priority,
         * and post-connection socket configuration.
         */
        suspend fun connect(
            host: String,
            port: Int,
            timeoutMs: Int = 10000,
            maxRetries: Int = 2
        ): SocketTransport = withContext(Dispatchers.IO) {
            var lastException: Exception? = null

            // Prioritize IPv4 addresses over IPv6 link-local to avoid scope ID mismatches
            val candidateAddresses: List<InetAddress> = try {
                val all = InetAddress.getAllByName(host).toList()
                all.sortedByDescending { it is Inet4Address }
            } catch (_: Exception) {
                try {
                    listOf(InetAddress.getByName(host))
                } catch (e: Exception) {
                    emptyList()
                }
            }

            if (candidateAddresses.isEmpty()) {
                throw java.net.UnknownHostException("Cannot resolve host address: $host")
            }

            for (attempt in 1..maxRetries) {
                for (targetAddress in candidateAddresses) {
                    var socket: Socket? = null
                    try {
                        Log.d(TAG, "Connecting to ${targetAddress.hostAddress}:$port (attempt $attempt/$maxRetries)...")
                        socket = Socket()

                        // Connect first to establish native descriptor
                        socket.connect(InetSocketAddress(targetAddress, port), timeoutMs)

                        // Configure socket parameters only after connection is active
                        configureSocket(socket)

                        Log.d(TAG, "Successfully connected to ${targetAddress.hostAddress}:$port")
                        return@withContext SocketTransport(socket)
                    } catch (e: Exception) {
                        lastException = e
                        Log.w(TAG, "Connection attempt to ${targetAddress.hostAddress}:$port failed: ${e.message}")
                        try {
                            socket?.close()
                        } catch (_: Exception) {}
                    }
                }

                if (attempt < maxRetries) {
                    delay(500) // Brief delay before retrying
                }
            }

            throw lastException ?: java.net.ConnectException("Failed to connect to $host:$port after $maxRetries attempts")
        }

        /**
         * Configures socket options: TCP_NODELAY (disable Nagle), keepAlive, and 45s read timeout.
         *
         * Send/receive buffer sizes are intentionally NOT set: an explicit SO_RCVBUF/SO_SNDBUF
         * disables Linux TCP auto-tuning, which on Android Wi-Fi grows windows up to 4-8 MB
         * (config_wifi_tcp_buffers) - larger than any fixed value we could safely pick.
         */
        fun configureSocket(socket: Socket) {
            try {
                socket.tcpNoDelay = true
                socket.keepAlive = true
                socket.soTimeout = 45000 // 45s read timeout
            } catch (e: Exception) {
                Log.w(TAG, "Warning configuring connected socket options: ${e.message}")
            }
        }

        /**
         * Opens a pool of parallel TCP connections to the specified host and port.
         */
        suspend fun connectPool(
            host: String,
            port: Int,
            count: Int,
            timeoutMs: Int = 10000
        ): List<SocketTransport> = withContext(Dispatchers.IO) {
            val list = mutableListOf<SocketTransport>()
            for (i in 0 until count) {
                try {
                    val transport = connect(host, port, timeoutMs)
                    list.add(transport)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to connect worker $i in pool: ${e.message}")
                    break
                }
            }
            list
        }

        suspend fun createServer(port: Int = 0): Pair<ServerSocket, Int> =
            withContext(Dispatchers.IO) {
                val serverSocket = try {
                    ServerSocket(port, 50, InetAddress.getByAddress(byteArrayOf(0, 0, 0, 0)))
                } catch (_: Exception) {
                    ServerSocket(port)
                }
                Pair(serverSocket, serverSocket.localPort)
            }
    }
}
